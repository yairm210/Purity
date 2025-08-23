package yairm210.purity.validation.wellknown


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
    "java.text.DecimalFormat",
    "java.util.Locale",
    "java.util.UUID",
    "java.lang.Integer",
    "java.lang.StackTraceElement",
    "java.lang.Math",
    
)
