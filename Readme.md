#  <img src="docs/img/Purity.svg" width="24"> Purity

![Gradle Plugin Portal Version](https://img.shields.io/gradle-plugin-portal/v/io.github.yairm210.purity-plugin)
[![Build status](https://github.com/yairm210/Purity/actions/workflows/gradle.yml/badge.svg)](https://github.com/yairm210/Purity/actions/workflows/gradle.yml)

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

### Advanced usage

Further details are available in the [documentation](https://yairm210.github.io/Purity/usage/advanced-usage/)

### Rules

- Pure functions may not:
  - Get or set external vars (vars created outside the function)
  - Call other non-pure functions

- Readonly functions may not:
  - Set external vars
  - Call other non-readonly functions (pure functions are considered readonly as well)

Any violation of these rules creates a compilation error.

### Limitations

- All getters are assumed to not be state-changing
- All constructors are assumed to be pure - to change state only of the instance being created

## Development

Development instructions [here](https://yairm210.github.io/Purity/development/development/)

## Acknowledgments

Projects that helped me understand how to setup the project:
* [Foso/KotlinCompilerPluginExample](https://github.com/Foso/KotlinCompilerPluginExample)
* [bnorm/kotlin-power-assert](https://github.com/bnorm/kotlin-power-assert)
