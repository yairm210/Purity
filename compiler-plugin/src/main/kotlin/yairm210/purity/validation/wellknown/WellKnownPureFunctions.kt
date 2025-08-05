package yairm210.purity.validation.wellknown


val wellKnownPureFunctions = setOf(
    "kotlin.internal.ir.CHECK_NOT_NULL", // AKA !!
    "kotlin.internal.ir.noWhenBranchMatchedException",
    "kotlin.to",
    "kotlin.assert",
    "kotlin.require",
    "kotlin.requireNotNull",
    "kotlin.check",
    "kotlin.checkNotNull",

    "kotlin.let",
    "kotlin.run",
    "kotlin.also",
    "kotlin.apply",
    "kotlin.takeIf",
    "kotlin.takeUnless",

    "kotlin.collections.mutableListOf",
    "kotlin.collections.mutableSetOf",
    "kotlin.collections.listOf",
    "kotlin.collections.arrayListOf",
    "kotlin.collections.setOf",
    "kotlin.collections.hashSetOf",
    "kotlin.collections.mapOf",
    "kotlin.collections.hashMapOf",
    "kotlin.sequences.sequenceOf",
    "kotlin.collections.emptyList",
    "kotlin.collections.emptySet",
    "kotlin.collections.emptyMap",

    "kotlin.collections.component1", // required for destructuring declarations
    "kotlin.collections.component2",

    "kotlin.sequences.sequence",
    "kotlin.sequences.SequenceScope.yield",
    "kotlin.sequences.SequenceScope.yieldAll",
    "kotlin.sequences.emptySequence",

    "kotlin.io.println", // not TECHNICALLY pure, but PRACTICALLY pure - used for debug logs
    "kotlin.io.print",
)

val wellKnownPureFunctionsPrefixes = listOf(
    "kotlin.text.",
    "kotlin.ranges.",
    "kotlin.math.",
    "kotlin.comparisons.",
    "kotlin.random.",
)

