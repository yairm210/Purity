package yairm210.purity.annotations

import kotlin.annotation.AnnotationTarget.CLASS

/**
 * Indicates that this class can mutate *only* state that it owns - lost upon destruction of the instance.
 * The equivalent of this for external classes is WellKnownInternalStateClasses - see https://yairm210.github.io/Purity/usage/configuration/#handling-external-classes
 *
 * Validated on compilation
 */
@Deprecated(
    "Renamed to InternalStateAccess, which can also be placed on functions - not just classes",
    ReplaceWith("InternalStateAccess")
)
@Target(CLASS) public annotation class InternalState
