# Flexible Java Types

## Goals

* Eliminate the need in external annotations for compilation
 * Compilation results (errors) will never depend on availability of annotations
* Eliminate some problems in loading Java descriptors (propagation issues, raw types etc)
* Facilitate future development of dynamic types

## Flexible Types

This is a new kind of types. A flexible type consists of two inflexible ones: a lower bound and an upper bound, written

```
(Lower..Upper)
```

This syntax is not supported in Kotlin. Flexible types are non-denotable.

Invariants:
* `Lower <: Upper` (also, can't be the same)
* `Lower`, `Upper` are not flexible types themselves, but may contain flexible types (e.g. as type arguments)
* `Lower`, `Upper` are not error types

Subtyping rules:

Let `T`, `L`, `U`, `A`, `B` be inflexible types. Symbol `|-` (turnstile) means "entails".

* `L <: T   |-   (L..U) <: T`
* `T <: U   |-   T <: (L..U)`
* `A <: U   |-   (A..B) <: (L..U)`

Least Upper Bound (aka "common supertype"):

* `lub[(A..B), (C..D)] = (lub[A, C], lub[B, D])

Type equivalence (aka `JetTypeChecker.DEFAULT.equalTypes()`):

`T1 ~~ T2 <=> T1 <: T2 && T2 <: T1`

 NOTE: This relation is NOT transitive: `T?` ~~ (T..T?)` and `(T..T?) ~~ T`, but `T? !~ T`


## Loading Java Types

For the sake of notation, we'll write `k(T)` for a Kotlin type loaded for a Java type `T`

A Java type `T` that legitimately has no type arguments (not a Raw type) is loaded as

```
k(T) = (T..T?) // T is not a generic type,  notation: T!
k(G<T>) = (G<k(T)>..G<k(T)>?)            // notation: G<T!>!
k(T[]) = (Array<k(T)>..Array<out k(T)>?) // notation: Array<(out) T!>!
k(java.util.Collection<T>) = (kotlin.MutableCollection<k(T)>..kotlin.Collection<k(T)>?)
                                         // notation (Mutable)Collection<T!>!
```

Examples:

```
k(java.lang.String) = kotlin.String!
k(int) = kotlin.Int // No flexible types here
k(java.lang.Integer) = kotlin.Int!
k(Foo<Bar>) = Foo<Bar!>!
k(int[]) = IntArray
```

## Overriding

When overriding a method from a Java class, one can not use flexible type, only replace them with denotable Kotlin types:

```java
class Foo {
    List<String> list(String s);
}
```

```kotlin
class Bar : Foo() {
    override fun list(s: String): List<String>
    // or
    override fun list(s: String?): List<String?>?
    // or
    override fun list(s: String?): List<String>?
    // or
    override fun list(s: String): MutableList<String?>
    // or
    // any other combination of nullability and mutability
}
```

## Translation to Java byte codes

Goal: blow early when a null is assigned to a non-null holder.

* Assignment/method call

If there's an expected type and the upper bound is not its subtype, an assertion should be emitted.

Examples:
```kotlin
val x: String = javaStringMethod() // assert that value is not null
val y: MutableList<Foo> = javaListMethod() // assert that value "is MutableList" returns true
val arr: Array<Bar> = javaArrayMethod() // assert value "is Bar[]"
```

* Increment, assignment operations (+= etc)

`a++` stands for `a = a.inc()`, so
- check a to satisfy the `a.inc()` conditions for receiver
- check `a.inc()` result for assignability to `a`

## Assertion Generation

Constructs in question: anything that provides an expected type, i.e.
 - assignments
 - parameter default values
 - delegation by: supertypes and properties
 - dereferencing: x.foo
 - all kinds of calls (foo, foo(), x[], x foo y, x + y, x++, x += 3, for loop, multi-declarations, invoke-convention, ...)
 - explicit expected type (foo: Bar)
 - for booleans: if (foo), foo || bar, foo && bar (!foo is a call)
 - argument of throw