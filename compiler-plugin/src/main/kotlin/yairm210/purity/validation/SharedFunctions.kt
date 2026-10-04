@file:OptIn(UnsafeDuringIrConstructionAPI::class)
package yairm210.purity.validation

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrFileEntry
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.expressions.IrWhen
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.FqName
import yairm210.purity.PurityConfig
import yairm210.purity.validation.internalstateaccess.wellknown.wellKnownInternalStateClasses

/**
 * Functions and classes shared between two or more of the purity-checking usecases
 * ([yairm210.purity.validation.functionpurity], [yairm210.purity.validation.modifiesinternalstate],
 * [yairm210.purity.validation.returnsnewinstance]) - kept here rather than duplicated or awkwardly
 * homed in just one of them.
 */

fun getLocationForExpression(function: IrFunction, expression: IrElement): CompilerMessageLocation {
    // Some IR nodes (e.g. synthetic super-calls, compiler-generated overrides) have no source offset
    // of their own - fall back to the enclosing function's location rather than reporting a bogus 1:1
    val located = if (expression.startOffset >= 0) expression else function
    return getLocationForExpression(function.fileEntry, located)
}

fun getLocationForExpression(
    fileEntry: IrFileEntry,
    expression: IrElement
): CompilerMessageLocation {
    val lineAndColumn = fileEntry.getLineAndColumnNumbers(expression.startOffset)
    return CompilerMessageLocation.create(
        path = fileEntry.name,
        line = lineAndColumn.line + 1, // Convert to 1-indexed
        column = lineAndColumn.column + 1, // Convert to 1-indexed
        lineContent = null
    )!!
}

/** Safe calls (`a?.b()`) desugar to an IrBlock/IrWhen wrapping the null-check and the real call
 * (roughly: `val tmp = a; if (tmp == null) null else tmp.b()`) - unwraps down to the underlying
 * expression (e.g. the IrCall to `b`) so purity checks can see through the null-safety wrapper. */
internal fun unwrapSafeCall(expression: IrExpression): IrExpression {
    var current = expression
    while (true) {
        current = when (current) {
            is IrBlock -> current.statements.lastOrNull() as? IrExpression ?: return current
            is IrWhen -> current.branches.firstOrNull { branch ->
                val result = branch.result
                !(result is IrConst && result.value == null)
            }?.result ?: return current
            else -> return current
        }
    }
}

/** Does [irExpression] represent something bearing [annotation] - a local var/parameter annotated
 * directly, a smart-cast/`!!`-wrapped one, or a getter call for a property annotated with it? */
internal fun representsAnnotationBearer(irExpression: IrExpression, annotation: FqName): Boolean {
    if (irExpression is IrGetValue) { // local function variable
        return irExpression.symbol.owner.hasAnnotation(annotation)
    }
    if (irExpression is IrTypeOperatorCall){ // This seems to be type conversion? I'm not sure honestly, seems pretty random to me where it pops up
        return representsAnnotationBearer(irExpression.argument, annotation)
    }
    if (irExpression is IrCall) {
        return irExpression.symbol.owner.let {
            it == it.correspondingPropertySymbol?.owner?.getter // A getter..
                    // ... for a property that is immutable
                    && it.correspondingPropertySymbol?.owner?.hasAnnotation(annotation) == true
        }
    }
    return false
}

/** Is [function] a compiler-generated default property setter (no custom body - just `field = value`),
 * that is also `final`? Such a setter is exactly equivalent to a raw field set, with zero risk of any
 * other side effect, regardless of what class it belongs to - so it can be trusted on any receiver the
 * caller already has mutation/ownership rights over, without needing that class to otherwise be
 * "safe" (e.g. well-known-internal-state or @InternalStateMutation/@InternalStateAccess). Must be `final`: an `open`/
 * overridable setter could resolve, at runtime, to a subclass override with arbitrary side effects,
 * even though this call site's symbol still points at the (harmless) base default accessor. */
internal fun isTrustedDefaultSetter(function: IrSimpleFunction): Boolean =
    function.isSetter && function.origin == IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR && function.modality == Modality.FINAL

/** Is [function] a compiler-generated default property getter (no custom body - just `return field`),
 * that is also `final`? Such a getter is exactly equivalent to a raw field read, with zero risk of any
 * side effect - reading a value through it is no different, capability-wise, than already being handed
 * that value directly. Used to trust reading a VAR property of ANY receiver (even another instance)
 * under the instance-boundary read restriction (@InternalStateAccess), the same way val-getters are
 * already unconditionally trusted as @Pure. Must be `final` for the same reason as [isTrustedDefaultSetter]. */
internal fun isTrustedDefaultGetter(function: IrSimpleFunction): Boolean =
    function.isGetter && function.origin == IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR && function.modality == Modality.FINAL

/** `+=`/`*=` (and smart-casts/`!!` on a nullable) evaluate the receiver once into a compiler-generated
 * temp val to avoid re-evaluating a possibly-side-effecting expression, e.g. `equivalentOffer.amount +=
 * x` (where `equivalentOffer` is a nullable `@LocalState val`) lowers to roughly `val tmp = <not-null
 * check>(equivalentOffer); tmp.amount = tmp.amount + x`. Unwraps such a temp down to the real underlying
 * expression it copies, so annotation/ownership checks on the original declaration still see through it. */
internal fun unwrapCompoundAssignmentTemp(expression: IrExpression?): IrExpression? {
    if (expression !is IrGetValue) return expression
    val owner = expression.symbol.owner
    if (owner !is IrVariable || owner.isVar) return expression
    val initializer = owner.initializer ?: return expression
    val unwrappedInitializer = if (initializer is IrTypeOperatorCall) initializer.argument else initializer
    // Only unwrap simple alias chains (temp := otherVal) - not e.g. `val tmp = Foo()`, where unwrapping
    // would change the expression's identity/meaning rather than just seeing through a compiler-inserted copy
    if (unwrappedInitializer !is IrGetValue) return expression
    return unwrapCompoundAssignmentTemp(unwrappedInitializer)
}

