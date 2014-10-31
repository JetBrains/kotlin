# Dynamic Types

## Goal

Interoperability with native JavaScript

## Examples

Unbounded dynamic type:
``` kotlin
fun jsFun(p: dynamic): dynamic
```

or

All types are dynamic by default
``` kotlin
dynamic fun jsFun(p, r = foo(), typed: Type /*this one is not dynamic*/) { }
```

Bounded dynamic type:
``` kotlin
fun foo(p: dynamic A): dynamic B
```

## TODO

- [ ] Dynamic functions?
  - [ ] what is the default return type? 
  - [ ] Can we omit `return` expressions when the return type is `dynamic`?
  - [ ] Can we return `Unit` when return type is `dynamic`?
- [ ] Dynamic classes/traits?
  - [ ] All members are implicitly `dynamic`
  - [ ] All types whose type constructors are marked `dynamic` are themselves dynamic types

## Typing rules

- `dynamic` is assignable to anything
- everything is assignable to `dynamic`
- `dynamic T` is assignable to `T`
- subtypes of `T` are assignable to `dynamic T`
- `dynamic` is the same as `dynamic Any?`

??? When receiver/argument is of type `R := dynamic T`
- if a call is resolved with `T` instead of `R`, it is a static call, no dynamicity involved
- 

