package yairm210.purity.validation



// All the functions of these are readonly
val wellKnownReadonlyClasses = setOf(
    "kotlin.collections.List",
    "kotlin.collections.Set",
    "kotlin.collections.Map",
    "kotlin.collections.Collection",
    "kotlin.collections.Iterator",
    "kotlin.collections.IntIterator",
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
    
    "kotlin.text.Regex", // Even when reading, we read a string, which is immutable
    "kotlin.text.MatchResult",

    "kotlin.ranges.IntRange",
    "kotlin.ranges.LongRange",
    "kotlin.ranges.CharRange",
    "kotlin.ranges.FloatRange",
    "kotlin.ranges.DoubleRange",
    
    "kotlin.enums.EnumEntries",
    "java.text.NumberFormat",
    "java.util.Locale",
    "java.util.UUID",
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
    
    // kotlin collections
    "kotlin.collections.Iterator.hasNext",
    "kotlin.collections.Iterator.next",
    "kotlin.collections.Iterable.iterator",
    "kotlin.collections.get",
    "kotlin.collections.getOrNull",
    "kotlin.collections.getOrElse",
    "kotlin.collections.containsKey",
    "kotlin.collections.containsValue",
    "kotlin.collections.Collection.contains",
    "kotlin.collections.Collection.containsAll",
    "kotlin.collections.dropLastWhile",
    "kotlin.collections.isNullOrEmpty",
    "kotlin.collections.filterKeys",
    "kotlin.collections.minus",
    "kotlin.collections.plus",
    "kotlin.collections.reversed",
    "kotlin.collections.intersect",
    "kotlin.Array.get",
    
    
    // Java Reflection
    "java.lang.Class.getField",
    "java.lang.Class.getFields",
    "java.lang.Class.getMethod",
    "java.lang.Class.getMethods",
    "java.lang.Class.getDeclaredField",
    "java.lang.Class.getDeclaredFields",
    "java.lang.Class.getDeclaredMethod",
    "java.lang.Class.getDeclaredMethods",
    "java.lang.Class.getAnnotation",
    "java.lang.Class.getAnnotations",
    "java.lang.Class.getDeclaredAnnotation",
    "java.lang.Class.getDeclaredAnnotations",
    "java.lang.Class.getPackage",
    "java.lang.Class.getSimpleName",
    "java.lang.Class.getName",
    "java.lang.Class.getCanonicalName",
    "java.lang.Class.getTypeName",
    "java.lang.Class.getInterfaces",
    "java.lang.Class.getSuperclass",
    "java.lang.Class.isArray",
    "java.lang.Class.isEnum",
    "java.lang.Class.isInstance",
    "java.lang.Class.isAssignableFrom",
    "java.lang.Class.isAnnotation",
    "java.lang.Class.isInterface",
    "java.lang.Class.isPrimitive",
    "java.lang.Class.isSynthetic",
    "java.lang.Class.isAnonymousClass",
    "java.lang.Class.isLocalClass",
    "java.lang.Class.isMemberClass",
    "java.lang.Class.getEnumConstants",
    "java.lang.reflect.Field.getAnnotation",
    "java.lang.reflect.Field.getAnnotations",
    "java.lang.reflect.Field.getDeclaredAnnotation",
    "java.lang.reflect.Field.getDeclaredAnnotations",
    "java.lang.reflect.Field.getType",
    "java.lang.reflect.Field.getTypeName",
    "java.lang.reflect.Field.getName",
    "java.lang.reflect.Field.getModifiers",
    "java.lang.reflect.Field.getDeclaringClass",
    "java.lang.reflect.Field.get",
    "java.lang.reflect.Field.getBoolean",
    "java.lang.reflect.Field.getByte",
    "java.lang.reflect.Field.getChar",
    "java.lang.reflect.Field.getShort",
    "java.lang.reflect.Field.getInt",
    "java.lang.reflect.Field.getLong",
    "java.lang.reflect.Field.getFloat",
    "java.lang.reflect.Field.getDouble",
    "java.lang.reflect.Field.getGenericType",
    "java.lang.reflect.Field.getGenericTypeName",
    "java.lang.reflect.Field.getGenericType",
    
    // Kotlin Reflection
    "kotlin.reflect.KMutableProperty0.get",
    "kotlin.reflect.KMutableProperty1.get",
    "kotlin.reflect.KMutableProperty2.get",
    
)  + getCommonSequenceIterableFunctions()


// MOST of these are readonly, but some are unfortunately not. :(
// This is easier than making one huge list of all the acceptable functions
// TODO: this is not good - could break in future Kotlin versions.
// All well known functions need to be specified explicitly.

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
)

fun getCommonSequenceIterableFunctions(): Set<String>{
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
        "maxOfOrNull",
        "maxOrNull",
        "min",
        "minBy",
        "minByOrNull",
        "minOf",
        "minOfOrNull",
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
        "withIndex",
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
