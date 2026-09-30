package yairm210.purity.annotations

import kotlin.annotation.AnnotationTarget.FUNCTION

/**
 * An annotation to indicate that this function always returns a newly allocated instance, not
 * aliased/shared with its inputs or with external state.
 *
 * This means that if the return type is @ModifiesInternalStateOnly, the resulting value is
 * guaranteed to be LocalState, just like for constructors.
 * This is unnecessary for immutable classes, since they can't be mutated regardless.
 *
 * The equivalent of this for external functions/classes is WellKnownNewInstanceFunctions/WellKnownNewInstanceClasses - see https://yairm210.github.io/Purity/usage/configuration/#handling-external-classes
 *
 * Validated on compilation
 */
@Target(FUNCTION) public annotation class ReturnsNewInstance
