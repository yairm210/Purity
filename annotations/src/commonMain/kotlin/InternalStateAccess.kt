package yairm210.purity.annotations

import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FUNCTION

/**
 * On a function: Can read ONLY INSTANCE-INTERNAL mutable state. Can write ONLY INSTANCE-INTERNAL mutable state.
 * On a class: All functions of the class are at least this restrictive (may annotate functions for further restrictions).
 *
 * Strictly more restrictive than [InternalStateMutation]: that one allows reading anything, this
 * one additionally requires that reads stay within `this` instance (or an instance it owns) as well.
 * Most collection classes (ArrayList, HashMap, etc.) do NOT qualify for this - their functions like
 * `addAll`/`containsAll` read an arbitrary external collection passed as an argument, so they're only
 * [InternalStateMutation], but many of their individual functions - e.g. "add(), get()" - are.
 *
 * Validated on compilation
 */
@Target(CLASS, FUNCTION) public annotation class InternalStateAccess
