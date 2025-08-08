package yairm210.purity.annotations

import kotlin.annotation.AnnotationTarget.LOCAL_VARIABLE
import kotlin.annotation.AnnotationTarget.VALUE_PARAMETER

/**
 * Indicates that the class alters its internal state only, AND that this instance is inaccessible from elsewhere in the code (e.g. created within this function)
 * Thus, its state-mutating functions can be called without violating function purity.
 */ 
@Target(LOCAL_VARIABLE, VALUE_PARAMETER) public annotation class LocalState
