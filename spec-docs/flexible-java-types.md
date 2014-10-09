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

For the special case where `Upper = Lower?`, i.e. `(T..T?)`, for brevity we write `T!`.


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

## Equality and Domination

We have to distinguish syntactic and semantic equality of types.

*Syntactic equality* (`==`, implemented by JetType.equals()) means exact match of desugared forms of types, thus `T` != `T!`.

*Semantic equality* (`~~`, implemented by `JetTypeChecker.DEFAULT.equalTypes()`) (which is not an equality relation) means that one type may be used instead of another. Formally:

```
  T1 ~~ T2 <=> T1 :< T2 && T2 :< T1
```

thus, `T ~~ T!`, `List<T>! ~~ List<T!>`, etc.

**Note** that `~~` is not transitive: `T ~~ T!` and `T! ~~ T?`, but `T !~ T!`.

Syntactically equivalent types are indistinguishable, unlike some semantically equivalent types, and when we have a set of syntactically unequal,
but semantically equal types, we can't pick any of them as a representative.

Example:

```
class Inv<T>
fun <T> foo(a: Inv<T>, b: Inv<T>): T

fun test(a: Inv<Foo>, b: Inv<Foo!>) {
    foo(a, b)
}
```

In this case, the constraints on `T` are:
 - `T = Foo`
 - `T = Foo!`

We have to choose a representative from the set `{ Foo, Foo! }`, and make it the resulting value for type variable `T`.
Intuitively, `Foo!` is better than `Foo` in this case, but there can be trickier cases as well:
- `{ List<Foo!>, List<Foo>! }`
- `{ Foo<Bar!, Bar>, Foo<Bar, Bar!>! }`

The latter case suggests that we need to find a *common dominating type* for a set of types, which is a non-trivial operation.

Let's say that `B` is dominated by `A` and write `B <~ A`, iff

- `A ~~ B`
- at least one of the following holds
  - `A == B`
  - `A = (Al..Au)`, `B = (Bl..Bu)` where `Bl <~ Al`, `Bu <~ Au`
  - `A == B!`
  - `A = C<Pa>` or `A = C<Pa>!`, `B = C<Pb>` and `Pb <~ Pa` (for many arguments, analogously)
  - `A = (Sub<Psub>..Super<Psub>)`, `B = Middle<Pmid>` and



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