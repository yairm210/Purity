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
    fun unmarkedGetX() = x
    fun unmarkedSetX(value: Int) { x = value }
}

enum class MyEnum { A, B }

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

    @Readonly // RIGHT: readonly, because reading external variables is allowed
    fun readonlyGetVariable(): Int { return functionVariable }

    @Pure @TestExpectCompileError
    fun pureGetVariable(): Int { return functionVariable }

    @Readonly @TestExpectCompileError
    fun readonlySetVariable() { functionVariable = 4 }

    @Pure @TestExpectCompileError
    fun pureSetVariable() { functionVariable = 4 }
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

fun testReadonlyFunctionsOnImmutableFunctionVariableConsideredPure(){
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

fun testReadonlyFunctionsOnImmutableClassPropertyConsideredPure(){
    class A{
        @Immutable val immutableList = arrayListOf(1,2,3)

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
}

fun testLocalStatesAlterable() {
    @Readonly fun getArrayList() = ArrayList<String>()
    @Readonly
    fun alterInnerStateClass() {
        @LocalState
        val existingArrayList = getArrayList()
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

interface ListFactory {
    @ReturnsNewInstance fun makeList(): ArrayList<Int>
}

fun testMarkingInterfaceMarksImplementations() {
    // If an interface is marked as @Pure, all implementations are considered pure
    class Square(val width: Int) : AreaCalculator {
        override fun area(): Int = width * width  // Checked as if the function is marked with @Pure

        @Pure
        fun otherFunction(): Int = area() // Can call area() since it is considered @Pure
    }
}


@TestExpectCompileError // This is a hack - since internal notPassablePasser fails on passing function references, this will fail too
// I couldn't find a way to check IrCall parameter passing *only* at the bottom-most function, which annoys me -_-
fun testPassingReadonlyFunction() {
    @Readonly
    fun invoker(@Readonly function: (Int) -> Unit) {
        function(4) // Can invoke input params marked as @Readonly
    }

    // Can even invoke non-Readonly function - see documentation
    @Readonly
    fun invokerError(function: (Int) -> Unit) {
        function(4) 
    }

    // We sent a non-readonly function, so this should fail
    fun testFunctionNotReadonly() {
        invoker @TestExpectCompileError { i: Int -> println("Hello, World!") }
    }

    fun testFunctionReadonly() = invoker { 1 + 1 }

    fun testPassThroughFunction() {
        fun passThroughReadonly(@Readonly function: (Int) -> Unit) =
            invoker(function) // allowed, since function is marked as @Readonly as well

        fun pureReceiver(@Pure function: (Int) -> Unit) {}

        fun passThroughPure(@Pure function: (Int) -> Unit) =
            pureReceiver(function) // allowed, since function is marked as @Readonly as well

        fun passThroughPureToReadonly(@Pure function: (Int) -> Unit) = invoker(function)
    }

    class ClassWithReadonlyFunction {
        @Readonly
        fun passable(i: Int) {}

        fun passer() = invoker(::passable)

        fun notPassable(i: Int) {}

        @TestExpectCompileError
        fun notPassablePasser() {
            invoker(::notPassable) // This should fail, since notPassable is not marked as @Readonly
        }
    }

    var f = 5
    fun testUnacceptableDefaultValue(
        @Readonly function: (Int) -> Unit = @TestExpectCompileError { f = 6 } // Default function is marked as @Readonly
    ) {}
    fun testAcceptableDefaultValue(
        @Readonly function: (Int) -> Unit = { println() } // Default function is marked as @Readonly
    ) {}

    fun testCallingUsingDefault(){
        testAcceptableDefaultValue()
    }
}

enum class Order{
    First, Second, Third
}
fun testEnumComparisonIsPure() {
    @Pure fun isHigherThan(orderA: Order, orderB: Order) = orderA > orderB
}

fun testDataClassDestructuringConsideredPureOrReadonly() {
    @Pure
    fun splitDataClass() {
        val pair = "example" to 42
        // destructuring declarations of immmutable data classes are considered pure
        val (str, num) = pair 
    }

    data class MutablePair(var first: String, var second: Int)
    @Readonly
    fun splitMutableDataClassReadonly() {
        val pair = MutablePair("example", 42)
        val (str, num) = pair
    }

    @Pure @TestExpectCompileError
    fun splitMutableDataClassPure() {
        val pair = MutablePair("example", 42)
        val (str, num) = pair // destructuring declarations are considered pure
    }
}

fun testDestructureHashmapEntries(){
    @Readonly
    fun splitHashmapEntries() {
        val hashmap = hashMapOf("key1" to "value1", "key2" to "value2")
        for ((key, value) in hashmap) {
            var result = "$key: $value"
        }
    }
}

fun testCache(){
    class withCache {
        @Cache private val cacheMap: MutableMap<Int, Int> = mutableMapOf()
        @Readonly
        fun cachedMutatingFunction(input: Int): Int {
            return cacheMap.getOrPut(input){input * 2}
        }

        @Cache private var value = 0
        @Readonly
        fun cachedSettingFunction(input: Int): Int {
            if (value == 0) value = 42 // "heavy processing function"
            return value
        }
    }
}

fun testWellKnownPureClassesConsideredImmutable() {
    @Pure
    fun useWellKnownPureClasses() {
        val string = "Hello, World!"
        var sumOfChars = 0
        string.indices.forEach { sumOfChars += string[it].code }
    }
}

fun testFunctionsCanSafelyCallSubfunctions(){
    // Function purity checks include all subfunction code - so the only difference is if they write to function-local variables or not
    @Pure 
    fun functionTested(): Int {
        var internal = 0
        fun subFunction(){
            internal += 1
        }
        subFunction()
        return internal
    }
}

fun testVarargsConsideredImmutable(){
    @Pure
    fun varargsFunction(vararg numbers: Int): Int {
        return numbers.sum() // varargs are considered immutable, so this is pure
    }
}

fun testNestedFunctionsCanBeCalled(){
    @Readonly
    fun readonlySequence() = sequence { 
        var i = 0
        fun changeLocalVariable(){
            i = 1
        }
        changeLocalVariable()
        yield(1)
    }
}

fun testLocalStateRecognizedAutomaticallyForKnownClasses(){
    @Pure
    fun alterInnerStateClass() {
        val existingArrayList = ArrayList<String>()
        existingArrayList.add("string") // Anything is allowed on a LocalState variable
    }


    @Pure
    fun alterStateOfNewInstance() {
        val existingArrayList = hashSetOf("hi")
        existingArrayList.add("string") // Anything is allowed on a LocalState variable - here detected since hashSetOf returns a new instance
    }
}

fun testTrustedDefaultSetterOnOwnedInstanceOfPlainClass() {
    // PlainData is not a well-known internal-state class, nor @ModifiesInternalStateOnly - but its
    // setter for `value` is a compiler-generated default setter (just `field = value`), so it's trusted
    // on an owned (freshly-constructed) local instance even though the class itself offers no guarantee
    class PlainData {
        var value = 0
    }

    @Pure
    fun assignOnOwnedPlainInstance() {
        val data = PlainData()
        data.value = 5 // fine - default setter on an owned instance
    }

    open class OpenData {
        open var value = 0
    }
    class OverriddenData : OpenData() {
        var sideEffectCount = 0
        override var value: Int
            get() = super.value
            set(newValue) { sideEffectCount += 1; super.value = newValue } // has a real side effect
    }

    @Pure @TestExpectCompileError
    fun assignOnOwnedOpenInstance() {
        val data: OpenData = OverriddenData() // static type OpenData, but could be any subclass
        data.value = 5 // NOT allowed - open setter could resolve to an override with side effects
    }
}

fun testInheritingFunctionsInheritSuppression(){
    var external = 0 
    open class A{
        @Pure @Suppress("purity") open fun suppressed() {}
    }

    class B : A() {
        override fun suppressed() { external += 1 }
    }
}

fun testPerLineSuppression() {
    var external = 0

    @Pure
    fun suppressedCallInPureFunction() {
        @Suppress("purity")
        val ignored = external // reading external var suppressed on this line
    }

    @Pure
    fun suppressedSetInPureFunction() {
        @Suppress("purity")
        val ignored: Unit = run { external += 1 } // setting external var suppressed on this line
    }

    @Pure
    fun suppressionOnOneLineDoesNotAffectOthers() {
        @Suppress("purity")
        val ignored = external // suppressed
        // The function is still @Pure overall — no further violations below
    }

    @Pure @TestExpectCompileError
    fun suppressionDoesNotCoverOtherLines() {
        @Suppress("purity")
        val ignored = external // this line suppressed
        val stillChecked = external // this line is NOT suppressed — should still error
    }
}

fun testForLocalStateAnnotation() {
    @Readonly
    fun localStateFunction() {
        // This is recognized as local state
        val localListOfLists = mutableListOf(mutableListOf<String>(), mutableListOf())
        // This is not 
        for (@LocalState item in localListOfLists) {
            item.add("hi")
        }
    }
}

fun testInternalClassesConsideredLocalState(){
    class NotDeclaredInternal{
        var x = 2
        fun doSomething() { x = 4 }
    }
    @Readonly @TestExpectCompileError
    fun notInternalUser(){
        val notInternal = NotDeclaredInternal()
        notInternal.doSomething()
    }

    @InternalState
    class DeclaredInternal{
        var x = 2
        fun doSomething() { x = 4 }
    }
    @Readonly
    fun internalUser(){
        val notInternal = DeclaredInternal()
        notInternal.doSomething()
    }
}


fun testInternalStateMethodsCanOnlyMutateOwnState() {
    var externalVar = 0
    fun externalMutatingFunction() { externalVar += 1 }

    @ModifiesInternalStateOnly
    class Inner {
        var y = 0
        fun assignY(value: Int) { y = value }
    }

    @ModifiesInternalStateOnly
    class Good {
        var x = 0
        val inner = Inner()
        fun assignX(value: Int) { x = value } // mutating own field - fine

        @Readonly
        fun readX() = x

        fun setXFromReadonlyCall() { x = readX() + 1 } // calling own Readonly method - fine

        fun setXViaLocal() {
            var local = 5
            local += 1 // mutating a local - fine
            x = local
        }

        fun clone(): Good {
            val new = Good() // newly constructed, unaliased - safe to mutate
            new.x = this.x
            new.assignX(this.x + 1)
            return new
        }

        // Calling a mutating method on a field whose own class is also @ModifiesInternalStateOnly - fine,
        // since that class itself guarantees it only mutates state that it owns
        fun mutateInnerField() {
            inner.assignY(5)
        }

        val innerList = mutableListOf<Inner>()
        // `+=` on a nullable @LocalState val's property evaluates the receiver once into a compiler
        // generated temp (to avoid re-evaluating the smart-cast/not-null check) - the temp var itself
        // isn't annotated, so ownership checks must see through it to the original @LocalState val
        fun bumpMatchingInner(target: Int) {
            @LocalState val match = innerList.firstOrNull { it.y == target }
            if (match != null) {
                match.y += 1
            }
        }

        // A local function (closure) is not itself a top-level member of the class - it's an
        // implementation detail of this member function, and is checked as part of its body traversal,
        // with full access to this function's own local vals - fine
        fun collectWaypoints(startTile: Int, tiles: List<Int>): List<Int> {
            val result = mutableListOf<Int>()
            fun addWaypoint(tile: Int) {
                if (tile != startTile && (result.isEmpty() || result.last() != tile)) result.add(tile)
            }
            tiles.forEach { addWaypoint(it) }
            return result
        }

        // Invoking a lambda that is itself marked @Readonly or @Pure is fine - both guarantee it won't
        // mutate anything outside itself
        fun invokeReadonlyLambda(@Readonly action: () -> Int): Int {
            return action()
        }

        // Invoking ANY lambda is allowed, unmarked or not - same as @Pure/@Readonly (which intentionally
        // do not check the lambda's own purity annotations at the invoke() call site)
        fun invokeUnmarkedLambda(action: () -> Int): Int {
            return action()
        }
    }

    class PlainHelper {
        var y = 0
        fun assignY(value: Int) { y = value }
    }

    @ModifiesInternalStateOnly
    class BadFieldOfPlainType {
        val helper = PlainHelper()
        @TestExpectCompileError
        fun mutateHelperField() {
            // PlainHelper is not @ModifiesInternalStateOnly (nor a well-known internal-state class),
            // so it offers no guarantee about what its methods might do - NOT allowed, even though
            // helper is this instance's own field
            helper.assignY(5)
        }
    }

    @ModifiesInternalStateOnly
    class BadWhitewashedField(externallyHeldInner: Inner) {
        // Field's class is @ModifiesInternalStateOnly, but the val is assigned from a constructor
        // parameter - the caller may still hold this same Inner reference, so it's NOT owned by this
        // instance, even though it looks like an "internal state field" at a glance
        val inner = externallyHeldInner
        @TestExpectCompileError
        fun mutateInnerField() {
            inner.assignY(5)
        }
    }

    // Interface delegation (`by map`) generates synthetic member functions (clear(), putAll(), etc.)
    // that just forward to the delegate - these are compiler-generated, not user-written, and must not
    // be validated as if the user wrote a call to an external mutating function inside their own body
    @ModifiesInternalStateOnly
    class DelegatingToMap(private val map: HashMap<Int, String> = hashMapOf()) : MutableMap<Int, String> by map

    @ModifiesInternalStateOnly
    class BadCall {
        var x = 0
        @TestExpectCompileError
        fun setXFromExternalCall() {
            externalMutatingFunction() // calling a non-Pure/Readonly/InternalState function - NOT allowed
            x = 1
        }
    }

    @ModifiesInternalStateOnly
    class BadSet {
        var x = 0
        @TestExpectCompileError
        fun setExternalVar() {
            externalVar = 1 // setting a var that isn't local or own field - NOT allowed
        }
    }

    @ModifiesInternalStateOnly
    class BadCrossInstanceSet {
        var x = 0
        @TestExpectCompileError
        fun setXFromOther(other: BadCrossInstanceSet) {
            other.x = 5 // mutating a DIFFERENT instance's state, even of the same class - NOT allowed
        }
    }

    @ModifiesInternalStateOnly
    class BadCrossInstanceCall {
        var x = 0
        fun assignX(value: Int) { x = value }
        @TestExpectCompileError
        fun setXFromOther(other: BadCrossInstanceCall) {
            other.assignX(5) // calling a non-Pure/Readonly method on a DIFFERENT instance - NOT allowed
        }
    }
}

fun testModifiesInternalStateOnlyOnFunctionDirectly() {
    // The annotation can now be placed on a specific function, without needing to mark the whole class
    class PartiallyRestricted {
        var x = 0
        var y = 0

        @ModifiesInternalStateOnly
        fun setXOnly(value: Int) { x = value } // fine - mutating own field

        // Not annotated - free to do whatever it wants, e.g. this would be fine even though
        // it wouldn't be allowed if this function were itself @ModifiesInternalStateOnly
        fun setYFreely(value: Int) { y = value }
    }

    var externalVar = 0

    class BadFunctionLevel {
        var x = 0
        @ModifiesInternalStateOnly @TestExpectCompileError
        fun setXFromExternal() {
            externalVar = 1 // NOT allowed - not local, not own field
        }
    }
}

fun testPlusEqualsSet(){
    @ModifiesInternalStateOnly class Internal(var a:Int)
    
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

fun testWellKnownNewInstanceFunctionsDetermineLocalState(){
    @Pure
    fun testIterableCreatesNew(){
        val list = listOf(1, 2, 3)
        val mutableList = list.toMutableList()
        mutableList.add(4)
    }
}

fun testReturnsNewInstanceAnnotationDeterminesLocalState(){
    @Readonly @ReturnsNewInstance
    fun makeNewList(): ArrayList<Int> = ArrayList()

    @Readonly
    fun testCustomFunctionCreatesNew(){
        val list = makeNewList()
        list.add(4)
    }
}

fun testReturnsNewInstanceValidation(){
    // Directly returning a constructor call is fine
    @ReturnsNewInstance
    fun makeListDirect(): ArrayList<Int> = ArrayList()

    // Returning a val assigned from a constructor call is fine
    @ReturnsNewInstance
    fun makeListViaVal(): ArrayList<Int> {
        val list = ArrayList<Int>()
        return list
    }

    // Returning a call to another @ReturnsNewInstance function is fine
    @ReturnsNewInstance
    fun makeListDelegated(): ArrayList<Int> = makeListDirect()

    // Returning an input parameter is NOT a new instance
    @ReturnsNewInstance @TestExpectCompileError
    fun returnsParameterNotNewInstance(list: ArrayList<Int>): ArrayList<Int> = list

    // emptyList/emptySet/emptyMap are well-known new-instance functions - even though they return a
    // shared singleton, their static type is read-only so no caller can mutate through it anyway
    @ReturnsNewInstance
    fun makeEmptyList(): List<Int> = emptyList()

    // Returning a call to an external stdlib function that is NOT well-known (no accessible body
    // either) - the error should suggest the FQN config route, since there's no body to check or annotate
    @ReturnsNewInstance @TestExpectCompileError
    fun returnsSharedJavaSingletonNotNewInstance(): List<Int> {
        if (true) return java.util.Collections.emptyList()
        return ArrayList()
    }

    // Returning a call to a local, unannotated function whose body genuinely satisfies the rule -
    // the error should suggest annotating that function directly, as the first/preferred option
    fun makeListDirectUnannotated(): ArrayList<Int> = ArrayList()

    @ReturnsNewInstance @TestExpectCompileError
    fun delegatesToUnannotatedFunction(): ArrayList<Int> = makeListDirectUnannotated()
}

fun testReturnsNewInstanceThroughSafeCall() {
    // Safe calls (a?.b()) desugar to a null-check wrapper around the real call - the LocalState
    // detection must see through that wrapper, or `startingUnits` never gets recognized as LocalState
    class Era {
        @Readonly @ReturnsNewInstance
        fun getStartingUnits(): ArrayList<String> = ArrayList()
    }

    val eras = mapOf("Ancient era" to Era())

    @Readonly
    fun testSafeCallLocalState(startingEra: String) {
        val startingUnits = eras[startingEra]?.getStartingUnits()
        if (startingUnits != null) {
            startingUnits.add("Warrior") // mutating call - allowed since startingUnits is LocalState
        }
    }
}

fun testReturnsNewInstanceInheritedFromOverride() {
    // Overriding a @ReturnsNewInstance function inherits the same obligation, even without re-annotating
    class ValidFactory : ListFactory {
        override fun makeList(): ArrayList<Int> = ArrayList()
    }

    class InvalidFactory(val shared: ArrayList<Int>) : ListFactory {
        @TestExpectCompileError
        override fun makeList(): ArrayList<Int> = shared // not a new instance - should fail
    }
}

//
//fun testCustomValGetterNotReadonly(){
//    class A {
//        var i: Int = 0
//        val j: Int
//            get() = ++i
//    }
//    
//    @Readonly @TestExpectCompileError
//    fun testCanPlusCustomValGetter(){
//        val bob = A()
//        print(bob.j)
//    }
//}

// --- @Mutated parameter tests ---

fun testMutatedBasic() {
    // @Pure/@Readonly functions may call non-Readonly functions on @Mutated parameters
    @Pure
    fun sortList(@Mutated list: MutableList<Int>) {
        list.sort()
        list.add(0)
    }

    // Other purity constraints still apply: @Pure cannot read external mutable state
    @Readonly
    fun readAndMutate(@Mutated list: MutableList<Int>): Int {
        val size = list.size  // Readonly — OK
        list.add(size)        // non-Readonly on @Mutated param — OK
        return list.sum()     // Pure — OK
    }
}

fun testMutatedPurityStillEnforced() {
    var externalVar = 0

    // @Pure + @Mutated: the @Pure constraint still applies; external state cannot be read
    @Pure @TestExpectCompileError
    fun pureCannotReadExternal(@Mutated list: MutableList<Int>) {
        list.add(externalVar)  // ERROR: @Pure cannot read external mutable var
    }

    // @Readonly + @Mutated: @Readonly may read external state — this is the key difference
    @Readonly
    fun readonlyCanReadExternal(@Mutated list: MutableList<Int>) {
        list.add(externalVar)  // OK: @Readonly is allowed to read external state
    }
}

fun testMutatedCallerSafety() {
    val externalList = mutableListOf("x")
    @Pure fun populate(@Mutated list: MutableList<String>, value: String) { list.add(value) }

    // Safe: LocalState arg
    @Pure
    fun pureWithSafeArg() {
        val localList = ArrayList<String>()
        populate(localList, "a")  // localList=LocalState, "a"=value type — OK
    }

    @Readonly
    fun readonlyWithSafeArg() {
        val localList = ArrayList<String>()
        populate(localList, "a")  // OK
    }

    // Unsafe: externalList is not LocalState or a value type
    @Pure @TestExpectCompileError
    fun pureWithUnsafeArg() {
        populate(externalList, "a")  // NOT OK: externalList would be mutated
    }

    @Readonly @TestExpectCompileError
    fun readonlyWithUnsafeArg() {
        populate(externalList, "a")  // NOT OK
    }
}

fun testMutatedChaining() {
    val externalList = mutableListOf("a")
    @Pure fun helper(@Mutated list: MutableList<String>) { list.add("x") }

    // Propagating a @Mutated param to another @Mutated param is OK since we have mutation rights
    @Pure
    fun validChain(@Mutated list: MutableList<String>) {
        helper(list)  // list is itself @Mutated — safe to pass on
    }

    // Passing external state to a @Mutated param is NOT OK
    @Pure @TestExpectCompileError
    fun invalidChain(@Mutated input: MutableList<String>) {
        helper(externalList)  // externalList is not @Mutated, LocalState, or a value type
    }
}