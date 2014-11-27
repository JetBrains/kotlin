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

## Typing rules

- `dynamic` is assignable to anything
- everything is assignable to `dynamic`
- `dynamic` variable may hold `null`
- `dynamic?` is the same as `dynamic`, a warning should be issued
- `lub(T, dynamic) = dynamic`
- ??? `glb(T, dynamic) = T`
- `dynamic` can't be substituted for reified parameters of function/constructor calls (this means that it's not possible to create an array of `dynamic`)
- `dynamic` can't be used as a supertype or upper bound for a type parameter
- `dynamic` is less specific than any other type

## Syntax

```
type
    : ...
    | "dynamic"
    ;
```

"dynamic" is a *soft keyword*:
- if it occurs in a non-type context, it's an identifier
- in a type context, when followed by a dot (except for a dot that separates a receiver type from a function/property name) or an angle bracket `<`, it's an identifier
- on the left-hand-side of `::` in a callable reference: `dynamic::foo` implies that `dynamic` there is a normal identifier

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

## Resolution rules

- If a receiver is `dynamic` a call is resolved as dynamic if no members (these are members of `Any`, unless we implement bounded `dynamic`)
  and no extensions with dynamic receivers match the signature.
  - Motivation: otherwise, **any** extension to **any** type that simply happens to be in scope and match the name and arguments
    will be bound for a call with a `dynamic` receiver, i.e. there's no way to force a call to be dynamic, and in the case of a `*`-import
    the code may change its semantics just because somebody added some extension in another file.
  - This means that an extension to a normal, non-dynamic type **can not** be called on a `dynamic` receiver.
    If needed, one can force a call to an extension by casting the receiver to a static type: `(d as Foo).bar()`
- Augmented assignments on dynamic receivers (e.g. `dyn += foo`) are resolved to `plusAssign()` function, not `plus`, for generality:
  this permits calling them on vals (e.g. those holding collection-like objects)
- The invoke convention is limited so that for calls like `dyn.foo()` we do not look for property `foo` that has `invoke` defined on it
  (same for other cases like `+dyn` etc)

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
- dynamic types are forbidden on the right-hand side of `is`, `!is`, `as` and `as?` (but not as generic arguments, e.g. `x is List<dynamic>` is allowed)

## Prospect on bounded dynamic types

*(not to be implemented now)*

A bounded dynamic type `dynamic B` is represented as `(Nothing .. B?)`.

Calls on such receivers are resolved statically against members of `B`, and dynamically against non-members of `B` (including extensions).

> NOTE:
this is an issue: some users would expect extensions to be bound statically, but we can't allow it, because otherwise a dynamic
call with a name clashing with a name of an extension to `B` is impossible. Options:
 - bind extensions to `B` (i.e. extensions to `Any` for `dynamic`) statically, this leads to unexpected changes in semantics when a new extension
   is added in a *-imported package. Then, to make the dynamic calls possible, provide some sort of an intrinsic extension, e.g. `dynamic`)
   that takes a string for a name and a varargs of parameters of type `dynamic`. Thus, to call a `recv.foo(a, b)` as a dynamic call, we can
   always say `recv.dynamic("foo", a, b)`.
 - never bind extensions statically on dynamic receivers, allow calling them passing the receiver as the first parameter,
   so that we can call `foo(a)` instead of `a.foo()`. This poses no risk of accidentally changing semantics of some calls from dynamic to static

Assignability rules:
 - any subtype of `B?` can be passed where `dynamic B` is expected
 - `dynamic B` can be passed where any supertype of `B` or subtype of `B`, but not a type unrelated to `B` is expected

 Unbounded `dynamic` is the same as `dynamic Any`.