@file:OptIn(UnsafeDuringIrConstructionAPI::class)
package yairm210.purity.validation.internalstateaccess

import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.*
import yairm210.purity.PurityConfig
import yairm210.purity.validation.FunctionAnnotations
import yairm210.purity.validation.functionpurity.CheckFunctionPurityVisitor
import yairm210.purity.validation.functionpurity.ExpectedFunctionPurityChecker
import yairm210.purity.validation.functionpurity.FunctionPurity
import yairm210.purity.validation.suppressesPurity

/**
 * Validates the contract of `@InternalStateAccess`: "can only read and mutate state that it owns" -
 * i.e. may read AND write only state it owns ([FunctionPurity.InternalStateAccess]: read:InstanceInternal,
 * write:InstanceInternal; strictly more restrictive than [FunctionPurity.InternalStateMutation]).
 * Delegates the actual per-statement checking to [CheckFunctionPurityVisitor], the same checker used
 * for `@Pure`/`@Readonly`/`@InternalStateMutation` - this function just handles the pre-checks
 * specific to how `@InternalStateAccess` gets applied (class-level marking, local functions,
 * already-Pure/Readonly functions).
 */
internal fun validateInternalStateAccess(
    function: IrFunction,
    purityConfig: PurityConfig,
    messageCollector: MessageCollector,
): List<String> {
    if (!FunctionAnnotations.InternalStateAccess.isExplicitlyMarked(function, purityConfig)) return emptyList()
    if (function !is IrSimpleFunction) return emptyList()

    // Local functions (declared inside another function) are not themselves top-level members of
    // the class - see the identical note in InternalStateMutationValidator.
    if (function.parent !is IrClass) return emptyList()

    // Checked separately by the normal Pure/Readonly purity checks
    if (ExpectedFunctionPurityChecker.isMarkedAsPure(function, purityConfig)) return emptyList()
    if (ExpectedFunctionPurityChecker.isReadonly(function, purityConfig)) return emptyList()

    if (function.body == null) return emptyList()
    if (function.suppressesPurity()) return emptyList()

    val visitor = CheckFunctionPurityVisitor(function, FunctionPurity.InternalStateAccess, messageCollector, purityConfig)
    function.accept(visitor, Unit)
    return visitor.reportedMessages
}
