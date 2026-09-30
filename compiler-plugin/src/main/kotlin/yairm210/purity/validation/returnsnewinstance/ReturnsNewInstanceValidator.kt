@file:OptIn(UnsafeDuringIrConstructionAPI::class)
package yairm210.purity.validation.returnsnewinstance

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrVisitor
import yairm210.purity.PurityConfig
import yairm210.purity.validation.Annotations
import yairm210.purity.validation.FunctionAnnotations
import yairm210.purity.validation.OwnedInstanceVariableTracker
import yairm210.purity.validation.getLocationForExpression
import yairm210.purity.validation.unwrapSafeCall

/** Builds a suggestion for how to make [calledFunction] recognized as always returning a new instance -
 * annotating it directly if it's part of this compilation and its body already satisfies the rule
 * (checked hypothetically, regardless of whether it's currently annotated/well-known), or declaring
 * it via the FQN config otherwise. Returns null if the function is part of this compilation but its
 * body demonstrably does NOT satisfy the rule - annotating it would just cause a new compile error,
 * and asserting it via the FQN config would be asserting something false, so no suggestion is safe. */
fun suggestReturnsNewInstanceFix(calledFunction: IrFunction, purityConfig: PurityConfig): String? {
    val calledFunctionFqName = calledFunction.fqNameForIrSerialization.asString()
    // A function has an accessible body only if it's part of the current compilation - external/stdlib
    // functions (e.g. kotlin.text.split) are deserialized without one, so we can't annotate or validate them
    val isCompilationAvailable = calledFunction.body != null
    return when {
        !isCompilationAvailable ->
            " - If \"$calledFunctionFqName\" always returns a newly allocated instance not aliased elsewhere, " +
                "you can add \"$calledFunctionFqName\" to wellKnownNewInstanceFunctions via the PurityConfiguration in gradle - " +
                "see https://yairm210.github.io/Purity/usage/advanced-usage/#marking-functions-as-returning-a-new-instance \n"
        checkReturnsNewInstanceBody(calledFunction, purityConfig).isEmpty() ->
            " - You can annotate \"$calledFunctionFqName\" as @ReturnsNewInstance - " +
                "see https://yairm210.github.io/Purity/usage/advanced-usage/#marking-functions-as-returning-a-new-instance \n"
        else -> null
    }
}

/** A @ReturnsNewInstance function (or override of one) must only return: a constructor call, a call to
 * another @ReturnsNewInstance function, or a local val that was itself assigned from one of those. */
internal fun validateReturnsNewInstance(
    function: IrFunction,
    purityConfig: PurityConfig,
    messageCollector: MessageCollector,
): List<String> {
    if (!FunctionAnnotations.ReturnsNewInstance.isExplicitlyMarked(function, purityConfig)) return emptyList()

    val errorSeverity = if (function.hasAnnotation(Annotations.TestExpectCompileError)) CompilerMessageSeverity.WARNING else CompilerMessageSeverity.ERROR
    val messages = mutableListOf<String>()

    for ((message, element) in checkReturnsNewInstanceBody(function, purityConfig)) {
        messageCollector.report(errorSeverity, message, getLocationForExpression(function, element))
        messages.add(message)
    }
    return messages
}

/** Checks [function]'s body against the @ReturnsNewInstance rule, regardless of whether it is
 * currently marked/known as such - i.e. "would this pass if annotated?". Returns one
 * (message, offending IrReturn) pair per violation. */
private fun checkReturnsNewInstanceBody(
    function: IrFunction,
    purityConfig: PurityConfig,
): List<Pair<String, IrElement>> {
    val ownedInstances = OwnedInstanceVariableTracker(purityConfig)
    val violations = mutableListOf<Pair<String, IrElement>>()

    val visitor = object : IrVisitor<Unit, Unit>() {
        override fun visitVariable(declaration: IrVariable, data: Unit) {
            ownedInstances.visitVariable(declaration)
            super.visitVariable(declaration, data)
        }

        override fun visitReturn(expression: IrReturn, data: Unit) {
            if (expression.returnTargetSymbol == function.symbol) {
                val value = unwrapSafeCall(expression.value)
                val isNewInstance = when (value) {
                    is IrConstructorCall -> true
                    is IrCall -> FunctionAnnotations.ReturnsNewInstance.isExplicitlyMarked(value.symbol.owner, purityConfig)
                    is IrGetValue -> ownedInstances.isOwned(value)
                    else -> false
                }
                if (!isNewInstance) {
                    var message = "Function \"${function.name}\" is marked as (or overrides a function marked as) @ReturnsNewInstance " +
                        "but returns a value that is not a constructor call, a call to another @ReturnsNewInstance function, " +
                        "or a local val assigned from one of those.\n"

                    // If the returned value is itself a function call, suggest how to make that function
                    // recognized as returning a new instance too, so it can be used here directly
                    if (value is IrCall) {
                        suggestReturnsNewInstanceFix(value.symbol.owner, purityConfig)?.let { message += it }
                    }

                    violations.add(message to expression)
                }
            }
            super.visitReturn(expression, data)
        }

        override fun visitElement(element: IrElement, data: Unit) {
            element.acceptChildren(this, data)
        }
    }
    function.accept(visitor, Unit)
    return violations
}
