@file:OptIn(UnsafeDuringIrConstructionAPI::class)
package yairm210.purity.validation

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrSetField
import org.jetbrains.kotlin.ir.expressions.IrSetValue
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
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

    // Checked separately by the normal Pure/Readonly purity checks
    if (ExpectedFunctionPurityChecker.isMarkedAsPure(function, purityConfig)) return emptyList()
    if (ExpectedFunctionPurityChecker.isReadonly(function, purityConfig)) return emptyList()

    if (function.body == null) return emptyList()
    if (function.suppressesPurity()) return emptyList()

    val thisReceiver = function.dispatchReceiverParameter
    fun isThis(expression: IrExpression?): Boolean =
        expression is IrGetValue && expression.symbol.owner == thisReceiver

    // A freshly constructed (or @ReturnsNewInstance-returned) local val is guaranteed unaliased, so
    // mutating it is just as safe as mutating `this` - e.g. `val new = Foo(); new.x = 1` in a clone() method
    val ownedInstances = OwnedInstanceVariableTracker(purityConfig)

    val descriptor = "Function \"${function.name}\" is marked as (or is a method of a class marked as) @ModifiesInternalStateOnly"

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
                val receiver = if (receiverIndex != -1) expression.arguments[receiverIndex] else null

                val allowed = ExpectedFunctionPurityChecker.isMarkedAsPure(calledFunction, purityConfig)
                        || ExpectedFunctionPurityChecker.isReadonly(calledFunction, purityConfig)
                        || isThis(receiver) // any method called on this instance is trusted, whatever its purity
                        || ownedInstances.isOwned(receiver) // ...as is one called on a freshly-owned local instance

                if (!allowed) {
                    report(
                        "$descriptor but calls \"${calledFunction.fqNameForIrSerialization}\", which is not Pure, Readonly, " +
                            "or a method called on this instance or an owned local instance.\n" +
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
            val isOwnField = isThis(expression.receiver) || ownedInstances.isOwned(expression.receiver)
            if (!isOwnField) {
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
