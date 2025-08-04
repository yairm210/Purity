package yairm210.purity

class PurityConfig {
    var warnOnPossibleAnnotations: Boolean = false
    var wellKnownPureClassesFromUser: Set<String> = setOf()
    var wellKnownPureFunctionsFromUser: Set<String> = setOf()
    var wellKnownReadonlyClassesFromUser: Set<String> = setOf()
    var wellKnownReadonlyFunctionsFromUser: Set<String> = setOf()
    var wellKnownInternalStateClassesFromUser: Set<String> = setOf()
}