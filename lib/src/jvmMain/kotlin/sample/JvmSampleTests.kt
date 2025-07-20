package sample

import yairm210.purity.annotations.*
import java.util.*
import kotlin.collections.HashMap

actual class Sample {
    actual fun checkMe() = 42
}

actual object Platform {
    actual val name: String = "JVM"
}

enum class MyEnum {
    A, B
}

// 

val map = HashMap<String, String>()

@Readonly
fun correctReadonlyInterfaceOverrideUsage(): Boolean {
    return map.containsKey("key")
}

//
@Readonly
fun correctSequenceChainingPure(): String {
    return sequenceOf("Hello, ", "World!")
        .joinToString(separator = "")
}

class A{
    var x = 5
    @Readonly
    fun getXX() = x
}


class B{
    val x = 5
    @Pure
    fun getXX() = x
}

var enumMap = EnumMap<MyEnum, String>(MyEnum::class.java)

@Readonly
fun getUniques(uniqueType: MyEnum) = enumMap[uniqueType]
    ?.asSequence()
    ?: emptySequence()

var myList = listOf("Hello", "World")
@Readonly
fun correctListChainingPure(): String {
    return myList
        .filter { it.isNotEmpty() }
        .joinToString(separator = " ")
}


fun main() {
    var external = 3
    
    // RIGHT: readonly, because reading external variables is allowed
    @Readonly
    fun correctReadonly(): Int {
        1.rangeTo(2)
        val x = listOf(1,2)
        x[0]
        return external
    }
    
    @Pure
    fun give(a: Int): Int {
        return a
    }
    
    @Pure @TestExpectCompileError
    fun readsExternalVar(): Int {
        return external
    }

    @Pure @TestExpectCompileError
    fun setsExternalVar(a: Int, b: Int): Int {
        external = 4
        return a
    }
    
    @Pure @TestExpectCompileError
    fun callsReadonly(){
        correctReadonly() // Pure functions cannot call readonly functions
    }

    @Pure
    fun getList() = listOf(1,2)
    
    @Readonly
    fun callPureFunction() { // Readonly functions can call pure functions
        getList().filter { it > 1 }
    }

    fun Int.self(): Int {
        return this
    }

    // NOT reported as a problem since the variable is internal
    @Pure
    fun setsInternalVariable(): Int {
        var internal = 3
        internal = 4
        val seq = sequence<String> {
            // Subscope can alter its vars and function vars
            var internal2 = 0
            yield("Hello")
            yield("World")
            internal2 += 1
            internal += 1
        }
        return internal
    }

    @Immutable
    val immutableList = arrayListOf(1,2,3)
    
    @Pure
    fun readImmutable(index: Int): Int {
        immutableList.filter { it > 2 } // allowed as 'extensionReceiver is @Immutable'
        return immutableList[index] // allowed as 'dispatchReceiver is @Immutable'
    }
    
    @Pure @TestExpectCompileError
    fun writeImmutable(){ // @Immutable only allows readonly functions to be considered pure, not write functions
        immutableList.add(4) // This is not allowed, so this function is not pure
    }
     
    @Pure
    fun alterInnerStateClass() {
        @LocalState
        val existingArrayList = ArrayList<String>()
        existingArrayList.remove("string") // Anything is allowed on a LocalState variable
    }
}

class SampleClass {
    @Immutable
    val immutableMap = mapOf(1 to 2, 3 to 4)
    @Pure
    fun pureFunction(int: Int): Int {
        return immutableMap[int] ?: 0
    }
    
    // Autorecognized as pure
    fun returnMap() = immutableMap
    
    @Pure
    fun getReturnMap() = returnMap()
    
    var mutableMap = mutableMapOf(1 to 2, 3 to 4)
    fun returnMutableMap() = mutableMap
    
    @Readonly
    fun getReturnMutableMap() = returnMutableMap()
    
    @Pure @TestExpectCompileError
    // returnMutableMap is recognized as readonly, not pure
    fun incorrectPureGetReturnMutableMap() = returnMutableMap() // This is not pure, because it returns a mutable map
}


interface AreaCalculator {
    @Pure
    fun area(): Int
}

class Square(val width: Int) : AreaCalculator {
    // Should be defined as pure since it overrides a pure function
    override fun area(): Int {
        return width * width
    }

    @Pure
    fun otherFunction(): Int {
        return area()
    }
}