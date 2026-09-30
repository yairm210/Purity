# What does Pure mean, in a referenced language?

Mathematically, Pure is a concept that applies to **values**, 
and in a language like Kotlin where references are everywhere, it starts being fuzzy at the edges.

In this plugin, we ensure that **values returned by @Pure functions will always be the same.**
What we do NOT ensure, is that the **instances** will be the same; Nor do we ensure that **fields of instances** will be the same.

Take the following example:

```kotlin
val sharedList = ArrayList<String>()
@Pure fun getList() = sharedList
```
getList is @Pure since it **is not affected by side effects**, and **does not cause side effects**.
However, the value INSIDE the shared list may be mutated; We get the **same box** back but its **contents** may change.

We could disallow returning mutable data from pure functions; But then the following function would be considered impure:
```kotlin
@Pure fun getList(list: List) = list
```
and if an **identity function** is not pure then where even are we?

So, if Pure functions can return lists whose contents change, what ARE we enforcing? 
We're enforcing the original mathematical definition - the value boundary. 
Since Pure functions **cannot use** the mutable data within the list, they can in effect return boxes whose contents can change, but they **cannot open them.**

# Function attributes vs value attributes

Purity encompasses 2 different types of attributes: function attributes and value attributes.

**Function attributes** are relatively simple to check deeply. The entire code is available at compilation time; We can see which functions call which other functions and infer information, similar to inferring type propagation.

**Value attributes** in this plugin are shallow - they only consider the **top level instance**, and do not tell us anything about the nested values.

Checking **at compile time**, attributes of nested data that will be known only **at runtime**, is a Hard Problem, and is outside the scope of this plugin.

# Autodetection vs explicit marking

Where possible we want users to be able to use "easily detectable" functions as if they were marked explicitly.

The question is, how deeply do we check this. Since most function markers are semi-transitive (I am X if I call a function that is Y or X),
we cannot scan the entire calltree for autodetection without getting into really gnarly problems of recursiveness.

What we can do, is for a specific subset of functions, detect if this counts.

# What do we validate?

The base of this plugin is that you **should not** be able to mark a function with an attribute, if that function does not conform to rules for that attribute.

However, code outside the purview of compilation is not available for validation.

Therefore:
- Functions marked directly with attributes - code available for validation - is validated
- Functions marked by fully qualified name in the plugin settings - code unavailable - **taken at face value**, trusting the user.
