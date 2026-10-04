## Advanced Usage


### Marking variables as Immutable

Many helpful functions, especially on collections, are readonly - because the function itself is the same regardless of the list it's iterating.

These functions are however deterministic - so when run on an immutable value, they will always return the same result, i.e. are Pure.

You can mark vals as Immutable to allow recognizing readonly functions run on them, as pure:

```kotlin
@Immutable
val immutableMap = mapOf(1 to 2, 3 to 4)

@Pure
fun pureFunction(int: Int): Int {
    return immutableMap[int] ?: 0
}
```

### Local State variables

Function purity is determined by its outer boundary - given the same call, return the same result. How we generate that result is up to us

One way many functions work is by building up a *mutable* object - a list, a map, etc - and returning it.

Common classes we can recognize as "holding internal state" (classes/functions marked `@InternalStateMutation`, or well-known ones - most collection classes like ArrayList/HashMap qualify, since their functions only ever mutate the receiver itself), and thus new instances can be recognized as "only available within the function".
New classes can be added via `wellKnownInternalStateClasses` in the config.

### Restricting functions to their own internal state

Besides `@Pure` (reads nothing external, writes nothing external) and `@Readonly` (reads anything, writes nothing external), two more annotations restrict functions in terms of what mutable state they may read/write, relative to the instance (`this`) they belong to:

- `@InternalStateMutation`: can read ALL mutable state, but can write ONLY its own instance-internal mutable state.
- `@InternalStateAccess`: can read AND write ONLY its own instance-internal mutable state - strictly more restrictive than `@InternalStateMutation`.

```kotlin
@InternalStateMutation
class Counter {
    var count = 0
    fun increment() { count += 1 } // mutating its own field - fine
}

@InternalStateAccess
class StrictCounter {
    var count = 0
    fun increment() { count += 1 } // mutating its own field - fine
    fun read() = count // reading its own field - fine
}
```

