package yairm210.purity.annotations

import kotlin.annotation.AnnotationTarget.FUNCTION

/**
 * An annotation to indicate that this function always returns a newly allocated instance, making it inaccessible from anywhere else.
 *
 * This means that if the return type is InternalState, the resulting value is guaranteed to be
 * LocalState, just like for constructors.
 * This is unnecessary for immutable classes, or for functions known to be pure -
 * they must by definition return a new instance, or they wouldn't be able to return consistent results.
 *
 * The equivalent of this for external functions is WellKnownNewInstanceFunctions - see https://yairm210.github.io/Purity/usage/configuration/#handling-external-classes
 */
@Target(FUNCTION) public annotation class ReturnsNewInstance
