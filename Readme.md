## What is this?

A Kotlin Compiler Plugin for determining and enforcing Pure and Readonly functions.

### Why?

- Communicating and enforcing function intent
- Determining parallelizable calls (Pure functions are parallelizable with anything; Readonly are parallelizable with each other)

## Installation + Basic Usage

Install the plugin by adding the following to your `build.gradle.kts`:

```kotlin
plugins {
    id("io.github.yairm210.purity-plugin") version "0.0.39"
}

dependencies {
  compileOnly("io.github.yairm210:purity-annotations:0.0.39")
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

### Advanced usage

Further details are available in the [documentation site](https://yairm210.github.io/Purity/)

### Rules

- Pure functions may not:
  - Get or set external vars (vars created outside the function)
  - Call other non-pure functions

- Readonly functions may not:
  - Set external vars
  - Call other non-readonly functions (pure functions are considered readonly as well)

Any violation of these rules creates a compilation error.

## Acknowledgments

Projects that helped me understand how to setup the project:
* [Foso/KotlinCompilerPluginExample](https://github.com/Foso/KotlinCompilerPluginExample)
