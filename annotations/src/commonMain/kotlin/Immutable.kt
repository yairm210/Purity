package yairm210.purity.annotations

import kotlin.annotation.AnnotationTarget.LOCAL_VARIABLE
import kotlin.annotation.AnnotationTarget.PROPERTY

/**
 * Indicates that a val - such as a list or map - is immutable, and thus its Readonly functions should be considered Pure.
 * Has no effect on var, since the underlying value can be changed out from under us we cannot trust its value for Pure functions.
 */
// this differs from IrValueDeclaration.isImmutable - that only checks if it's a val or var,
//   NOT that its internals are modifiable e.g. listOf() vs ArrayListOf() 
@Target(LOCAL_VARIABLE, PROPERTY) public annotation class Immutable