/** Is [irClass] one whose own behavior is fully self-contained - marked @InternalStateAccess or
 * @InternalStateMutation (directly, the deprecated legacy @InternalState, or via well-known FQN,
 * built-in or user config), OR a subclass/implementation of one that is. Either annotation qualifies:
 * @InternalStateAccess is strictly stricter than @InternalStateMutation, so satisfying it satisfies
 * both needs. The superclass/interface walk mirrors FunctionAnnotations.isExplicitlyMarked's own
 * override-chain check, which already holds a subclass's overriding functions to the marked ancestor's
 * contract - so trusting instances of that subclass the same way here isn't a new loophole, it just
 * lets such a subclass actually benefit from the restriction it's already held to. */
internal fun isInternalStateClass(irClass: IrClass?, purityConfig: PurityConfig): Boolean {
    if (irClass == null) return false
    if (irClass.hasAnnotation(Annotations.InternalStateAccess)) return true
    if (irClass.hasAnnotation(Annotations.InternalStateMutation)) return true
    if (irClass.hasAnnotation(Annotations.InternalState)) return true // Deprecated - see InternalStateAccess
    val fullyQualifiedClassName = irClass.fqNameForIrSerialization.asString()
    if (fullyQualifiedClassName in wellKnownInternalStateClasses) return true
    if (fullyQualifiedClassName in purityConfig.wellKnownInternalStateClassesFromUser) return true
    if (irClass.superTypes.any { isInternalStateClass(it.getClass(), purityConfig) }) return true
    return false
}

/** Is [initializer] (after unwrapping safe calls) a constructor call, or a call to a function known
 * to return a new instance (@ReturnsNewInstance, or well-known e.g. toMutableList())? Such a value is
 * guaranteed newly-allocated and unaliased at the point of initialization. */
internal fun isNewInstanceExpression(initializer: IrExpression?, purityConfig: PurityConfig): Boolean {
    val unwrapped = initializer?.let { unwrapSafeCall(it) } ?: return false
    if (unwrapped is IrConstructorCall) return true
    if (unwrapped is IrCall && FunctionAnnotations.ReturnsNewInstance.isExplicitlyMarked(unwrapped.symbol.owner, purityConfig)) return true
    return false
}

/** Is [field] a `val` whose declared initializer is a new-instance expression (see [isNewInstanceExpression])?
 * Used to trust field-of-`this` access the same way a newly-constructed local val is trusted - without this,
 * a field could be "whitewashed": declare it as an internal-state-class val, but actually assign it (via the
 * constructor, say) a reference some external caller still holds, then mutate it through a call on the field. */
internal fun isNewInstanceField(field: IrField, purityConfig: PurityConfig): Boolean =
    field.isFinal && isNewInstanceExpression(field.initializer?.expression, purityConfig)

/** Tracks local vals that are guaranteed to hold a newly-allocated, unaliased instance - either a
 * direct constructor call, or a call to a function known to return a new instance (@ReturnsNewInstance,
 * or well-known e.g. toMutableList()). Feed every [IrVariable] in scope to [visitVariable], then use
 * [isOwned]/[contains] to check whether an expression/variable refers to one of the tracked instances.
 *
 * If [requireInternalStateClass] is set, only instances whose type is itself a well-known/annotated
 * internal-state class are tracked - used where the mutation-rights grant should be limited to such
 * (e.g. @Mutated parameter passing under @Pure/@Readonly). Otherwise, any newly-owned instance counts,
 * regardless of its type (e.g. @InternalStateMutation's clone-and-mutate pattern, or @ReturnsNewInstance's
 * "local val assigned from a constructor/another @ReturnsNewInstance call" rule). */
internal class OwnedInstanceVariableTracker(
    private val purityConfig: PurityConfig,
    private val requireInternalStateClass: Boolean = false,
) {
    private val trackedVariables = HashSet<IrVariable>()

    fun visitVariable(declaration: IrVariable) {
        if (declaration.isVar) return
        if (!isNewInstanceExpression(declaration.initializer, purityConfig)) return
        val initializer = declaration.initializer?.let { unwrapSafeCall(it) }
        if (requireInternalStateClass && !isInternalStateClass(initializer?.type?.getClass(), purityConfig)) return
        trackedVariables.add(declaration)
    }

    fun isOwned(expression: IrExpression?): Boolean =
        expression is IrGetValue && contains(expression.symbol.owner)

    fun contains(variable: IrValueDeclaration): Boolean = variable is IrVariable && variable in trackedVariables
}

internal fun IrAnnotationContainer.suppressesPurity(): Boolean {
    val suppressFqName = FqName("kotlin.Suppress")
    val suppressAnnotations = annotations.filter { it.isAnnotation(suppressFqName) }
    if (suppressAnnotations.isEmpty()) return false
    @Suppress("UNCHECKED_CAST")
    val suppressParameters: List<String> = suppressAnnotations
        .flatMap { annotation -> annotation.arguments }
        .flatMap { (it as IrVarargImpl).elements }
        .filterIsInstance<IrConst>()
        .map { it.value.toString() }
    return suppressParameters.contains("purity")
}

// overriddenSymbols only gives you the *direct* overrides, not the transitive ones.
internal fun getAllOverriddenFunctions(function: IrSimpleFunction): Sequence<IrSimpleFunction> {
    return function.overriddenSymbols.asSequence()
        .flatMap {
            val owner = it.owner
            sequenceOf(owner) + getAllOverriddenFunctions(owner)
        }
}
