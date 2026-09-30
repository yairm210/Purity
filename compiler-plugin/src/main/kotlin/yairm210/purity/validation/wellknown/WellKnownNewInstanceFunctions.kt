package yairm210.purity.validation.wellknown

/** Functions that return a new instance
 * This means that if the type is InternalState,
 *  the vals created are guaranteed to be LocalState, just like for constructors
 *  This is unnecessary for immutable classes, since they can't be mutated regardless.
 *
 *  Note: being Pure does NOT imply returning a new instance - a Pure function may return a
 *  shared/cached reference (e.g. a val getter), so purity alone must never be used to infer freshness.
 * */

val wellKnownNewInstanceFunctions = setOf(
    // Collection builder functions - always allocate a fresh instance.
    // Note: emptyList/emptySet/emptyMap are intentionally excluded - they return a shared singleton.
    "kotlin.collections.mutableListOf",
    "kotlin.collections.mutableSetOf",
    "kotlin.collections.mutableMapOf",
    "kotlin.collections.listOf",
    "kotlin.collections.setOf",
    "kotlin.collections.mapOf",
    "kotlin.collections.arrayListOf",
    "kotlin.collections.hashSetOf",
    "kotlin.collections.hashMapOf",
    "kotlin.collections.linkedMapOf",

    "kotlin.collections.distinct",
    "kotlin.collections.distinctBy",
    "kotlin.collections.drop",
    "kotlin.collections.dropWhile",
    "kotlin.collections.filter",
    "kotlin.collections.filterIndexed",
    "kotlin.collections.filterNot",
    "kotlin.collections.filterNotNull",
    "kotlin.collections.flatMap",
    "kotlin.collections.flatMapIndexed",
    "kotlin.collections.flatten",
    "kotlin.text.split",
) + getCommonNewInstanceSequenceIterableFunctions().asSequence()

fun getCommonNewInstanceSequenceIterableFunctions(): Set<String> {

    val iterableSequenceCommonFunctions = setOf(
        // Many of these return a new list for iterator, but NOT for sequence
        "associate",
        "associateBy",
        "associateWith",
        "chunked",
//        "distinctBy",
//        "drop", // not for sequence
//        "dropWhile",
//        "filter",
//        "filterIndexed",
//        "filterNot",
//        "filterNotNull",
//        "flatMap", // not for sequence
//        "flatMapIndexed",
//        "flatten",// not for sequence
        "groupBy",
        "partition",
        
        "sorted",
        "sortedBy",
        "sortedByDescending",
        "sortedDescending",
        "sortedWith",
        "sortedWithComparator",
        
        "toArray",
        "toHashSet",
        "toList",
        "toMap",
        "toMutableList",
        "toMutableMap",
        "toMutableSet",
        "toSet",
        "toSortedSet",
        
        "windowed",
//        "zip", 
//        "zipWithNext"
    )

    val fullyQualifiedFunctionNames = mutableSetOf<String>()
    for (prefix in listOf("kotlin.sequences.", "kotlin.collections.")){
        for (function in iterableSequenceCommonFunctions) {
            fullyQualifiedFunctionNames += prefix + function
        }
    }
    return fullyQualifiedFunctionNames
}