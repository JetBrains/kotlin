# Reified Type Parameters

Goal: support run-time access to types passed to functions, as if they were reified (currently limited to inline functions only).

## Syntax

A type parameter of a function can be marked as `reified`:

``` kotlin

inline fun foo<reified T>() {}
```

## Semantics, Checks and Restrictions

**Definition** A well-formed type is called *runtime-available* if
- it has the form `C`, where `C` is a classifier (object, class or trait) that has either no type parameters, or all its type parameters are `reified`, with the exception for class `Nothing`,
- it has the form `G<A1, ..., An>`, where `G` is a classifier with `n` type parameters, and for every type parameter `Ti` at least one of the following conditions hold:
    - `Ti` is a `reified` type parameter and the corresponding type argument `Ai` is a runtime-available type,
    - `Ai` is a *star-projection* (e.g. for `List<*>`, `A1` is a star-projection);
- it has the form `T`, and `T` is a `reified` type parameter.

Examples:
- Runtime-available types: `String`, `Array<String>`, `List<*>`;
- Non-runtime-available types: `Nothing`, `List<String>`, `List<T>` (for any `T`)
- Conditional: `T` is runtime-available iff the type parameter `T` is `reified`, same for `Array<T>`

Only runtime-available types are allowed as
- right-hand arguments for `is`, `!is`, `as`, `as?`
- arguments for `reified` type parameters *of calls* (for types any arguments are allowed, i.e. `Array<List<String>>` is still a valid type).

As a consequence, if `T` is a `reified` type parameter, the following constructs are allowed:
- `x is T`, `x !is T`
- `x as T`, `x as? T`
- reflection access on `T`: `javaClass<T>()`, `T::class` (when supported)

Restrictions regarding reified type parameters:
- Only a type parameter of an `inline` function can be marked `reified`
- The built-in class `Array` is the only class whose type parameter is marked `reified`. Other classes are not allowed to declare `reified` type parameters.
- Only a runtime-available type can be passed as an argument to a `reified` type parameter

Notes:
- No warning is issued on `inline` functions declaring no inlinable parameters of function types, but having a `reified` type parameter declared.

## Implementation notes for the JVM

In inline functions, occurrences of a `reified` type parameter `T` are replaced with the actual type argument.
If actual type argument is a primitive type, it's wrapper will be used within reified bytecode.

``` kotlin
open class TypeLiteral<T> {
    val type: Type
        get() = (javaClass.getGenericSuperclass() as ParameterizedType).getActualTypeArguments()[0]
}

inline fun <reified T> typeLiteral(): TypeLiteral<T> = object : TypeLiteral<T>() {} // here T is replaced with the actual type

typeLiteral<String>().type // returns 'class java.lang.String'
typeLiteral<Int>().type // returns 'class java.lang.Integer'
typeLiteral<Array<String>>().type // returns '[Ljava.lang.String;'
typeLiteral<List<*>>().type // returns 'java.util.List<?>'
```
