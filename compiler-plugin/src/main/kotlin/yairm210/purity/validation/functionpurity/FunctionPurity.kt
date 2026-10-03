package yairm210.purity.validation.functionpurity

/** How much mutable state a function is allowed to read or write:
 *  - [None]: none at all
 *  - [InstanceInternal]: only state belonging to its own instance (`this`, or an instance it owns)
 *  - [Any]: anything, unrestricted
 *
 * Ordered from most to least restrictive, so [MutationLevel.compareTo] (the natural enum ordinal
 * order) directly expresses "is this level at least as restrictive as that one". */
enum class MutationLevel {
    None,
    InstanceInternal,
    Any
}

/** A function's purity is fully described by two independent axes: how much mutable state it may
 * [mutableStateRead], and how much it may [mutableStateWrite]. The four cases below are the only
 * ones the plugin currently validates for, but the two axes are independent in principle - this is
 * not meant to be an exhaustive enumeration of every possible combination. */
enum class FunctionPurity(val mutableStateRead: MutationLevel, val mutableStateWrite: MutationLevel) {
    Pure(MutationLevel.None, MutationLevel.None),
    Readonly(MutationLevel.Any, MutationLevel.None),
    /** `@ModifiesInternalStateOnly`: may read anything, but may only write state it owns. */
    InternalStateMutating(MutationLevel.Any, MutationLevel.InstanceInternal),
    /** Unmarked - no restriction on reads or writes. */
    None(MutationLevel.Any, MutationLevel.Any),
}

/** Is this purity level at least as restrictive, on both axes, as [other] - i.e. does everything
 * allowed by this level fit within what [other] allows? */
fun FunctionPurity.isAtLeastAsRestrictiveAs(other: FunctionPurity): Boolean =
    mutableStateRead <= other.mutableStateRead && mutableStateWrite <= other.mutableStateWrite

/** Strictly more restrictive than [other] - at least as restrictive, and not equal. */
fun FunctionPurity.isStrictlyMoreRestrictiveThan(other: FunctionPurity): Boolean =
    this.isAtLeastAsRestrictiveAs(other) && this != other

/** Can a function declared as [caller] call a function that actually behaves as [callee] - i.e. is
 * [callee] at least as restrictive, on both axes, as [caller] requires? */
fun canCall(caller: FunctionPurity, callee: FunctionPurity): Boolean =
    callee.isAtLeastAsRestrictiveAs(caller)
