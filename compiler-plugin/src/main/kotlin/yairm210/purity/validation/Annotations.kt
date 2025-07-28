package yairm210.purity.validation

import org.jetbrains.kotlin.name.FqName

object Annotations {
    val Pure = FqName("yairm210.purity.annotations.Pure")
    val Readonly = FqName("yairm210.purity.annotations.Readonly")
    val LocalState = FqName("yairm210.purity.annotations.LocalState")
    val Cache = FqName("yairm210.purity.annotations.Cache")
    val Immutable = FqName("yairm210.purity.annotations.Immutable")
    val TestExpectCompileError = FqName("yairm210.purity.annotations.TestExpectCompileError")
    
}