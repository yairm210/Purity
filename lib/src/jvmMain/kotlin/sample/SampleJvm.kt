package sample

import yairm210.purity.annotations.Pure
import yairm210.purity.annotations.Readonly
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

// Disabled due to implementation problems in callerIsConstructedInOurFunction, see there 
//@Pure
//fun correctLocalStateNonChainingPure(): String {
//    val stringBuilder = StringBuilder()
//    stringBuilder.append("Hello, ")
//    stringBuilder.append("World!")
//    return stringBuilder.toString()
//}

val map = HashMap<String, String>()

@Readonly
fun correctReadonlyInterfaceOverrideUsage(): Boolean {
    return map.containsKey("key")
}

//
//@Readonly
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

//
//data class MyDataClass(val a: Int, val b: String)
//
//interface MyInterface {
//    fun doSomething(): String
//}

//
fun main() {
    var external = 3


    fun incorrectPureReturnsExternal() = external

    // RIGHT: readonly, because reading external variables is allowed
    @Readonly
    fun correctReadonly(): Int {
        1.rangeTo(2)
        val x = listOf(1,2)
        x[0]
        return external
    }

    // WRONG: not pure, because it reads an external variable
    @Pure @Suppress("purity")
    fun incorrectPure(): Int {
        return external
    }
    
    @Pure
    fun give(a: Int): Int {
        return a
    }

    @Pure @Suppress("purity")
    fun untrustable(a: Int, b: Int): Int {
        external = 4
        return a
    }

    @Pure
    fun getList() = listOf(1,2)

    fun Int.self(): Int {
        return this
    }
    
    @Pure @Suppress("purity")
    fun add(a: Int, b: Int): Int {
        external = 4
        untrustable(5, 6)
        getList()
        return a.self() + give(b) + 5 //untrustable(5,6)
    }

    // NOT reported as a problem since the variable is internal
    @Pure
    fun setsInternalVariable(): Int {
        var internal = 3
        internal = 4
        return internal
    }

    @Readonly
    fun unmarkedFunction(a: Int): Int {
        return a * a
    }


    val arrayList = ArrayList<String>()
    // This should NOT be considered for "modify internal state" checks - we didn't construct the ArrayList!
    //@Pure
    fun alterExternallyDeclaredInnerStateClass() {
        val existingArrayList = arrayList
        existingArrayList.remove("string")
    }
}