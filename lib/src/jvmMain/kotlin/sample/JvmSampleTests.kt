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

fun testUnmarkedFunctionCanGetAndSetVariables() {
    var x = 5

    // Unmarked function can read and write to variables
    fun unmarkedGetX() = x
    fun unmarkedSetX(value: Int) {
        x = value
    }
}

enum class MyEnum {
    A, B
}

fun testWellKnownReadonlyFunctions(){
    val map = HashMap<String, String>()

    @Readonly
    fun correctReadonlyInterfaceOverrideUsage(): Boolean {
        return map.containsKey("key")
    }
    
    val enumMap = EnumMap<MyEnum, String>(MyEnum::class.java)

    @Readonly
    fun getUniques(uniqueType: MyEnum) = enumMap[uniqueType]
        ?.asSequence()
        ?: emptySequence()


    @Readonly
    fun sequenceChaining(): String {
        return sequenceOf("Hello, ", "World!")
            .joinToString(separator = "")
    }
}

fun testClassVariableRetrieval(){
    class A{
        var x = 5
        @Readonly
        fun getXReadonly() = x
        
        @Pure @TestExpectCompileError
        fun getXPure() = x
    }
    
    class B {
        val x = 5
        @Pure
        fun getXX() = x
    }
}

fun testFunctionVariableGetSet(){
    var functionVariable = 3

    // RIGHT: readonly, because reading external variables is allowed
    @Readonly
    fun readonlyGetVariable(): Int {
        return functionVariable
    }
    
    @Pure @TestExpectCompileError
    fun pureGetVariable(): Int {
        return functionVariable
    }
    
    @Readonly @TestExpectCompileError
    fun readonlySetVariable() {
        functionVariable = 4
    }

    @Pure @TestExpectCompileError
    fun pureSetVariable() {
        functionVariable = 4
    }
}

fun testFunctionsCanOnlyCallTheirPurityAndHigher() {
    fun unmarkedFunction(){}
    @Readonly fun readonlyFunction(){}
    @Pure fun pureFunction(){}
    
    @Readonly fun readonlyCorrect(){
        readonlyFunction()
        pureFunction() // Allowed, since pure is higher than readonly
    }
    
    @Readonly @TestExpectCompileError fun readonlyIncorrect(){
        unmarkedFunction() // NOT allowed, since unmarked is lower than readonly
    }
    
    @Pure fun pureCorrect(){
        pureFunction() // Allowed, since pure is equal to pure
    }
    
    @Pure @TestExpectCompileError fun pureIncorrectReadonly(){
        readonlyFunction() // NOT allowed, since readonly is lower than pure
    }

    @Pure @TestExpectCompileError fun pureIncorrectRUnmarked(){
        unmarkedFunction() // NOT allowed, since readonly is lower than pure
    }
}

fun testReadonlyFunctionsOnImmutableConsideredPure(){
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
}

fun testLocalStatesAlterable() {
    @Pure
    fun alterInnerStateClass() {
        @LocalState
        val existingArrayList = ArrayList<String>()
        existingArrayList.add("string") // Anything is allowed on a LocalState variable
    }
}

fun testAutorecognizeSingleReturnFunctionOnValAsPure(){
    // On function variables
    
    val map = mapOf(1 to 2, 3 to 4)
    fun returnMap() = map // Autorecognized as pure

    @Pure
    fun getReturnMap() = returnMap() // can call pure function

    // On class properties
    class SampleClass {
        val map = mapOf(1 to 2, 3 to 4)
        fun returnMap() = this.map

        @Pure
        fun getReturnMutableMap() = returnMap()
    }
}

fun testAutorecognizeSingleReturnFunctionOnVarAsReadonly(){
    class SampleClass {
        var mutableMap = mutableMapOf(1 to 2, 3 to 4)
        fun returnMutableMap() = mutableMap

        @Readonly
        fun getReturnMutableMap() = returnMutableMap()

        @Pure @TestExpectCompileError
        // returnMutableMap is recognized as readonly, not pure
        fun incorrectPureGetReturnMutableMap() = returnMutableMap() // This is not pure, because it returns a mutable map
    }
}

// Apparently interfaces cannot be defined within functions, who knew?
interface AreaCalculator {
    @Pure fun area(): Int
}

fun testMarkingInterfaceMarksImplementations() {
    // If an interface is marked as @Pure, all implementations are considered pure
    class Square(val width: Int) : AreaCalculator {
        override fun area(): Int = width * width  // Checked as if the function is marked with @Pure

        @Pure
        fun otherFunction(): Int = area() // Can call area() since it is considered @Pure
    }
}


fun testPassingReadonlyFunction(){
    @Readonly
    fun invoker(@Readonly function: (Int) -> Unit) {
        function(4) // Can invoke input params marked as @Readonly
    }

    // We sent a non-readonly function, so this should fail
    fun testFunctionNotReadonly() {
        invoker @TestExpectCompileError { i:Int -> println("Hello, World!") }
    }

    fun testFunctionReadonly(){
        invoker { 1 + 1 }
    }
}

enum class Order{
    First, Second, Third
}
fun testEnumComparisonIsPure() {
    @Pure fun isHigherThan(orderA: Order, orderB: Order) = orderA > orderB
}
