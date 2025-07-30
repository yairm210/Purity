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
    "java.text.NumberFormat",
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
    "java.util.AbstractCollection.iterator",
    "java.util.AbstractList.get",
    "kotlin.collections.Iterator.hasNext",
    "kotlin.collections.Iterator.next",
    "kotlin.collections.MutableCollection.iterator",
    "kotlin.collections.get",
    "kotlin.collections.getOrNull",
    "kotlin.collections.getOrElse",
    "kotlin.collections.containsKey",
    "kotlin.collections.containsValue",
    "kotlin.collections.Collection.contains",
    "kotlin.collections.dropLastWhile",
    "kotlin.collections.isNullOrEmpty",
)  + getCommonSequenceCollectionFunctions() + mutableSetOf()


// MOST of these are readonly, but some are unfortunately not. :(
// This is easier than making one huge list of all the acceptable functions
// TODO: this is not good - could break in future Kotlin versions.
// All well known functions need to be specified explicitly.

val wellKnownPureFunctions = setOf(
    "kotlin.internal.ir.CHECK_NOT_NULL", // AKA !!
    "kotlin.internal.ir.noWhenBranchMatchedException",
    "kotlin.to",

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
)

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
        "forEach",
        "forEachIndexed",
        "groupBy",
        "groupingBy",
        "ifEmpty",
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
        "maxOf",
        "maxOrNull",
        "min",
        "minBy",
        "minByOrNull",
        "minOf",
        "minOrNull",
        "none",
        "partition",
        "plus",
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
    "kotlin.comparisons.",
    "kotlin.random.",
)
