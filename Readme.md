## What is this?

A Kotlin Compiler Plugin for determining and enforcing Pure and Readonly functions. Under construction!

## Usage

Install the plugin by adding the following to your `build.gradle.kts`:

```kotlin
plugins {
    id("io.github.yairm210.purity-plugin") version "0.0.27"
}

dependencies {
  compileOnly("io.github.yairm210:purity-annotations:0.0.27")
}
```


Mark pure functions using `@Pure`, and readonly functions using `@Readonly`:

```kotlin
import yairm210.purity.annotations.Pure
import yairm210.purity.annotations.Readonly
@Pure
fun pureFunction(x: Int): Int {
    return x * 2
}
@Readonly
fun readonlyFunction(list: List<String>): Int {
    return list.size
}
```


### Rules

- Pure functions may not:
  - Get or set external vars (vars created outside the function)
  - Call other non-pure functions

- Readonly functions may not:
  - Set external vars
  - Call other non-readonly functions (pure functions are considered readonly as well)

Any violation of these rules creates a compilation error.


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

### Marking variables as LocalState

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

### Optional configuration

#### Handling external classes

To support idiomatic Kotlin, Purity recognizes pure and readonly functions of well-known classes.

You can expand this recognition in the following way:

```kotlin
import yairm210.purity.PurityConfiguration // at the top of your build.gradle.kts

configure<PurityConfiguration> { // All of these are examples that are already contained in the known functions/classes 
  wellKnownPureClasses = setOf("kotlin.ranges.IntRange")
  wellKnownPureFunctions = setOf("kotlin.collections.listOf")
  wellKnownReadonlyFunctions = setOf("java.util.EnumMap.get")
}
```

#### Warn where annotations can be added 

By default, Purity only checks that functions are at least as strict as declared.
Once most of the code has been annotated, you may wish to see what other functions are strict enough to accept annotations,
  to 'lock them in' to their current purity level.

```kotlin
import yairm210.purity.PurityConfiguration // at the top of your build.gradle.kts

configure<PurityConfiguration> { 
  warnOnPossibleAnnotations = true
}
```

### Suppressing validity checks

Every dependency tree has leaves at the bottom. 
Often, you want to mark your lowest functions as pure or readonly, and have the compiler only check the from it and up.
You can do this my marking the function with `@Suppress("purity")`, like so:

```kotlin
var external = 3
// reads an external variable, but will not throw an error
@Pure @Suppress("purity")
fun actsAsPure(): Int {
    return external
}
```

## Acknowledgments

Projects that helped me understand how to setup the project:
* [Foso/KotlinCompilerPluginExample](https://github.com/Foso/KotlinCompilerPluginExample)

## TODO

- Handle function overrides - ensure that the overriding function is at least as strict as the overridden one 
- Handle function calls in lambdas - ensure that the lambda is at least as strict as the function it is passed to
- Allow mutating changes on function-local parameters (@Local annotation?)
  - Ensure that the expression for the local is pure?
