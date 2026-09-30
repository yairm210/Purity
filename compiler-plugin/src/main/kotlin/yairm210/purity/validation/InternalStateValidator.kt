@file:OptIn(UnsafeDuringIrConstructionAPI::class)
package yairm210.purity.validation

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.IrElement
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
 * Validates the contract of `@InternalState`: "can mutate *only* state that it owns".
 *
 * For a method of an `@InternalState` class that is not itself `@Pure`/`@Readonly` (those are
 * checked separately, by the normal purity checks), this ensures:
 *  A. every called function is either Pure, Readonly, or a method called on *this instance*
 *     (own methods, whatever their purity, are trusted to maintain the invariant themselves) -
 *     calling a method on a DIFFERENT instance (even of the same InternalState class) requires
 *     that call itself to be Pure/Readonly
 *  B. every var/field set is either local to this function, or a field of *this instance* - never
 *     a captured outer var, or a field on some other object (even another instance of this class)
 */
fun validateInternalStateMethod(
    function: IrSimpleFunction,
    purityConfig: PurityConfig,
    messageCollector: MessageCollector,
): List<String> {
    val parentClass = function.parentClassOrNull ?: return emptyList()
    if (!isInternalStateClass(parentClass, purityConfig)) return emptyList()

    // Checked separately by the normal Pure/Readonly purity checks
    if (ExpectedFunctionPurityChecker.isMarkedAsPure(function, purityConfig)) return emptyList()
    if (ExpectedFunctionPurityChecker.isReadonly(function, purityConfig)) return emptyList()

    if (function.body == null) return emptyList()
    if (function.suppressesPurity()) return emptyList()

    val thisReceiver = function.dispatchReceiverParameter
    fun isThis(expression: IrExpression?): Boolean =
        expression is IrGetValue && expression.symbol.owner == thisReceiver

    val errorSeverity = if (function.hasAnnotation(Annotations.TestExpectCompileError)) CompilerMessageSeverity.WARNING else CompilerMessageSeverity.ERROR
    val messages = mutableListOf<String>()

    fun report(message: String, element: IrElement) {
        messageCollector.report(errorSeverity, message, getLocationForExpression(function, element))
        messages.add(message)
    }

    val visitor = object : IrVisitor<Unit, Unit>() {
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

                if (!allowed) {
                    report(
                        "Function \"${function.name}\" is a method of @InternalState class \"${parentClass.name}\" " +
                            "but calls \"${calledFunction.fqNameForIrSerialization}\", which is not Pure, Readonly, " +
                            "or a method called on this instance.\n" +
                            " - @InternalState classes may only mutate state that they own.\n",
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
                    "Function \"${function.name}\" is a method of @InternalState class \"${parentClass.name}\" " +
                        "but sets variable \"${varValueDeclaration.name}\", which is not local to this function.\n" +
                        " - @InternalState classes may only mutate state that they own.\n",
                    expression
                )
            }
            super.visitSetValue(expression, data)
        }

        override fun visitSetField(expression: IrSetField, data: Unit) {
            val isOwnField = isThis(expression.receiver)
            if (!isOwnField) {
                report(
                    "Function \"${function.name}\" is a method of @InternalState class \"${parentClass.name}\" " +
                        "but sets field \"${expression.symbol.owner.name}\" on an object other than this instance.\n" +
                        " - @InternalState classes may only mutate state that they own.\n",
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
