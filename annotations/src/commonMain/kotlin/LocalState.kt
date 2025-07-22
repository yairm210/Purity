package yairm210.purity.annotations

import kotlin.annotation.AnnotationTarget.LOCAL_VARIABLE
import kotlin.annotation.AnnotationTarget.PROPERTY

/**
 * Indicates that function variable is constructed within the function, and thus all its functions may be considered Pure.
 * This is a stronger version of [Immutable], which only allows for read-only functions to be considered Pure.
 */ 
@Target(LOCAL_VARIABLE, PROPERTY) public annotation class LocalState
