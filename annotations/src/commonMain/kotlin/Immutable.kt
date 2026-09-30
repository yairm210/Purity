package yairm210.purity.annotations

import kotlin.annotation.AnnotationTarget.LOCAL_VARIABLE
import kotlin.annotation.AnnotationTarget.PROPERTY

/**
 * Indicates that a val - such as a list or map - is immutable, and thus its Readonly *internal state* functions should be considered Pure.
 * Has no effect on var, since the underlying value can be changed out from under us we cannot trust its value for Pure functions.
 * 
 * This is a *shallow* indicator - it refers to the instance, and not to contained fields and values.
 * e.g. on a list, it means the list is local, but the values contained in the list may be available elsewhere.
 */
// this differs from IrValueDeclaration.isImmutable - that only checks if it's a val or var,
//   NOT that its internals are modifiable e.g. listOf() vs ArrayListOf() 
@Target(LOCAL_VARIABLE, PROPERTY) public annotation class Immutable
