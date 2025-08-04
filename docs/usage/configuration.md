
## Configuration

### Disable validation

To disable all checks, either by some computed condition or just globally:

```kotlin
import yairm210.purity.PurityConfiguration // at the top of your build.gradle.kts

configure<PurityConfiguration> {
    enabled = false
}
```

This is a cheap way to validate that the plugin is not the cause of compilation or latency problems.

### Handling external classes

To support idiomatic Kotlin, Purity recognizes pure and readonly functions of well-known classes.

You can expand this recognition in the following way:

```kotlin
import yairm210.purity.PurityConfiguration // at the top of your build.gradle.kts

configure<PurityConfiguration> { // All of these are examples that are already contained in the known functions/classes 
  wellKnownPureClasses = setOf("kotlin.ranges.IntRange")
  wellKnownPureFunctions = setOf("kotlin.collections.listOf")
  wellKnownReadonlyFunctions = setOf("java.util.EnumMap.get")
  wellKnownInternalStateClasses = setOf("my.internal.list") // classes to be autorecognized as '@LocalState' when constructed
}
```

### Warn where annotations can be added

By default, Purity only checks that functions are at least as strict as declared.
Once most of the code has been annotated, you may wish to see what other functions are strict enough to accept annotations,
to 'lock them in' to their current purity level.

```kotlin
import yairm210.purity.PurityConfiguration // at the top of your build.gradle.kts

configure<PurityConfiguration> { 
  warnOnPossibleAnnotations = true
}
```
