package yairm210.purity.annotations

import kotlin.annotation.AnnotationTarget.LOCAL_VARIABLE

/**
 * Indicates that a val - such as a list or map - is immutable, and thus its Readable functions should be considered Pure
 */
// this differs from IrValueDeclaration.isImmutable - that only checks if it's a val or var,
//   NOT that its internals are modifiable e.g. listOf() vs ArrayListOf() 
@Target(LOCAL_VARIABLE) public annotation class Immutable
