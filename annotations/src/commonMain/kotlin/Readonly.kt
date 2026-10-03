package yairm210.purity.annotations

import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.VALUE_PARAMETER

/**
 * An annotation to indicate that this function is Readonly.
 * This means that it does not alter state.
 * Functions which *read* from mutable state can be readonly, but functions which *write* to state are not.
 *
 * Can read ALL mutable state. Can write NO mutable state.
 *
 * The equivalent of this for external functions/classes is WellKnownReadonlyFunctions/WellKnownReadonlyClasses - see https://yairm210.github.io/Purity/usage/configuration/#handling-external-classes
 *
 * Validated on compilation
 */
@Target(FUNCTION, VALUE_PARAMETER, AnnotationTarget.PROPERTY_GETTER) public annotation class Readonly
