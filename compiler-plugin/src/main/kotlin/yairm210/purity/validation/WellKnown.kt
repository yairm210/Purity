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
    "kotlin.collections.Iterator.hasNext",
    "kotlin.collections.Iterator.next",
    "kotlin.collections.get",
    "kotlin.collections.getOrNull",
    "kotlin.collections.getOrElse",
    "kotlin.collections.containsKey",
    "kotlin.collections.containsValue",
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
    "kotlin.collections.mutableListOf",
    "kotlin.collections.listOf",
    "kotlin.collections.setOf",
    "kotlin.collections.mapOf",
    "kotlin.sequences.sequenceOf",
    "kotlin.collections.emptyList",
    "kotlin.collections.emptySet",
    "kotlin.collections.emptyMap",
    
    "kotlin.sequences.sequence",
    "kotlin.sequences.SequenceScope.yield",
    "kotlin.sequences.SequenceScope.yieldAll",
    "kotlin.sequences.emptySequence",
    "kotlin.sequences.plus",
) + getCommonSequenceCollectionFunctions()

fun getCommonSequenceCollectionFunctions(): Set<String>{
    val iterableSequenceCommonFunctions = setOf(
        "all",
        "any",
        "asIterable",
        "asSequence",
        "associate",
        "associateBy",
        "associateWith",
        "average",
        "chunked",
        "contains",
        "containsAll",
        "count",
        "distinct",
        "distinctBy",
        "drop",
        "dropWhile",
        "elementAt",
        "elementAtOrElse",
        "elementAtOrNull",
        "filter",
        "filterIndexed",
        "filterNot",
        "filterNotNull",
        "filterTo",
        "find",
        "findLast",
        "first",
        "firstOrNull",
        "flatMap",
        "flatMapIndexed",
        "flatten",
        "fold",
        "groupBy",
        "groupingBy",
        "indexOf",
        "indexOfFirst",
        "indexOfLast",
        "isEmpty",
        "isNotEmpty",
        "iterator",
        "joinToString",
        "last",
        "lastIndexOf",
        "lastOrNull",
        "map",
        "mapIndexed",
        "mapIndexedNotNull",
        "mapNotNull",
        "max",
        "maxBy",
        "maxByOrNull",
        "maxOrNull",
        "min",
        "minBy",
        "minByOrNull",
        "minOrNull",
        "none",
        "partition",
        "reduce",
        "single",
        "singleOrNull",
        "sorted",
        "sortedBy",
        "sortedByDescending",
        "sortedDescending",
        "sortedWith",
        "sortedWithComparator",
        "sum",
        "sumBy",
        "sumByDouble",
        "sumByLong",
        "sumOf",
        "take",
        "takeWhile",
        "toArray",
        "toCollection",
        "toHashSet",
        "toList",
        "toMap",
        "toMutableList",
        "toMutableMap",
        "toMutableSet",
        "toSet",
        "toSortedSet",
        "windowed",
        "zip",
        "zipWithNext"
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
