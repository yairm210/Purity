@file:OptIn(UnsafeDuringIrConstructionAPI::class)
package yairm210.purity.validation.modifiesinternalstate

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
 * Validates the contract of `@ModifiesInternalStateOnly`: "can mutate *only* state that it owns" -
 * i.e. may read anything, but may only write state it owns ([FunctionPurity.InternalStateMutating]:
 * read:Any, write:InstanceInternal). Delegates the actual per-statement checking to
 * [CheckFunctionPurityVisitor], the same checker used for `@Pure`/`@Readonly` - this function just
 * handles the pre-checks specific to how `@ModifiesInternalStateOnly` gets applied (class-level
 * marking, local functions, already-Pure/Readonly functions).
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

    val visitor = CheckFunctionPurityVisitor(function, FunctionPurity.InternalStateMutating, messageCollector, purityConfig)
    function.accept(visitor, Unit)
    return visitor.reportedMessages
}
