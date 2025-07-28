## Kotlin IR Documentation

Kotlin IR is virtually undocumented, so here are some notes

## IrFunction

Represents a function, either declared in your code - in which case it has a body of declarations - or declared externally.

In either case, what you do know about it is the input and output types.

## IrDeclaration

A 'line of code' - can be a function call, setting a variable, an if statement, etc.

## IrExpression

This is 'anything that returns a value'. Some prime examples are:

- `IrCall` - a function call
- `IrConst` - a hardcoded const value - e.g. `1`, `"hi"`, `false`
- `IrGetValue` - a local variable

## IrCall

An `IrExpression` representing a function call - x() or a.x().

`irCall.symbol.owner` gets you to the IrFunction.

When the function is part of a class (a.x()), `irCall.dispatchReceiver` lets you retrieve the IrExpression to the left of the dot - in this case `a`.

When it's an extension function, the same applies to `irCall.extensionReceiver`.


## IrGetValue

An `IrExpression` that retrieves the value of a local variable, which can be val or var.

To find out about the actual val/var being referenced, you need to check `irGetValue.symbol.owner` which is a `IrValueDeclaration`.

For example, `irValueDeclaration.isVar` indicates mutability.

## IrProperty

Unlike local variables, all class properties are represented by `IrProperty`, which generates behind the scenes:

- An `IrField` for the backing field
- a getter `IrFunction`
- a setter `IrFunction` (for `var` properties)

This means all access to properties is through the getter/setter functions (`IrCall`) and not through `IrGetValue`