Both annotations can be placed on a class (applying to every function in it) or on an individual function. Their well-known-FQN equivalents in the config are `wellKnownInternalStateMutationFunctions` / `wellKnownInternalStateAccessFunctions` for functions, and `wellKnownInternalStateClasses` for classes marked as `@InternalStateMutation` (most collection classes - ArrayList, HashMap, etc. - read an arbitrary external collection argument in functions like `addAll`, so they only qualify for `@InternalStateMutation`, not the stricter `@InternalStateAccess`) - see [Handling external libraries](configuration.md#handling-external-libraries).

The deprecated `@InternalState` class annotation has been renamed to `@InternalStateMutation`.

For non-constructors, we need to add the `@LocalState` attribute manually:

```kotlin
@Pure fun getArrayList(): ArrayList<String> = ArrayList<String>().apply {
    add("string")
}

@Pure
fun alterExternallyDeclaredInnerStateClass() {
  @LocalState val newArrayList = getArrayList()
  newArrayList.add("another string") // Anything is allowed on a LocalState variable
}
```

Unfortunately, since it's only functions on the val itself that are allowed, we cannot chain calls.
The best be can do is assign the value back to a variable and then use it.

Note that adding `@LocalState` is a promise by the developer, and is abusable. Consider the following abuse example:

```kotlin
val existingArrayList = ArrayList<String>()

@Pure
fun alterExternallyDeclaredInnerStateClass() {
  @LocalState // False, and leads to broken contract!
  val localArrayList = existingArrayList // val access is allowed
  localArrayList.add("string") // Anything is allowed on a LocalState variable
}
```

### Marking functions as returning a new instance

`@LocalState` marks a *variable* as safe to mutate. `@ReturnsNewInstance` marks a *function* itself as always returning a newly allocated instance - not aliased/shared with its inputs or any external state - so that callers don't need to re-annotate every call site with `@LocalState`.

This means that if the function's return type is `InternalState`, the resulting value is automatically recognized as `@LocalState`, just like a direct constructor call:

```kotlin
@ReturnsNewInstance
fun buildList(): ArrayList<String> {
    val result = ArrayList<String>()
    result.add("string")
    return result
}

@Pure
fun caller() {
    val list = buildList() // Automatically recognized as LocalState - no manual annotation needed
    list.add("another string")
}
```

A `@ReturnsNewInstance` function's return statements are checked: each one must return either a constructor call, a call to another `@ReturnsNewInstance` function, or a local `val` assigned from one of those - otherwise it's a compilation error, since returning e.g. an input parameter or external state would break the contract.

The equivalent of this for external functions is `wellKnownNewInstanceFunctions` in the config - see [Handling external libraries](configuration.md#handling-external-libraries).

### Mutating input parameters

Use `@Mutated` on a parameter to allow non-Readonly calls on it while keeping the function's `@Pure` or `@Readonly` annotation:

```kotlin
@Pure
fun sortAndTrim(@Mutated list: MutableList<Int>, maxSize: Int) {
    list.sort()
    if (list.size > maxSize) list.subList(maxSize, list.size).clear()
}
```

Callers may only pass parameters they are themselves permitted to mutate — i.e. local state values (as specified above) or their own `@Mutated` input parameters.

For extension functions, mark the receiver (`this`) as mutated with the `@receiver:` use-site target:

```kotlin
@Pure
fun @receiver:Mutated MutableList<Int>.sortAndTrim(maxSize: Int) {
    sort()
    if (size > maxSize) subList(maxSize, size).clear()
}
```

### Caching

Often, you want to cache the result of a function in a class property - this is technically a state-altering operation, but the function as called could still be pure.

You can mark these with @Cache to indicate that mutating functions, and setting, can be used on them.

These properties *must* be private to ensure they cannot be mutated by external code.

Note that for mutating caches (like maps), this does not guarantee thread safety, so use thread-safe data structures for multithreading.

```kotlin
@Cache private val cacheMap: MutableMap<Int, Int> = mutableMapOf()
fun cachedMutatingFunction(input: Int): Int {
    return cacheMap.getOrPut(input){ input * 2 }
}

@Cache private var value = 0
fun cachedSettingFunction(): Int {
    if (value == 0) value = 42 // "heavy processing function"
    return value
}
```

### Autorecognized functions

Some functions are simple enough that they don't even need to be marked.

- functions that return a const are recognized as Pure (`fun getNum() = 42`)
- functions that return a property of this class (`fun getThing() = innerThing`) are recognized as Readonly if var, and Pure if val

### Inheritance

Functions that override other functions, or implement interfaces, are automatically recognized as *at least as strict* as the overridden function.

```kotlin
interface AreaCalculator {
  @Pure
  fun area(): Int
}

class Square(val width: Int) : AreaCalculator {
  override fun area(): Int = width * width  // Checked as if the function is marked with @Pure

  @Pure
  fun otherFunction(): Int = area() // Can call area() since it is considered @Pure
}

```

### Functions as parameters

Trick question: What purity is .let{}? The answer is: It depends on the function passed to it. Since we wish to allow passing pure functions to it, we recognize it as Pure.

The same is true for all other functions that take a function as a parameter - we allow invoking passed functions, without their purity affecting the purity of the function that takes them.

The reasoning is thus: The function that *calls* this function, if marked as Pure, cannot contain non-Pure code; If readonly, cannot contain non-Readonly code. Thus the called function takes on - at the least - the purity of the caller.

However, there are situations where you want to enforce the purity of a passed function. You can do so by marking the parameter as @Pure or @Readonly:

```kotlin
@Readonly
fun invoker(@Readonly function: (String) -> Unit) {
    function("world") 
}

// We sent a non-readonly function, so this should fail
fun compilationErrorInvokeWithNonReadonly() {  
    invoker { i:String -> println("Hello, $i!") } // will fail compliation - non-Readonly function passed to a Readonly function parameter
}
```

### Suppressing a single line

`@Suppress("purity")` on a function suppresses all purity checks for that entire function.
When you only need to bypass a single statement, you can place `@Suppress("purity")` on a local variable declaration instead — only the initializer of that variable is skipped, leaving the rest of the function fully checked.

```kotlin
var external = 0

@Pure
fun myFunction(): Int {
    @Suppress("purity")
    val ignored = external      // this read is suppressed

    return 42                   // rest of the function is still checked normally
}
```

For void or side-effecting calls, wrap the call in a `run` block and assign the result:

```kotlin
@Pure
fun myFunction() {
    @Suppress("purity")
    val ignored: Unit = run { external += 1 }  // side-effect suppressed on this line
}
```

The suppression covers the entire initializer expression, including any nested calls within it:

```kotlin
@Pure
fun myFunction() {
    @Suppress("purity")
    val result = buildString {         // all impure calls inside this block are suppressed
        append(external.toString())
    }
}
```

> **Note:** Suppression only works on `val`/`var` declarations because those are the only statement-level constructs that carry annotations in Kotlin's IR. Annotating a standalone expression call (e.g. `@Suppress("purity") impureCall()`) has no effect.

### Calling subfunctions

Subfunctions - functions created within the function scope - may be called even without being explicitly marked - since their contents are checked as part of the larger function.

This allows us to use subfunctions that write to local variables, while retaining the purity of the overall function:

```kotlin
 @Pure 
    fun functionTested(): Int {
        var internal = 0
        fun subFunction() { internal += 1 }
        subFunction()
        return internal
    }
```
