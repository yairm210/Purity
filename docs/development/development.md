
## Development

### Testing

To test the plugin on /lib, run `./gradlew :lib:clean :lib:build` - the plugin is only active when the build cache is changed

JvmSampleTests.kt is not a standard test suite - it succeeds if it compiles.
However, it is still divided into the various test cases, which are all standalone.

To debug, you can run gradle lib:build *via IntelliJ* to debug the actual compiler plugin. (Need to click f9 4 times on gradle, not sure how to fix)

To test on other projects, first increment the version (see below) - otherwise the plugin from the gradle plugin repository will be used.

### Publishing locally for use in local projects

To publish the *compiler plugin* locally:

- `./gradlew :compiler-plugin:publishToMavenLocal -PskipSigning=true`
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
Thus, the version must be updated in multiple places - just search-and-replace the version across everything since that will include documentation as well.

The parts that will preclude publishing are: 

- Update version in compiler plugin - `compiler-plugin/build.gradle.kts`
- Update version in gradle plugin - `PurityGradlePlugin.kt`
- Update version in `gradle-plugin/build.gradle.kts`

To publish, add a git tag `<version>` and push it to GitHub - the GitHub action will take care of the rest.
