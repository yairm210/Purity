package yairm210.purity.annotations

import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FUNCTION

/**
 * Indicates that this class/function can mutate *only* state that it owns - lost upon destruction of the instance.
 *
 * On a class: every non-Pure/Readonly method of the class may only call functions that are
 * themselves Pure, Readonly, or called on `this` instance, and may only set vars/fields local to
 * the function or belonging to `this` instance - never external state, and never another instance's
 * state (even of the same class).
 *
 * On a function: that specific function follows the same contract, regardless of whether its
 * class is annotated.
 *
 * The equivalent of this for external classes/functions is WellKnownInternalStateClasses/WellKnownModifiesInternalStateOnlyFunctions -
 * see https://yairm210.github.io/Purity/usage/configuration/#handling-external-classes
 *
 * Validated on compilation
 */
@Target(CLASS, FUNCTION) public annotation class ModifiesInternalStateOnly
