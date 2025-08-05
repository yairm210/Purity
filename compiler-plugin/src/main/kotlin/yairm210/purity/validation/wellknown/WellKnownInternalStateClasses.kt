package yairm210.purity.validation.wellknown

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
