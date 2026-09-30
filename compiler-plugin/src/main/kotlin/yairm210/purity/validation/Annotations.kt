package yairm210.purity.validation

import org.jetbrains.kotlin.name.FqName

object Annotations {
    val Pure = FqName("yairm210.purity.annotations.Pure")
    val Readonly = FqName("yairm210.purity.annotations.Readonly")
    val LocalState = FqName("yairm210.purity.annotations.LocalState")
    val InternalState = FqName("yairm210.purity.annotations.InternalState") // Deprecated - see ModifiesInternalStateOnly
    val ModifiesInternalStateOnly = FqName("yairm210.purity.annotations.ModifiesInternalStateOnly")
    val Cache = FqName("yairm210.purity.annotations.Cache")
    val Immutable = FqName("yairm210.purity.annotations.Immutable")
    val Mutated = FqName("yairm210.purity.annotations.Mutated")
    val ReturnsNewInstance = FqName("yairm210.purity.annotations.ReturnsNewInstance")
    val TestExpectCompileError = FqName("yairm210.purity.annotations.TestExpectCompileError")

}