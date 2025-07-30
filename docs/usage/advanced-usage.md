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

Since these are by definition *mutating* functions, we need to mark the function variable as local:

```kotlin
@Pure
fun alterExternallyDeclaredInnerStateClass() {
  @LocalState
  val newArrayList = ArrayList<String>()
  newArrayList.add("string") // Anything is allowed on a LocalState variable
}
```

Unfortunately, since it's only functions on the val itself that are allowed, we cannot chain calls.
The best be can do is assign the value back to a variable and then use it.

Note that this is a promise, and is abusable, for the same reason it's not automatically determinable. Consider the following abuse example:

```kotlin
val existingArrayList = ArrayList<String>()

@Pure
fun alterExternallyDeclaredInnerStateClass() {
  @LocalState // False, and leads to broken contract!
  val localArrayList = existingArrayList // val access is allowed
  localArrayList.add("string") // Anything is allowed on a LocalState variable
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
