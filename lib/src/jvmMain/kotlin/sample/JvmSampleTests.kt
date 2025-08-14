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

//fun testUnmarkedFunctionCanGetAndSetVariables() {
//    var x = 5
//    fun unmarkedGetX() = x
//    fun unmarkedSetX(value: Int) { x = value }
//}
//
//enum class MyEnum { A, B }
//
//fun testWellKnownReadonlyFunctions(){
//    val map = HashMap<String, String>()
//
//    @Readonly
//    fun correctReadonlyInterfaceOverrideUsage(): Boolean {
//        return map.containsKey("key")
//    }
//    
//    val enumMap = EnumMap<MyEnum, String>(MyEnum::class.java)
//
//    @Readonly
//    fun getUniques(uniqueType: MyEnum) = enumMap[uniqueType]
//        ?.asSequence()
//        ?: emptySequence()
//
//
//    @Readonly
//    fun sequenceChaining(): String {
//        return sequenceOf("Hello, ", "World!")
//            .joinToString(separator = "")
//    }
//}
//
//fun testClassVariableRetrieval(){
//    class A{
//        var x = 5
//        @Readonly
//        fun getXReadonly() = x
//        
//        @Pure @TestExpectCompileError
//        fun getXPure() = x
//    }
//    
//    class B {
//        val x = 5
//        @Pure
//        fun getXX() = x
//    }
//}
//
//fun testFunctionVariableGetSet(){
//    var functionVariable = 3
//    
//    @Readonly // RIGHT: readonly, because reading external variables is allowed
//    fun readonlyGetVariable(): Int { return functionVariable }
//    
//    @Pure @TestExpectCompileError
//    fun pureGetVariable(): Int { return functionVariable }
//    
//    @Readonly @TestExpectCompileError
//    fun readonlySetVariable() { functionVariable = 4 }
//
//    @Pure @TestExpectCompileError
//    fun pureSetVariable() { functionVariable = 4 }
//}
//
//fun testFunctionsCanOnlyCallTheirPurityAndHigher() {
//    fun unmarkedFunction(){}
//    @Readonly fun readonlyFunction(){}
//    @Pure fun pureFunction(){}
//    
//    @Readonly fun readonlyCorrect(){
//        readonlyFunction()
//        pureFunction() // Allowed, since pure is higher than readonly
//    }
//    
//    @Readonly @TestExpectCompileError fun readonlyIncorrect(){
//        unmarkedFunction() // NOT allowed, since unmarked is lower than readonly
//    }
//    
//    @Pure fun pureCorrect(){
//        pureFunction() // Allowed, since pure is equal to pure
//    }
//    
//    @Pure @TestExpectCompileError fun pureIncorrectReadonly(){
//        readonlyFunction() // NOT allowed, since readonly is lower than pure
//    }
//
//    @Pure @TestExpectCompileError fun pureIncorrectRUnmarked(){
//        unmarkedFunction() // NOT allowed, since readonly is lower than pure
//    }
//}
//
//fun testReadonlyFunctionsOnImmutableFunctionVariableConsideredPure(){
//    @Immutable 
//    val immutableList = arrayListOf(1,2,3)
//
//    @Pure
//    fun readImmutable(index: Int): Int {
//        immutableList.filter { it > 2 } // allowed as 'extensionReceiver is @Immutable'
//        return immutableList[index] // allowed as 'dispatchReceiver is @Immutable'
//    }
//
//    @Pure @TestExpectCompileError
//    fun writeImmutable(){ // @Immutable only allows readonly functions to be considered pure, not write functions
//        immutableList.add(4) // This is not allowed, so this function is not pure
//    }
//}
//
//fun testReadonlyFunctionsOnImmutableClassPropertyConsideredPure(){
//    class A{
//        @Immutable val immutableList = arrayListOf(1,2,3)
//
//        @Pure
//        fun readImmutable(index: Int): Int {
//            immutableList.filter { it > 2 } // allowed as 'extensionReceiver is @Immutable'
//            return immutableList[index] // allowed as 'dispatchReceiver is @Immutable'
//        }
//
//        @Pure @TestExpectCompileError
//        fun writeImmutable(){ // @Immutable only allows readonly functions to be considered pure, not write functions
//            immutableList.add(4) // This is not allowed, so this function is not pure
//        }
//    }
//}
//
//fun testLocalStatesAlterable() {
//    @Readonly fun getArrayList() = ArrayList<String>()
//    @Readonly
//    fun alterInnerStateClass() {
//        @LocalState
//        val existingArrayList = getArrayList()
//        existingArrayList.add("string") // Anything is allowed on a LocalState variable
//    }
//}
//
//fun testAutorecognizeSingleReturnFunctionOnValAsPure(){
//    // On function variables
//    
//    val map = mapOf(1 to 2, 3 to 4)
//    fun returnMap() = map // Autorecognized as pure
//
//    @Pure
//    fun getReturnMap() = returnMap() // can call pure function
//
//    // On class properties
//    class SampleClass {
//        val map = mapOf(1 to 2, 3 to 4)
//        fun returnMap() = this.map
//
//        @Pure
//        fun getReturnMutableMap() = returnMap()
//    }
//}
//
//fun testAutorecognizeSingleReturnFunctionOnVarAsReadonly(){
//    class SampleClass {
//        var mutableMap = mutableMapOf(1 to 2, 3 to 4)
//        fun returnMutableMap() = mutableMap
//
//        @Readonly
//        fun getReturnMutableMap() = returnMutableMap()
//
//        @Pure @TestExpectCompileError
//        // returnMutableMap is recognized as readonly, not pure
//        fun incorrectPureGetReturnMutableMap() = returnMutableMap() // This is not pure, because it returns a mutable map
//    }
//}
//
//// Apparently interfaces cannot be defined within functions, who knew?
//interface AreaCalculator {
//    @Pure fun area(): Int
//}
//
//fun testMarkingInterfaceMarksImplementations() {
//    // If an interface is marked as @Pure, all implementations are considered pure
//    class Square(val width: Int) : AreaCalculator {
//        override fun area(): Int = width * width  // Checked as if the function is marked with @Pure
//
//        @Pure
//        fun otherFunction(): Int = area() // Can call area() since it is considered @Pure
//    }
//}
//
//
//@TestExpectCompileError // This is a hack - since internal notPassablePasser fails on passing function references, this will fail too
//// I couldn't find a way to check IrCall parameter passing *only* at the bottom-most function, which annoys me -_-
//fun testPassingReadonlyFunction() {
//    @Readonly
//    fun invoker(@Readonly function: (Int) -> Unit) {
//        function(4) // Can invoke input params marked as @Readonly
//    }
//
//    // Can even invoke non-Readonly function - see documentation
//    @Readonly
//    fun invokerError(function: (Int) -> Unit) {
//        function(4) 
//    }
//
//    // We sent a non-readonly function, so this should fail
//    fun testFunctionNotReadonly() {
//        invoker @TestExpectCompileError { i: Int -> println("Hello, World!") }
//    }
//
//    fun testFunctionReadonly() = invoker { 1 + 1 }
//
//    fun testPassThroughFunction() {
//        fun passThroughReadonly(@Readonly function: (Int) -> Unit) =
//            invoker(function) // allowed, since function is marked as @Readonly as well
//
//        fun pureReceiver(@Pure function: (Int) -> Unit) {}
//
//        fun passThroughPure(@Pure function: (Int) -> Unit) =
//            pureReceiver(function) // allowed, since function is marked as @Readonly as well
//
//        fun passThroughPureToReadonly(@Pure function: (Int) -> Unit) = invoker(function)
//    }
//
//    class ClassWithReadonlyFunction {
//        @Readonly
//        fun passable(i: Int) {}
//
//        fun passer() = invoker(::passable)
//
//        fun notPassable(i: Int) {}
//
//        @TestExpectCompileError
//        fun notPassablePasser() {
//            invoker(::notPassable) // This should fail, since notPassable is not marked as @Readonly
//        }
//    }
//    
//    var f = 5
//    fun testUnacceptableDefaultValue(
//        @Readonly function: (Int) -> Unit = @TestExpectCompileError { f = 6 } // Default function is marked as @Readonly
//    ) {}
//    fun testAcceptableDefaultValue(
//        @Readonly function: (Int) -> Unit = { println() } // Default function is marked as @Readonly
//    ) {}
//    
//    fun testCallingUsingDefault(){
//        testAcceptableDefaultValue()
//    }
//}
//
//enum class Order{
//    First, Second, Third
//}
//fun testEnumComparisonIsPure() {
//    @Pure fun isHigherThan(orderA: Order, orderB: Order) = orderA > orderB
//}
//
//fun testDataClassDestructuringConsideredPureOrReadonly() {
//    @Pure
//    fun splitDataClass() {
//        val pair = "example" to 42
//        // destructuring declarations of immmutable data classes are considered pure
//        val (str, num) = pair 
//    }
//    
//    data class MutablePair(var first: String, var second: Int)
//    @Readonly
//    fun splitMutableDataClassReadonly() {
//        val pair = MutablePair("example", 42)
//        val (str, num) = pair
//    }
//    
//    @Pure @TestExpectCompileError
//    fun splitMutableDataClassPure() {
//        val pair = MutablePair("example", 42)
//        val (str, num) = pair // destructuring declarations are considered pure
//    }
//}
//
//fun testDestructureHashmapEntries(){
//    @Readonly
//    fun splitHashmapEntries() {
//        val hashmap = hashMapOf("key1" to "value1", "key2" to "value2")
//        for ((key, value) in hashmap) {
//            var result = "$key: $value"
//        }
//    }
//}
//
//fun testCache(){
//    class withCache {
//        @Cache private val cacheMap: MutableMap<Int, Int> = mutableMapOf()
//        @Readonly
//        fun cachedMutatingFunction(input: Int): Int {
//            return cacheMap.getOrPut(input){input * 2}
//        }
//        
//        @Cache private var value = 0
//        @Readonly
//        fun cachedSettingFunction(input: Int): Int {
//            if (value == 0) value = 42 // "heavy processing function"
//            return value
//        }
//    }
//}
//
//fun testWellKnownPureClassesConsideredImmutable() {
//    @Pure
//    fun useWellKnownPureClasses() {
//        val string = "Hello, World!"
//        var sumOfChars = 0
//        string.indices.forEach { sumOfChars += string[it].code }
//    }
//}
//
//fun testFunctionsCanSafelyCallSubfunctions(){
//    // Function purity checks include all subfunction code - so the only difference is if they write to function-local variables or not
//    @Pure 
//    fun functionTested(): Int {
//        var internal = 0
//        fun subFunction(){
//            internal += 1
//        }
//        subFunction()
//        return internal
//    }
//}
//
//fun testVarargsConsideredImmutable(){
//    @Pure
//    fun varargsFunction(vararg numbers: Int): Int {
//        return numbers.sum() // varargs are considered immutable, so this is pure
//    }
//}
//
//fun testNestedFunctionsCanBeCalled(){
//    @Readonly
//    fun readonlySequence() = sequence { 
//        var i = 0
//        fun changeLocalVariable(){
//            i = 1
//        }
//        changeLocalVariable()
//        yield(1)
//    }
//}
//
//fun testLocalStateRecognizedAutomaticallyForKnownClasses(){
//    @Pure
//    fun alterInnerStateClass() {
//        val existingArrayList = ArrayList<String>()
//        existingArrayList.add("string") // Anything is allowed on a LocalState variable
//    }
//
//
//    @Pure
//    fun alterInnerStateClassFromPureFunction() {
//        val existingArrayList = hashSetOf("hi")
//        existingArrayList.add("string") // Anything is allowed on a LocalState variable
//    }
//}
//
//fun testInheritingFunctionsInheritSuppression(){
//    var external = 0 
//    open class A{
//        @Pure @Suppress("purity") open fun suppressed() {}
//    }
//    
//    class B : A() {
//        override fun suppressed() { external += 1 }
//    }
//}
//
//fun testForLocalStateAnnotation() {
//    @Readonly
//    fun localStateFunction() {
//        // This is recognized as local state
//        val localListOfLists = mutableListOf(mutableListOf<String>(), mutableListOf())
//        // This is not 
//        for (@LocalState item in localListOfLists) {
//            item.add("hi")
//        }
//    }
//}
//
//fun testInternalClassesConsideredLocalState(){
//    class NotDeclaredInternal{
//        var x = 2
//        fun doSomething() { x = 4 }
//    }
//    @Readonly @TestExpectCompileError
//    fun notInternalUser(){
//        val notInternal = NotDeclaredInternal()
//        notInternal.doSomething()
//    }
//
//    @InternalState
//    class DeclaredInternal{
//        var x = 2
//        fun doSomething() { x = 4 }
//    }
//    @Readonly
//    fun internalUser(){
//        val notInternal = DeclaredInternal()
//        notInternal.doSomething()
//    }
//}


fun testPlusEqualsSet(){
    @InternalState class Internal(var a:Int)
    
    @Pure
    fun testCanPlusSetInternal(){
        val bob = Internal(3)
        bob.a += 2
    }
    
    class Local(var a:Int)
    @Pure
    fun testCanPlusSetLocal(){
        @LocalState val bob = Local(3)
        bob.a += 2
    }
}

fun testCustomValGetterNotReadonly(){
    class A {
        var i: Int = 0
        val j: Int
            get() = ++i
    }
    
    @Readonly @TestExpectCompileError
    fun testCanPlusCustomValGetter(){
        val bob = A()
        print(bob.j)
    }
}