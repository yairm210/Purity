package yairm210.purity.annotations

import kotlin.annotation.AnnotationTarget.FUNCTION

/**
 * An annotation to indicate that this function is Pure.
 * This means that it does not have any side effects and its output depends only on its input.
 * Functions which read from mutable state are not pure.
 */
@Target(FUNCTION) public annotation class Readonly
