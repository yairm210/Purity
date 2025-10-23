package yairm210.purity.validation.wellknown

/** Functions that return a new instance
 * This means that if the type is InternalState, 
 *  the vals created are guaranteed to be LocalState, just like for constructors
 *  This is unnecessary for immutable classes, or for functions known to be pure -
 *    They must by definition return a new instance, or they wouldn't be able to return consistent results
 * */

val wellKnownNewInstanceFunctions = setOf(
    "kotlin.collections.distinct",
    "kotlin.collections.distinctBy",
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