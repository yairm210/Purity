package yairm210.purity.validation

import org.jetbrains.kotlin.name.FqName

object Annotations {
    val Pure = FqName("yairm210.purity.annotations.Pure")
    val Readonly = FqName("yairm210.purity.annotations.Readonly")
    val LocalState = FqName("yairm210.purity.annotations.LocalState")
    val InternalState = FqName("yairm210.purity.annotations.InternalState") // Deprecated - see InternalStateAccess
    val InternalStateMutation = FqName("yairm210.purity.annotations.InternalStateMutation")
    val InternalStateAccess = FqName("yairm210.purity.annotations.InternalStateAccess")
    val Cache = FqName("yairm210.purity.annotations.Cache")
    val Immutable = FqName("yairm210.purity.annotations.Immutable")
    val Mutated = FqName("yairm210.purity.annotations.Mutated")
    val ReturnsNewInstance = FqName("yairm210.purity.annotations.ReturnsNewInstance")
    val TestExpectCompileError = FqName("yairm210.purity.annotations.TestExpectCompileError")

}