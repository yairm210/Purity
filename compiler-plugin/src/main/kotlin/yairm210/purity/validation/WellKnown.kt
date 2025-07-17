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
    "kotlin.Number",
    "kotlin.Boolean",
    "kotlin.Char",
    "kotlin.String",

    "kotlin.ranges.IntRange",
    "kotlin.ranges.LongRange",
    "kotlin.ranges.CharRange",
    "kotlin.ranges.FloatRange",
    "kotlin.ranges.DoubleRange",
    
    "kotlin.enums.EnumEntries",
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
)

// MOST of these are readonly, but some are unfortunately not. :(
// This is easier than making one huge list of all the acceptable functions
// TODO: Maybe run this statically and generate a compile-time list of all functions, so we don't need String comparisons
fun isWellKnownIterableOrSequenceFunction(fqname: String): Boolean {
    if (!fqname.startsWith("kotlin.collections.") && !fqname.startsWith("kotlin.sequences.")) return false
    if (fqname.endsWith("to")) return false // These get a collection and add to it
    if (fqname.endsWith("toCollection")) return false
    return true
}

val wellKnownPureFunctions = setOf(
    "kotlin.internal.ir.CHECK_NOT_NULL", // AKA !!

    "kotlin.let",
    "kotlin.run",
    "kotlin.also",
    "kotlin.apply",
    "kotlin.takeIf",
    "kotlin.takeUnless",
    
    // Alas, not all of kotlin.collections is pure... 
    "kotlin.collections.listOf",
    "kotlin.collections.setOf",
    "kotlin.collections.mapOf",
    "kotlin.sequences.sequenceOf",
    "kotlin.collections.emptyList",
    "kotlin.collections.emptySet",
    "kotlin.collections.emptyMap",
    
    "kotlin.sequences.sequence",
    "kotlin.sequences.SequenceScope.yield",
)

val wellKnownPureFunctionsPrefixes = listOf(
    "kotlin.text.",
    "kotlin.ranges.",
    "kotlin.math.",
    "kotlin.comparisons."
)

/** Classes that hold state internally.
 * This means that if this function created that class, and it does not leak, it can call all functions on it and be considered pure
 */
val wellKnownInternalStateClasses = setOf(
    "java.lang.StringBuilder",
    "java.util.EnumMap",
    "java.util.HashMap",
    "java.util.LinkedHashMap",
    "java.util.ArrayList",
    "java.util.HashSet",
    "java.util.LinkedHashSet",
    "java.util.concurrent.ConcurrentHashMap",
)
    
