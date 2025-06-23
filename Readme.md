## What is this?

A Kotlin Compiler Plugin that determines Pure and Readonly functions. Under construction!

## Usage

Install the plugin by adding the following to your `build.gradle.kts`:

```kotlin
plugins {
    id("io.github.yairm210.purity-plugin") version "0.0.5"
}
```

Mark pure functions using `@Contract(pure = true)`, and readonly functions using `@Contract("readonly")`.

## Development

To test the plugin on /lib, run `./gradlew :lib:clean :lib:build` - the plugin is only active when the build cache is changed

To publish the compiler plugin locally:
- Update version in compiler plugin - `compiler-plugin/build.gradle.kts` 
- Update version in gradle plugin - `PurityGradlePlugin.kt`
- `./gradlew :compiler-plugin:publishToMavenLocal`

To publish the gradle plugin locally:
- Update version in `gradle-plugin/build.gradle.kts`
- `./gradlew :gradle-plugin:build`

To publish the compiler plugin to Maven Central, with the correct env variables in place, run `./gradlew compiler-plugin:publishAndReleaseToMavenCentral --no-configuration-cache`

To publish the gradle plugin to the Gradle Plugin Repository, with the correct env variables in place, run `./gradlew gradle-plugin:publishPlugins`

In your other local project, add to settings.gradle.kts:

```kotlin
pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}
```

And add the following to your build.gradle.kts:

```kotlin
plugins {
    id("compiler.gradleplugin.test") version "1.0.4" // Use the version published to Maven Local
}
```

`

### Project Structure

- <kbd>lib</kbd> - A Kotlin Multiplatform project which applies a gradle plugin (compiler.plugin.helloworld) which triggers the compiler plugin.
- <kbd>compiler-plugin</kbd> - This module contains the Kotlin Compiler Plugin
- <kbd>gradle-plugin</kbd> - This module contains the gradle plugin which trigger the compiler plugin


## Acknowledgments

Projects that helped me understand how to setup the project:
* [Foso/KotlinCompilerPluginExample](https://github.com/Foso/KotlinCompilerPluginExample)
