package yairm210.purity.annotations

import kotlin.annotation.AnnotationTarget.FUNCTION

/**
 * A test-only annotation that indicates that the annotated function is EXPECTED to error
 * Used for plugin tests.
 */
@Target(FUNCTION) public annotation class TestExpectCompileError
