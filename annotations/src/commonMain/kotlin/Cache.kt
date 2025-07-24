package yairm210.purity.annotations

import kotlin.annotation.AnnotationTarget.PROPERTY

/**
 * Indicates that a private property is a cache for a function, and therefore can be both read and written to.
 * Note that this does NOT ensure thread safety, so choose thread-safe data structures multithreading.
 */ 
@Target(PROPERTY) public annotation class Cache
