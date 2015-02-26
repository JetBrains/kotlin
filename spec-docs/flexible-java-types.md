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

## Warnings on nullability misuse

A type loaded from Java is said to *bear* a `@Nullable`/`@NotNull` annotation when
 - it's a return type a method so annotated;
 - it's a type of a field or a parameter so annotated;
 - it's a so annotated type (Java 8 and later).

A value is `@Nullable`/`@NotNull` when its type bears such an annotation.

Inside this section, a value is *nullable*/*not-null* when
 - it's `@Nullable`/`@NotNull`, or
 - it's type in Kotlin when refined with data flow info is nullable/not-null.

The compiler issues warnings specific to `@Nullable`/`@NotNull` in the following situations:
 - a `@Nullable` value is assigned to a not-null location (including passing parameters and receivers to functions/properties);
 - a nullable value is assigned to a `@NotNull` location;
 - a `@NotNull` value is dereferenced with a safe call (`?.`), used in `!!` or on the left-hand side of an elvis operator `?:`;
 - a `@NotNull` value is compared with `null` through `==`, `!=`, `===` or `!==`

## More precise type information from annotations

Goals:
 - Catch more errors related to nullability in case of Java interop
 - Keep all class hierarchies consistent at all times (no hierarchy errors related to incorrect annotations)
 - (!) If the code compiled with annotations, it should always compile without annotations (because external annotations may disappear)

 This process never results in errors. On any mismatch, a bare platform signature is used (and a warning issued).

### Annotations recognized by the compiler

- `org.jetbrains.annotations.Nullable` - value may be null/accepts nulls
- `org.jetbrains.annotations.NotNull` - value can not be null/passing null leads to an exception
- `org.jetbrains.annotations.ReadOnly` - only non-mutating methods can be used on this collection/iterable/iterator
- `org.jetbrains.annotations.Mutable` - mutating methods can be used on this collection/iterable/iterator
- `kotlin.jvm.KotlinSignature(str)` - `str` is a string representation of a more precise signature

See [appendix](#appendix) for more details

### Enhancing signatures with annotated declarations

NOTE: the intention is that if the enhanced signature is not compatible with the overridden signatures from superclasses,
it is discarded, and a warning is issued. We also would like to discard only the mismatching parts of the signature, and thus keep as
much information as possible.

Example:

``` java
class Super {
    void foo(@NotNull String p) {...}
}

class Sub extends Super {
    @Override
    void foo(@Nullable String p) {...} // Warning: Signature does not match the one in the superclass, discarded
}
```

#### @KotlinSignature

``` java
package kotlin.jvm;

@Target({ElementType.METHOD, ElementType.TYPE, ElementType.CONSTRUCTOR, ElementType.FIELD})
public @interface KotlinSignature {
    String value();
}
```

Usage:

``` java
class C {
    @KotlinSignature("fun foo(): MutableList<String?>")
    List<String> foo() { ... }
}

```

Name resolution: `@KotlinSignature` can use short names from types, because full names are already specified in respective positions in
the Java signature.

- If there's a `@KotlinSignature` annotation, other annotations are ignored
- If erasure of the signature specified by `@KotlinSignature` differs from the actual erased signature,
  a warning is reported and the `@KotlinSignature` annotation is ignored
- Otherwise, the signature from `@KotlinSignature` is used.

#### @Nullable, @NotNull, @ReadOnly, @Mutable

What can be annotated:
 - field: annotation applies to its type
 - method: annotation applies to its return type
 - parameter: annotation applies to its type
 - type (in Java 8): annotation applies to this type

Consider a type `(L..U?)`. Nullability annotations enhance it in the following way:
 - `@Nullable`: `(L?..U?)`
 - `@NotNull`: `(L..U)`

Note that if upper and lower bound of a flexible type are the same, it is replaced by the bounds (e.g. `(T?..T?) => T?`)

Consider a collection type `(MC<T>..C<T>?)` (upper bound may be nullable or not). Mutability annotations enhance it in the following way:
 - `@ReadOnly`: `(C<T>..C<T>?)`
 - `@Mutable`: `(MC<T>..MC<T>?)`

Nullability annotations are applied after mutability annotations.

Examples:

| Java | Kotlin|
|------|-------|
| `Foo` | `Foo!` |
| `@Nullable Foo` | `Foo?` |
| `@NotNull Foo` | `Foo` |
| `List<T>` | `(Mutable)List<T!>!` |
| `@ReadOnly List<T>` | `List<T!>!` |
| `@Mutable List<T>` | `MutableList<T!>!` |
| `@NotNull @Mutable List<T>` | `MutableList<T!>` |
| `@Nullable @ReadOnly List<T>` | `List<T!>?` |

*NOTE*: array types are never flattened: `@NotNull Object[]` becomes `(Array<Any!>..Array<out Any!>)`.

### Propagating type information from superclasses

A signature is represented as a list of its parts:
 - upper bounds of type parameters
 - value parameter types
 - return type

Enhancement rules (the result of their application is called a *propagated signature*) for each part:
 - collect annotations from all supertypes and the override in the subclass
 - for parts other than return type (which may be covariantly overridden) if there are conflicts (`@Nullable` together with `@NotNull` or
   `@ReadOnly` together with `@Mutable`), discard the respective annotations and issue appropriate warnings
 - for return types (only the 0-index, see below):
     - fist, take annotations from supertypes, and among them: if there's `@Nullable`, discard `@NotNull`, if there's `@Mutable` discard `@ReadOnly`
     - then if in the subtype there's `@Nullable` and in the supertype there's `@NotNull`, discard the nullability annotations (analogously,
       for mutability annotations)
 - apply the annotations and check compatibility with all parts from supertypes, if there's any incompatibility, issue a warning and take
   a platform type

Detecting annotations on parts from supertypes:
 - consider all types have the form of `(L..U)`, where an inflexible type `T` is written `(T..T)`
 - if `L` is nullable, say that `@Nullable` annotation is present
 - if `U` is not-null, say that `@NotNull` is present
 - if `L` is a read-only collection/iterable/iterator type, say that `@ReadOnly` is present
 - if `U` is a mutable collection/iterable/iterator type, say that `@Mutable` is present

Examples:

``` java
interface A {
  @NotNull
  String foo(@NotNull String p);
}

interface B {
  @Nullable
  String foo(@Nullable String p);
}

interface C extends A, B {
  // this is an override in Java, but would not be an override in Kotlin because of a conflict in parameter types: String vs String?
  // Thus, the resulting descriptor is
  //    fun foo(p: String!): String
  // return type is covariantly enhanced to not-null,
  // a warning issued about the parameter

  @Override
  String foo(String p);
}
```

Other cases:

```
R foo(@NotNull P p) // super A
R foo(P p) // super B
R foo(P p) // subclass C

// Result:
fun foo(p: P): R! // parameter type propagated from A
```

```
R foo(@NotNull P p) // super A
R foo(P p) // super B
R foo(@Nullable P p) // subclass C

// Result:
fun foo(p: P!): R! // conflict on parameter between A and C
```

```
R foo(P p) // super A
R foo(P p) // super B
R foo(@NotNull P p) // subclass C

// Result:
fun foo(p: P): R! // parameter type specified in C, no conflict with superclasses
```

```
@NotNull
R foo(P p) // super A
R foo(P p) // super B
@Nullable
R foo(P p) // subclass C

// Result:
fun foo(p: P!): R! // conflict on return type: subtype wants a nullable, but not-null already promised
```

```
R foo(@NotNull @ReadOnly List<T> p) // super A
R foo(@Nullable @ReadOnly List<T> p) // subclass B

// Result:
fun foo(p: List<T>!): R! // conflict on nullability, no conflict on mutability
```

```
fun foo(MutableList<T> p): R // super A, written in Kotlin or has @KotlinSignature
@Nullable
R foo(List<T> p) // subclass B

// Result:
fun foo(MutableList<T> p): R! // parameter propagated from superclass (@Mutable, @NotNull), conflict on return type
```

*NOTE*: nullability warnings should still be reported in the Kotlin code in case of discarding the enhancing information due to conflicts.

**Propagation into generic arguments**. Since annotations have to be propagated to type arguments as well as the head type constructor,
the following procedure is used. First, every sub-tree of the type is assigned an index which is its zero-based position in the textual
representation of the type (`0` is root). Example: for `A<B, C<D, E>>`, indices go as follows: `0 - A<...>, 1 - B, 2 - C<D, E>, 3 - D, 4 - E`,
which corresponds to the left-to-right breadth-first walk of the tree representation of the type. For flexible types, both bounds are indexed
in the same way: `(A<B>..C<D>)` gives `0 - (A<B>..C<D>), 1 - B and D`. Now, in the aforementioned procedure, annotations are collected and
considered *at each index*, and only index 0 of the return type is considered as possibly covariant (this is done for simplicity).

Example:
 - `Mutable(List)<A!>!`
 - `Mutable(List)<A?>!`
 - 0: `Mutable(List)<A>!`, `Mutable(List)<A?>!`
 - 1: `A!`, `A!`, `A?`, `A?`

NOTE: if the set of descriptors overridden by the resulting enhanced signature differs from the set overridden by the platform signature,
the enhanced signature must be discarded and a warning issued.

Checklist:
   - any platform signature should override any enhanced/propagated signature created for the same member or one of its overridden.

### Appendix

We can also support the following annotations out-of-the-box:
* [`android.support.annotation`](https://developer.android.com/reference/android/support/annotation/package-summary.html)
 * [`android.support.annotation.Nullable`](https://developer.android.com/reference/android/support/annotation/Nullable.html)
 * [`android.support.annotation.NonNull`](https://developer.android.com/reference/android/support/annotation/NonNull.html)
* From [FindBugs](http://findbugs.sourceforge.net/manual/annotations.html) and [`javax.annotation`](https://code.google.com/p/jsr-305/source/browse/trunk/ri/src/main/java/javax/annotation/)
 * `*.annotations.CheckForNull`
 * `*.NonNull`
 * `*.Nullable`
* [`javax.validation.constraints`](http://docs.oracle.com/javaee/6/api/javax/validation/constraints/package-summary.html)
 * `NotNull` and `NotNull.List`
* [Project Lombok](http://projectlombok.org/features/NonNull.html)
* [`org.eclipse.jdt.annotation`](https://wiki.eclipse.org/JDT_Core/Null_Analysis)
* [`org.checkerframework.checker.nullness.qual`](http://types.cs.washington.edu/checker-framework/current/checker-framework-manual.html#nullness-checker)
