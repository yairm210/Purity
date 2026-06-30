package yairm210.purity.annotations

import kotlin.annotation.AnnotationTarget.VALUE_PARAMETER

/**
 * Marks a parameter as one that this function will mutate.
 * The function keeps its @Pure or @Readonly annotation — @Mutated only declares that this
 * specific parameter may be modified, while all other purity constraints still apply.
 *
 * A @Pure or @Readonly function may pass a @Mutated argument only if the value is
 * @LocalState, a value type (Int, String, etc.), or itself a @Mutated parameter.
 */
@Target(VALUE_PARAMETER) public annotation class Mutated
