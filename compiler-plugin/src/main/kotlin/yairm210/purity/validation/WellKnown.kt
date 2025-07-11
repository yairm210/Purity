package yairm210.purity.validation



// All the functions of these are readonly
val wellKnownReadonlyClasses = setOf(
    "kotlin.String",
    "kotlin.collections.List",
    "kotlin.collections.Set",
    "kotlin.collections.Map",
    "kotlin.collections.Collection",
    "kotlin.collections.Iterator",
    "kotlin.collections.IntIterator",
    "kotlin.sequences.Sequence",
    "kotlin.text.Regex",
    "kotlin.text.MatchResult",
    "kotlin.sequences.Sequence",
)

val wellKnownPureClasses = setOf(
    "kotlin.Int",
    "kotlin.Long",
    "kotlin.Float",
    "kotlin.Double",
    "kotlin.Boolean",
    "kotlin.Char",
    "kotlin.String",

    "kotlin.ranges.IntRange",
    "kotlin.ranges.LongRange",
    "kotlin.ranges.CharRange",
    "kotlin.ranges.FloatRange",
    "kotlin.ranges.DoubleRange",
)

// Where possible, use interfaces that cover a lot of classes
val wellKnownReadonlyFunctions = setOf(
    // Somehow Kotlin manages to make "overridden symbols" for hashmap functions contain AbstractMap and  
    // kotlin.collections.MutableMap and kotlin.collections.Map. But NOT Java.util.Map. WTF?
    "kotlin.collections.Map.isEmpty",
    "kotlin.collections.Map.containsKey",
    "kotlin.collections.Map.containsValue",
    "kotlin.collections.Map.get",
    // Collection
    "java.util.Collection.isEmpty",
    "java.util.Collection.contains",
    "java.util.Collection.size",
    // Kotlin collection extension functions
    "kotlin.collections.asSequence",
    "kotlin.collections.first",
    "kotlin.collections.firstOrNull",
    "kotlin.collections.any",
)

val wellKnownPureFunctions = setOf(
    "kotlin.internal.ir.CHECK_NOT_NULL", // AKA !!
    // Alas, not all of kotlin.collections is pure... 
    "kotlin.collections.listOf",
    "kotlin.collections.setOf",
    "kotlin.collections.mapOf",
    "kotlin.sequences.sequenceOf",
    "kotlin.collections.emptyList",
    "kotlin.collections.emptySet",
    "kotlin.collections.emptyMap",
)

val wellKnownPureFunctionsPrefixes = listOf(
    "kotlin.sequences.",
    "kotlin.text."
)

/** Classes that hold state internally.
 * This means that if this function created that class, and it does not leak, it can call all functions on it and be considered pure
 */
val wellKnownInternalStateClasses = setOf(
    "kotlin.collections.ArrayList",
    "kotlin.collections.HashMap",
    "kotlin.collections.LinkedHashMap",
    "kotlin.collections.HashSet",
    "kotlin.collections.LinkedHashSet",
    "kotlin.text.StringBuilder",
    "java.lang.StringBuilder",
    "java.util.EnumMap",
    "java.util.HashMap",
    "java.util.LinkedHashMap",
    "java.util.ArrayList",
    "java.util.HashSet",
    "java.util.LinkedHashSet",
    "java.util.concurrent.ConcurrentHashMap",
)
    
