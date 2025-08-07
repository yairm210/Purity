# Development Principles

## Minimum user interface

Ideally, users should only need to add `@Pure` and `@Readonly` to functions. **Everything else** should be determined automatically.

In practice, this is not possible, and thus 'escape hatches' - like `@LocalState` and `@Immutable` - are needed. 
We strive to provide users with enough options that they don't need to change their existing code to accomodate purity checking, 
and to provide enough automation they only need the escape hatches in cases we cannot statically determine purity.

## Everything is configurable

We have many long lists of "well known" things - pure and readonly classes and types, local-state classes, etc.

Each of these should have 3 possible sources:

- Configurations for **Java and Kotlin standard libraries** should be in well-known hardcoded lists in the compiler plugin
- Configuration for **external libraries** should be possible via the plugin configuration in Gradle
- Configuration for **the code being checked** should be addable via annotations on the code itself 
