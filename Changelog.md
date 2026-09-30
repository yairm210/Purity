## 1.8

Added @ReturnsNewInstance marking for functions, allowing us to recognize their return values as "internal state" in functions that call them

## 1.7

Added @Mutated annotation for functions that mutate their inputs, to allow calling them from readonly/pure functions with owned values passed through. 

## 1.6

Allow @Suppress("purity") to work on single var/val assignments

## 1.5

Better support for overriding pure functions from parent classes/interfaces

## 1.4

[Support for KMP targets](https://github.com/yairm210/Purity/pull/52]) - by @Sintrastes

[Fixed problems with files with no package declaration](https://github.com/yairm210/Purity/pull/49) - by @Sintrastes

[Better error messages for passed lambda functions]() 

## 1.3

Support for Kotlin 2.2.x - Kudos @findusl

## 1.2

If a function is known to return a new instance, the result is considered local state and can be mutated

## 1.1

Can now +=, *=, etc for local state variables without breaking purity

## 1.0 

Initial release!
