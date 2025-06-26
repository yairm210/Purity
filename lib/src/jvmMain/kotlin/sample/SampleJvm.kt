package sample

import org.jetbrains.annotations.Contract

actual class Sample {
    actual fun checkMe() = 42
}

actual object Platform {
    actual val name: String = "JVM"
}

enum class MyEnum {
    A, B
}

@Contract(pure = true)
fun localStateNonChaining(): String {
    val stringBuilder = StringBuilder()
    stringBuilder.append("Hello, ")
    stringBuilder.append("World!")
    return stringBuilder.toString()
}

@Contract("readonly")
fun sequenceChaining(): String {
    return sequenceOf("Hello, ", "World!")
        .joinToString(separator = "")
}

//
//data class MyDataClass(val a: Int, val b: String)
//
//interface MyInterface {
//    fun doSomething(): String
//}

//
//fun main() {
//    var external = 3
//
//    
//    fun incorrectPureReturnsExternal() = external
//
//    // RIGHT: readonly, because reading external variables is allowed
//    @Contract("readonly")
//    fun correctReadonly(): Int {
//        1.rangeTo(2)
//        val x = listOf(1,2)
//        x[0]
//        return external
//    }
//    
//    // WRONG: not pure, because it reads an external variable
//    @Contract(pure = true)
//    fun incorrectPure(): Int {
//        return external
//    }
////
//    @Contract(pure = true)
//    fun give(a: Int): Int {
//        return a
//    }

//    @Contract(pure = true)
//    fun untrustable(a: Int, b: Int): Int {
//        external = 4
//        return a
//    }
    
//    @Contract(pure = true)
//    fun getList() = listOf(1,2)

//    fun Int.self(): Int {
//        return this
//    }
//    @Contract(pure = true)
//    fun add(a: Int, b: Int): Int {
//        external = 4
////        untrustable(5, 6)
////        getList()
//        return a.self() + give(b) + 5 //untrustable(5,6)
//    }
//    
//    // NOT reported as a problem since the variable is internal
//    @Contract(pure = true)
//    fun setsInternalVariable(): Int {
//        var internal = 3
//        internal = 4
//        return internal
//    }
//    
//    @Contract("readonly")
//    fun unmarkedFunction(a: Int): Int {
//        return a * a
//    }
//    
//    println(add(1,2))
//}