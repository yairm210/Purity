package yairm210.purity.annotations

import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.VALUE_PARAMETER

/**
 * An annotation to indicate that this function is Readonly.
 * This means that it does not alter 
 * Functions which read from mutable state can be readonly, but functions which *write* to state cannot be.
 * 
 * The equivalent of this for external functions is WellKnownReadonlyFunctions - see https://yairm210.github.io/Purity/usage/configuration/#handling-external-classes
 */
@Target(FUNCTION, VALUE_PARAMETER, AnnotationTarget.PROPERTY_GETTER) public annotation class Readonly
