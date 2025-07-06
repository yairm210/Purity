## What is this?

A Kotlin Compiler Plugin that determines Pure and Readonly functions. Under construction!

## Usage

Install the plugin by adding the following to your `build.gradle.kts`:

```kotlin
plugins {
    id("io.github.yairm210.purity-plugin") version "0.0.10"
}
```

Mark pure functions using `@Contract(pure = true)`, and readonly functions using `@Contract("readonly")`.

### Suppressing validity checks

Every dependency tree has leaves at the bottom. 
Often, you want to mark your lowest functions as pure or readonly, and have the compiler only check the from it and up.
You can do this my marking the function with `@Suppress("yairm210.purity")`, like so:

```kotlin
var external = 3
// reads an external variable, but will not throw an error
@Contract(pure = true) @Suppress("yairm210.purity")
fun actsAsPure(): Int {
    return external
}
```

## Development

To test the plugin on /lib, run `./gradlew :lib:clean :lib:build` - the plugin is only active when the build cache is changed

To test on other projects, first increment the version (see below) - otherwise the plugin from the gradle plugin repository will be used.

To publish the *compiler plugin* locally:
- Comment out the `signAllPublications()` line in `compiler-plugin/build.gradle.kts` if you don't have the signing keys set up
- `./gradlew :compiler-plugin:publishToMavenLocal`
- It should now be available in `~/.m2/repository/io/github/yairm210/compiler-compiler-plugin/<version>`

To publish the gradle plugin locally:
- `./gradlew :gradle-plugin:build`
- It should now be available in `~/.m2/repository/io/github/yairm210/gradle-plugin/<version>`

To use the local plugin in another local project, add to `build.gradle.kts`:

```kotlin
plugins {
    id("io.github.yairm210.purity-plugin") version "<version>" // Require the gradle plugin
}
repositories {
    mavenLocal() // To get the compiler plugin locally
}
```

And add to `settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        mavenLocal() // To get the compiler plugin locally
        gradlePluginPortal() // So other plugins can be resolved
    }
}
```
### Project Structure

- <kbd>lib</kbd> - A Kotlin Multiplatform project which applies a gradle plugin (compiler.plugin.helloworld) which triggers the compiler plugin.
- <kbd>compiler-plugin</kbd> - This module contains the Kotlin Compiler Plugin
- <kbd>gradle-plugin</kbd> - This module contains the gradle plugin which trigger the compiler plugin


### Versioning

Gradle plugins used in other projects must be included in settings.gradle.kts as 'includeBuild', so they'll be available as a plugin.

This unfortunately precludes them from depending on buildSrc for a single source of truth for the version.
Thus, the version must be updated in multiple places:

- Update version in compiler plugin - `compiler-plugin/build.gradle.kts`
- Update version in gradle plugin - `PurityGradlePlugin.kt`
- Update version in `gradle-plugin/build.gradle.kts`

## Acknowledgments

Projects that helped me understand how to setup the project:
* [Foso/KotlinCompilerPluginExample](https://github.com/Foso/KotlinCompilerPluginExample)
