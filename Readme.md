## What is this?

A Kotlin Compiler Plugin for determining and enforcing Pure and Readonly functions. Under construction!

## Usage

Install the plugin by adding the following to your `build.gradle.kts`:

```kotlin
plugins {
    id("io.github.yairm210.purity-plugin") version "0.0.24"
}

dependencies {
  compileOnly("io.github.yairm210:purity-annotations:0.0.24")
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


### Immutable 



### Optional configuration

#### Marking external classes

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
