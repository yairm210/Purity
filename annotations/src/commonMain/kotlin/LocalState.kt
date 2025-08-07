package yairm210.purity.annotations

import kotlin.annotation.AnnotationTarget.LOCAL_VARIABLE
import kotlin.annotation.AnnotationTarget.VALUE_PARAMETER

/**
 * On a class, this indicates that it can mutate *only* state that it "owns"
 * On a function variable, indicates that the class is as above, AND that this instance is constructed within the function.
 * This means that within a function that is the sole owner of this data, its state-mutating functions can be called without violating function purity.
 */ 
@Target(LOCAL_VARIABLE, VALUE_PARAMETER) public annotation class LocalState
