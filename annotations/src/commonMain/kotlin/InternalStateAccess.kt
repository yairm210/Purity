package yairm210.purity.annotations

import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FUNCTION

/**
 * On a function: Can read ONLY INSTANCE-INTERNAL mutable state. Can write ONLY INSTANCE-INTERNAL mutable state.
 * On a class: All functions of the class are at least this restrictive (may annotate functions for further restrictions).
 * Examples: ArrayList, HashMap.
 * 
 * The equivalent of this for external classes is WellKnownInternalStateClasses -
 * see https://yairm210.github.io/Purity/usage/configuration/#handling-external-classes
 *
 * Validated on compilation
 */
@Target(CLASS, FUNCTION) public annotation class InternalStateAccess
