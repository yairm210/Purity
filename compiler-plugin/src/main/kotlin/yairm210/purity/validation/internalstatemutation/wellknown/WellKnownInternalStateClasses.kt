package yairm210.purity.validation.internalstatemutation.wellknown

/** Classes that only mutate their internal state.
 * All their functions are at least InternalStateMutation - individual functions may be more restrictive (e.g. InternalStateAccess, Readonly)
 **/
val wellKnownInternalStateClasses = setOf(
    "kotlin.collections.MutableList",
    "kotlin.collections.MutableSet",
    "kotlin.collections.MutableMap",
    "kotlin.collections.List",
    "kotlin.collections.Set",
    "kotlin.collections.Map",
    "kotlin.collections.ArrayDequeue",

    "java.lang.StringBuilder",
    "java.util.EnumMap",
    "java.util.HashMap",
    "java.util.LinkedHashMap",
    "java.util.ArrayList",
    "java.util.HashSet",
    "java.util.LinkedHashSet",
    "java.util.BitSet",
    "java.util.concurrent.ConcurrentHashMap",
    "java.util.TreeMap",
)
