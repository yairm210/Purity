package yairm210.purity.validation.returnsnewinstance.wellknown

/** Functions that return a new instance
 * This means that if the type is InternalState,
 *  the vals created are guaranteed to be LocalState, just like for constructors
 *  This is unnecessary for immutable classes, since they can't be mutated regardless.
 *
 *  Note: being Pure does NOT imply returning a new instance - a Pure function may return a
 *  shared/cached reference (e.g. a val getter), so purity alone must never be used to infer that the returned value is a new instance.
 * */

val wellKnownNewInstanceFunctions = setOf(
    // Collection builder functions - always allocate a new instance.
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

    // These return a shared singleton, not a new instance - but their static type is the
    // read-only (immutable) collection interface, so no caller can ever mutate through it anyway.
    // Practically indistinguishable from a new instance for purity purposes.
    "kotlin.collections.emptyList",
    "kotlin.collections.emptySet",
    "kotlin.collections.emptyMap",

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
    "java.util.BitSet.clone",
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