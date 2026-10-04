package yairm210.purity.annotations

import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FUNCTION

/**
 * On a function: Can read ALL mutable state. Can write ONLY INSTANCE-INTERNAL mutable state.
 * On a class: All functions of the class are at least this restrictive (may annotate functions for further restrictions).
 * Example: BFS checker - reads the underlying graph, only updates internal mapping of total-cost-to-node.
 *
 * The equivalent of this for external functions is WellKnownInternalStateMutationFunctions -
 * see https://yairm210.github.io/Purity/usage/configuration/#handling-external-classes
 *
 * Validated on compilation
 */
@Target(CLASS, FUNCTION) public annotation class InternalStateMutation
