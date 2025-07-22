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
    "java.util.AbstractCollection.contains",
    "java.util.AbstractCollection.isEmpty",
    "java.util.AbstractCollection.size",
    "java.util.AbstractList.get",
)


// MOST of these are readonly, but some are unfortunately not. :(
// This is easier than making one huge list of all the acceptable functions
// TODO: this is not good - could break in future Kotlin versions.
// All well known functions need to be specified explicitly.

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
    "kotlin.sequences.emptySequence",
) + getCommonSequenceCollectionFunctions()

fun getCommonSequenceCollectionFunctions(): Set<String>{
    val iterableSequenceCommonFunctions = setOf(
        "joinToString",
        "filter",
        "map",
        "flatMap",
        "take",
        "takeWhile",
        "count",
        "find",
        "first",
        "firstOrNull",
        "last",
        "lastOrNull",
        "any",
        "all",
        "none",
        "reduce",
        "fold",
        "sum",
        "sumBy",
        "sumByDouble",
        "sumByLong",
        "max",
        "maxBy",
        "maxByOrNull",
        "maxOrNull",
        "min",
        "minBy",
        "minByOrNull",
        "minOrNull",
        "distinct",
        "distinctBy",
        "sorted",
        "sortedBy",
        "sortedByDescending",
        "sortedDescending",
        "sortedWith",
        "sortedWithComparator",
        "groupBy",
        "groupingBy",
        "partition",
        "zip",
        "zipWithNext",
        "chunked",
        "windowed",
        "associate",
        "associateBy",
        "associateWith",
        "toList",
        "toSet",
        "toMap",
        "toMutableList",
        "toMutableSet",
        "toMutableMap",
        "asSequence",
        "asIterable",
    )
    
    val fullyQualifiedFunctionNames = mutableSetOf<String>()
    for (prefix in listOf("kotlin.sequences.", "kotlin.collections.")){
        for (function in iterableSequenceCommonFunctions) {
            fullyQualifiedFunctionNames += prefix + function
        }
    }
    return fullyQualifiedFunctionNames
}

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
    
