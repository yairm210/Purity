# Introduction

## What is this?

A Kotlin Compiler Plugin for determining and enforcing Pure and Readonly functions.

### Why?

- Communicating and enforcing function intent
- Determining parallelizable calls (Pure functions are parallelizable with anything; Readonly are parallelizable with each other)

## Installation + Basic Usage

Install the plugin by adding the following to your `build.gradle.kts`:

```kotlin
plugins {
    id("io.github.yairm210.purity-plugin") version "1.2.3"
}

dependencies {
  implementation("io.github.yairm210:purity-annotations:1.2.3")
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


### Suppressing validity checks

Every dependency tree has leaves at the bottom.

In order to gradually build up the functions that are checked by the compiler, you may want to work top-down, marking functions as you go.

You can stop the process from here on by marking a function with `@Suppress("purity")`, like so:

```kotlin
var external = 3
// reads an external variable, but will not throw a compilation error
@Pure @Suppress("purity")
fun actsAsPure(): Int {
    return external
}
```

### Limitations

- All getters are assumed to not be state-changing
- All constructors are assumed to be pure - to change state only of the instance being created
