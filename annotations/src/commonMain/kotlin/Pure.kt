package yairm210.purity.annotations

import kotlin.annotation.AnnotationTarget.FUNCTION

/**
 * An annotation to indicate that this function is Pure.
 * This means that it does not have any side effects and its output depends only on its input.
 * Functions which read from mutable state are not pure.
 *
 * Can read NO mutable state. Can write NO mutable state.
 *
 * The equivalent of this for external functions/classes is WellKnownPureFunctions/WellKnownPureClasses - see https://yairm210.github.io/Purity/usage/configuration/#handling-external-classes
 *
 * Validated on compilation
 */
@Target(FUNCTION, AnnotationTarget.VALUE_PARAMETER) public annotation class Pure
