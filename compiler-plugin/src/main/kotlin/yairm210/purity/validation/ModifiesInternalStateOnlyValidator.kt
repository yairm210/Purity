@file:OptIn(UnsafeDuringIrConstructionAPI::class)
package yairm210.purity.validation

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetField
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrSetField
import org.jetbrains.kotlin.ir.expressions.IrSetValue
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrVisitor
import yairm210.purity.PurityConfig

/**
 * Validates the contract of `@ModifiesInternalStateOnly`: "can mutate *only* state that it owns".
 *
 * For a function marked/expected to follow this (directly, or because its class is) that is not itself
 * `@Pure`/`@Readonly` (those are checked separately, by the normal purity checks), this ensures:
 *  A. every called function is either Pure, Readonly, or a method called on *this instance* (own
 *     methods, whatever their purity, are trusted to maintain the invariant themselves) - calling a
 *     method on a DIFFERENT instance (even of the same class) requires that call itself to be Pure/Readonly
 *  B. every var/field set is either local to this function, or a field of *this instance* - never a
 *     captured outer var, or a field on some other object (even another instance of this class)
 */
internal fun validateModifiesInternalStateOnly(
    function: IrFunction,
    purityConfig: PurityConfig,
    messageCollector: MessageCollector,
): List<String> {
    if (!FunctionAnnotations.ModifiesInternalStateOnly.isExplicitlyMarked(function, purityConfig)) return emptyList()
    if (function !is IrSimpleFunction) return emptyList()

    // Local functions (declared inside another function, e.g. a closure like `fun addWaypoint(...)`
    // inside a member function) are not themselves top-level members of the class - they're an
    // implementation detail of whichever member function declares them, and are already covered by
    // that member's own top-to-bottom body traversal (which visits into nested function bodies too).
    // Checking them again in isolation loses access to the enclosing scope's local vals (e.g. a
    // `mutableListOf()` captured from the outer function), causing false positives.
    if (function.parent !is IrClass) return emptyList()

    // Checked separately by the normal Pure/Readonly purity checks
    if (ExpectedFunctionPurityChecker.isMarkedAsPure(function, purityConfig)) return emptyList()
    if (ExpectedFunctionPurityChecker.isReadonly(function, purityConfig)) return emptyList()

    if (function.body == null) return emptyList()
    if (function.suppressesPurity()) return emptyList()

    val thisReceiver = function.dispatchReceiverParameter
    fun isThis(expression: IrExpression?): Boolean =
        expression is IrGetValue && expression.symbol.owner == thisReceiver

    // A newly constructed (or @ReturnsNewInstance-returned) local val is guaranteed unaliased, so
    // mutating it is just as safe as mutating `this` - e.g. `val new = Foo(); new.x = 1` in a clone() method
    val ownedInstances = OwnedInstanceVariableTracker(purityConfig)

    // A field of `this` instance is itself state this instance owns - but only if A. that field's own
    // class also guarantees it only mutates its own internal state, and B. the field is a val whose
    // declared initializer is itself a new (constructor/@ReturnsNewInstance) instance - otherwise the
    // check could be "whitewashed": declare an internal-state-class val field, but actually assign it
    // (e.g. via the constructor) a reference some external caller still holds, then mutate it through
    // a call on the field - defeating the "may only mutate state that it owns" guarantee entirely.
    fun receiverOf(call: IrCall): IrExpression? {
        val index = call.symbol.owner.parameters
            .indexOfFirst { it.kind == IrParameterKind.DispatchReceiver || it.kind == IrParameterKind.ExtensionReceiver }
        return if (index != -1) call.arguments[index] else null
    }
    fun backingFieldOf(expression: IrExpression?): IrField? = when (expression) {
        is IrGetField -> if (isThis(expression.receiver)) expression.symbol.owner else null
        is IrCall -> if (expression.symbol.owner.isGetter && isThis(receiverOf(expression)))
            expression.symbol.owner.correspondingPropertySymbol?.owner?.backingField else null
        else -> null
    }
    fun isOwnField(expression: IrExpression?): Boolean {
        val field = backingFieldOf(expression) ?: return false
        return isInternalStateClass(field.type.getClass(), purityConfig) && isNewInstanceField(field, purityConfig)
    }

    // A trusted default property setter (see isTrustedDefaultSetter) is exactly equivalent to a raw
    // field set, regardless of what class it belongs to - trusted under the same conditions as a
    // direct field set (this/owned-local receiver), without needing the receiver's class to itself
    // be @ModifiesInternalStateOnly.
    fun isOwnedReceiverForDefaultSetter(calledFunction: IrSimpleFunction, receiver: IrExpression?): Boolean =
        isTrustedDefaultSetter(calledFunction) && (isThis(receiver) || ownedInstances.isOwned(receiver))

    // Interface delegation (`class C : Map<K,V> by map`) generates synthetic member functions that just
    // forward to the delegate field - trusted (without the freshness check above, since a delegate field
    // is a fixed, single-assignment implementation slot, not an arbitrary owned object) but ONLY if that
    // delegate's own class also guarantees it only mutates its own internal state - otherwise forwarding
    // to it is exactly as unsafe as calling any other external mutating function would be.
    fun isTrustedDelegateField(expression: IrExpression?): Boolean {
        if (function.origin != IrDeclarationOrigin.DELEGATED_MEMBER) return false
        val field = backingFieldOf(expression) ?: return false
        return isInternalStateClass(field.type.getClass(), purityConfig)
    }

    // Invoking any function value is trusted, same as @Pure/@Readonly (see CheckFunctionPurityVisitor) -
    // intentionally not checking the lambda's own purity annotations; @ModifiesInternalStateOnly must be
    // at least as permissive as @Readonly, which already allows this unconditionally
    fun isInvokingLambda(calledFunction: IrSimpleFunction, receiver: IrExpression?): Boolean =
        calledFunction.name.asString() == "invoke" && receiver?.type?.isFunction() == true

    val descriptor = "Function \"${function.name}\" is marked as @ModifiesInternalStateOnly"

    val errorSeverity = if (function.hasAnnotation(Annotations.TestExpectCompileError)) CompilerMessageSeverity.WARNING else CompilerMessageSeverity.ERROR
    val messages = mutableListOf<String>()

    fun report(message: String, element: IrElement) {
        messageCollector.report(errorSeverity, message, getLocationForExpression(function, element))
        messages.add(message)
    }

    val visitor = object : IrVisitor<Unit, Unit>() {
        override fun visitVariable(declaration: IrVariable, data: Unit) {
            ownedInstances.visitVariable(declaration)
            super.visitVariable(declaration, data)
        }

        override fun visitCall(expression: IrCall, data: Unit) {
            val calledFunction = expression.symbol.owner

            // Calls to subfunctions defined within this function are already checked as part of it
            if (function !in calledFunction.parents) {
                val receiverIndex = calledFunction.parameters
                    .indexOfFirst { it.kind == IrParameterKind.DispatchReceiver || it.kind == IrParameterKind.ExtensionReceiver }
                val rawReceiver = if (receiverIndex != -1) expression.arguments[receiverIndex] else null
                // Smart-casts/`!!` (IrTypeOperatorCall) and `+=`/`*=`-generated temp vals wrap/copy the
                // real receiver without changing its identity - see through both to the real expression
                val unwrappedOnce = if (rawReceiver is IrTypeOperatorCall) rawReceiver.argument else rawReceiver
                val receiver = unwrapCompoundAssignmentTemp(unwrappedOnce)

                val allowed = ExpectedFunctionPurityChecker.isMarkedAsPure(calledFunction, purityConfig)
                        || ExpectedFunctionPurityChecker.isReadonly(calledFunction, purityConfig)
                        || isThis(receiver) // any method called on this instance is trusted, whatever its purity
                        || ownedInstances.isOwned(receiver) // ...as is one called on a newly-owned local instance
                        || (receiver != null && (representsAnnotationBearer(receiver, Annotations.Mutated)
                            || representsAnnotationBearer(receiver, Annotations.LocalState)
                            || representsAnnotationBearer(receiver, Annotations.Cache))) // ...as is one called on a parameter/val this function has mutation rights over
                        || isOwnField(receiver) // ...as is one called on a field of this instance - it's still this instance's own state
                        || isTrustedDelegateField(receiver) // ...as is a `by`-delegated call forwarded to an internal-state delegate
                        || isOwnedReceiverForDefaultSetter(calledFunction, receiver) // ...as is a plain, non-overridable default setter on an owned receiver
                        || isInvokingLambda(calledFunction, receiver) // ...as is invoking any lambda, same as @Pure/@Readonly
                        // Allow setting @Cache properties, same as @Pure/@Readonly
                        || (calledFunction.isSetter && calledFunction.correspondingPropertySymbol?.owner?.hasAnnotation(Annotations.Cache) == true)

                if (!allowed) {
                    report(
                        "$descriptor but calls \"${calledFunction.fqNameForIrSerialization}\", which is not Pure, Readonly, " +
                            "or a method called on this instance, an owned local instance, or a field of this instance.\n" +
                            " - @ModifiesInternalStateOnly may only mutate state that it owns.\n",
                        expression
                    )
                }
            }
            super.visitCall(expression, data)
        }

        override fun visitSetValue(expression: IrSetValue, data: Unit) {
            val varValueDeclaration = expression.symbol.owner
            // Vars created within this function (including subfunctions) are this function's own local state
            val isOwnLocal = varValueDeclaration is IrVariable && function in varValueDeclaration.parents
            if (!isOwnLocal) {
                report(
                    "$descriptor but sets variable \"${varValueDeclaration.name}\", which is not local to this function.\n" +
                        " - @ModifiesInternalStateOnly may only mutate state that it owns.\n",
                    expression
                )
            }
            super.visitSetValue(expression, data)
        }

        override fun visitSetField(expression: IrSetField, data: Unit) {
            val rawReceiver = expression.receiver
            val unwrappedOnce = if (rawReceiver is IrTypeOperatorCall) rawReceiver.argument else rawReceiver
            val receiver = unwrapCompoundAssignmentTemp(unwrappedOnce)
            val isOwnedReceiver = isThis(receiver) || ownedInstances.isOwned(receiver)
            if (!isOwnedReceiver) {
                report(
                    "$descriptor but sets field \"${expression.symbol.owner.name}\" on an object other than this instance or an owned local instance.\n" +
                        " - @ModifiesInternalStateOnly may only mutate state that it owns.\n",
                    expression
                )
            }
            super.visitSetField(expression, data)
        }

        override fun visitElement(element: IrElement, data: Unit) {
            element.acceptChildren(this, data)
        }
    }
    function.accept(visitor, Unit)
    return messages
}
