# Dynamic Types

## Goal

Interoperability with native JavaScript

## Examples

Unbounded dynamic type:
``` kotlin
fun jsFun(p: dynamic): dynamic
```

## TODO

- [ ] Dynamic functions?
  - [ ] what is the default return type?
  - [ ] Can we omit `return` expressions when the return type is `dynamic`?
  - [ ] Can we return `Unit` when return type is `dynamic`?
- [ ] Dynamic classes/traits?
  - [ ] All members are implicitly `dynamic`
  - [ ] All types whose type constructors are marked `dynamic` are themselves dynamic types
- [ ] Bounded dynamic types

## Typing rules

- `dynamic` is assignable to anything
- everything is assignable to `dynamic`
- `dynamic` variable may hold `null`
- `dynamic?` is the same as `dynamic`, a warning should be issued
- `lub(T, dynamic) = dynamic`
- ??? `glb(T, dynamic) = T`
- `dynamic` can't be substituted for reified parameters of function/constructor calls (this means that it's not possible to create an array of `dynamic`)
- `dynamic` can't be used as a supertype or upper bound for a tpe parameter

## Syntax

```
type
    : ...
    | "dynamic"
    ;
```

"dynamic" is a *soft keyword*:
- if it occurs in a non-type context, it's an identifier
- in a type context, when followed by a dot or an angle bracket `<`, it's an identifier

## Representation

Internally, `dynamic` is represented as a flexible type `Nothing..Any?`, with the following capabilities:
- makeNullable has no effect
- (???) makeNotNull changes it to a non-null version: `Nothing..Any`
- if a receiver of a call is dynamic (or a dynamic implicit receiver is available), and the call can not be resolved statically
  (no fitting candidates are found, NOTE: this does not include ambiguity), a dynamic candidate descriptor is created for the arity of the call,
  and the call is resolved to it.
- (???) All methods of JetType are delegated to the upper bounds, instead of lower bound

## Implications

`Nothing` being mentioned, there's a risk of taking `dynamic` for a bottom type in some contexts, this is not intended and should be tested carefully.

## Type Argument Inference

When expected type of a call is `dynamic`, it does not automatically provide type arguments for nested calls.
Example:

``` kotlin
fun foo(d: dynamic) {...}

foo(listOf()) // can't determine T for listOf<T>()
```

Discussion:
- we could tweak inference so that it takes `dynamic` as a bound for all type variables whose containing type has a dynamic bound,
but it's hard to be sure it's worth the while

## Notes

- dynamic types are not supported on the JVM back-end
