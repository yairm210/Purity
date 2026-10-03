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
 * [mutableStateRead], and how much it may [mutableStateWrite]. The cases below are the only
 * ones the plugin currently validates for, but the two axes are independent in principle - this is
 * not meant to be an exhaustive enumeration of every possible combination. */
enum class FunctionPurity(val mutableStateRead: MutationLevel, val mutableStateWrite: MutationLevel) {
    /** `@Pure`: can read NO mutable state. Can write NO mutable state. */
    Pure(MutationLevel.None, MutationLevel.None),
    /** `@Readonly`: can read ALL mutable state. Can write NO mutable state. */
    Readonly(MutationLevel.Any, MutationLevel.None),
    /** `@InternalStateMutation`: can read ALL mutable state. Can write ONLY INSTANCE-INTERNAL mutable state. */
    InternalStateMutation(MutationLevel.Any, MutationLevel.InstanceInternal),
    /** `@InternalStateAccess`: can read ONLY INSTANCE-INTERNAL mutable state. Can write ONLY
     * INSTANCE-INTERNAL mutable state. Strictly more restrictive than [InternalStateMutation]. */
    InternalStateAccess(MutationLevel.InstanceInternal, MutationLevel.InstanceInternal),
    /** Can read ONLY INSTANCE-INTERNAL mutable state. Can write NO mutable state - the harshest
     * (per-axis) combination of `@Readonly` and `@InternalStateAccess`. Not reachable via a single
     * annotation - only arises when a function is marked as both at once; see [combineHarshest].
     * This IS a useful distinction since this function can be called directly by Readonly functions,
     * And also called on owned instances even in Pure functions. */
    InternalStateReadonly(MutationLevel.InstanceInternal, MutationLevel.None),
    /** Unmarked: can read ALL mutable state. Can write ALL mutable state - no restriction. */
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

/** Combines several simultaneously-applicable purity levels into the single harshest (most
 * restrictive, per axis) result - used when a function is, somehow, marked by more than one level
 * at once (e.g. both `@Readonly` and `@InternalStateAccess`). Returns [FunctionPurity.None] (no
 * restriction) if [levels] is empty. Every combination of {[FunctionPurity.Pure], [FunctionPurity.Readonly],
 * [FunctionPurity.InternalStateMutation], [FunctionPurity.InternalStateAccess]} maps onto one of the
 * named entries above - if that were ever no longer true, this throws rather than silently losing a restriction. */
fun combineHarshest(levels: List<FunctionPurity>): FunctionPurity {
    if (levels.isEmpty()) return FunctionPurity.None
    val combinedRead = levels.minOf { it.mutableStateRead }
    val combinedWrite = levels.minOf { it.mutableStateWrite }
    return FunctionPurity.entries.first { it.mutableStateRead == combinedRead && it.mutableStateWrite == combinedWrite }
}
