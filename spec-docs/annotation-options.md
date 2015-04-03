# Annotation Options

Goals:
* Support annotation options, such as retention policy and targeting

See [related discussion about Scala](http://lampwww.epfl.ch/~mihaylov/attributes.html).

## Discussion

For each option of annotations there's a general dichotomy of how it can be specified in code.

Option 0: Separate annotations

``` kotlin
retention(SOURCE)
target(CLASSIFIER, FIELD)
annotation class example
```

Option 1: Make `annotation` into an annotation, and use its properties

``` kotlin
annotation(
    retention = SOURCE,
    targets = array(CLASSIFIER, FIELD)
)
class example
```

A variation of this is

``` kotlin
annotation(target(CLASSIFIER, FIELD), retention = SOURCE) class example
```

Annotations can be parameters to other annotations.

Having option as separate annotation is what Java has and seems more extensible, although it actually isn't (adding new parameters to one annotation is no better or worse than adding new annotation recognized by the compiler).

Having those as parameters is more discoverable, but has some syntactic shortcomings: no varargs can be used.

Option 2 (final): combination

``` kotlin
target(CLASSIFIER, FIELD) annotation(retention = SOURCE, repeatable = false, documented = false) class example
```

It makes sense because target can have vararg as its argument, and everything else (retention, repeatable, and may be documented / inherited in future) is combined into one annotation.

There is an important question here: what is an annotation if annotation is an annotation itself? The following answers are possible:
* Everything annotated with annotation, regardless of its resolving
* kotlin.annotation.annotation itself AND everything annotated with (correctly resolved) kotlin.annotation.annotation

Looks like the second answer makes more sense.

## Targeting

To check applicability, we can use the following constants:

| Kotlin constant | Java constant |
|-----------------|---------------|
| PACKAGE | \<same> |
| CLASSIFIER | TYPE |
| ANNOTATION_CLASS | ANNOTATION_TYPE |
| TYPE_PARAMETER | \<same>
| PROPERTY | \<no analog> |
| FIELD | \<same>
| LOCAL_VARIABLE | \<same> |
| VALUE_PARAMETER | PARAMETER |
| CONSTRUCTOR | \<same> |
| FUNCTION | METHOD |
| PROPERTY_GETTER | METHOD |
| PROPERTY_SETTER | METHOD |
| TYPE | TYPE_USE |
| EXPRESSION | \<no analog> |
| FILE | \<no analog> |

Putting an annotation on an element that is not allowed by the specified target is a compile-time error.
No targets specified means that all targets which exist in Java6 are accepted (everything except TYPE_PARAMETER, TYPE, EXPRESSION, and FILE).

> NOTE: Java has the following [targets](https://docs.oracle.com/javase/8/docs/api/java/lang/annotation/ElementType.html)
By default, Java has everything but Java8-specific targets (`TYPE_USE`, `TYPE_PARAMETER`), which makes it unclear as of which target should we take by default.

One option to work around the problem of adding more targets later: have an explicit `ALL` target. But there's the issue of matching it with Java's one.

For `TYPE` it may make sense to add an extra `@typeTarget` annotation with the following options:
* `ALL` - any usage of types
* `RETURN_TYPE` (including that of function types?)
* `VALUE_PARAMETER_TYPE`(including that of function types?, including receiver types?)
* `TYPE_ARGUMENT`
* `TYPE_CONSTRUCTOR` (complement of `TYPE_ARGUMENT`)
* `SUPERTYPE`
* `UPPER_BOUND`
* `ANNOTATION_TYPE`
* `CONSTRUCTOR_USAGE` (this one is an issue: use site is ambiguous with annotated expression)
* <maybe more>

Also there are some exotic type usages, such as ones on outer types: `@A (@B Outer).Inner`, here `@A` belongs to `Inner`, and `@B` belongs to `Outer`.

**TODO** Open question: what about traits/classes/objects?
**TODO** local variables are just like properties, but local



> Possible platform-specific targets
* SINGLETON_FIELD for objects
* PROPERTY_FIELD
* (?) DEFAULT_FUNCTION
* (?) LAMBDA_METHOD
* PACKAGE_FACADE
* PACKAGE_PART

### Mapping onto Java

Kotlin has more possible targets than Java, so there's an issue of mapping back and forth. The table above gives a correspondence.

When we compile a Kotlin class to Java, we write a `@java.lang.annotation.Target` annotation that reflects the targets. For targets having no correspondent ones in Java (e.g. `EXPRESSION`) nothing is written to `j.l.a.Target`. If the set of Java targets is empy, `j.l.a.Target` is not written to the class file.

In addition to `java.lang.annotation.Target`, a Kotlin-specific annotation `kotlin.target` is written containing all the Kotlin targets listed:

``` kotlin
package kotlin.annotation

enum class AnnotationTarget {
    PACKAGE
    ...
}

target(ANNOTATION_CLASS)
annotation(RUNTIME) class target(vararg allowedTargets: AnnotationTarget)
```

When loading an annotation, we only read `kotlin.target`. When `kotlin.target` is missing, on the JVM, we read `j.l.a.Target` and map its values to Kotlin ones according to the table above. This implies that we can load pure Java annotations that know nothing about Kotlin, and that an annotation written in Java can be targeted, e.g. for Kotlin expressions, because one can simply manually specify `kotlin.target` for it.

### Syntax

It makes sense to use `kotlin.target` explicitly in Kotlin code:

``` kotlin
target(EXPRESSION, TYPE)
annotation class MyAnn
```

> An alternative would be to make target a property of `kotlin.annotation`, but then we'd
* lose the advantage of varargs, because there are more optional parameters
* be non-uniform with Java, thus making it harder to figure how to make a Java annotation Kotlin-friendly

## Retention

> NOTE: Retention is a Java-specific concern, more or less. CLR retains all attributes at runtime, and JS too

It makes a lot of sense to make `RUNTIME` the default retention.

Since `RetentionPolicy.CLASS` is not a good fit for Kotlin that has functions outside any class, it's better to have `BINARY` instead. Also, we could not use `java.lang.annotation.RetentionPolicy` anyways, since it's platform-specific. Thus, we need to have our own enum:

``` kotlin
package kotlin.annotation

enum class AnnotationRetention {
    SOURCE
    BINARY
    RUNTIME
}
```

> Now, we could map `java.lang.annotation.Retention` and `RetentionPolicy` to `kotlin.retention` and `kotlin.AnnotationRetention`, and then map `CLASS` to `BINARY`, but that is a little too much

Then, it makes sense to make `retention` a property of `kotlin.annotation`:

``` kotlin
target(TYPE)
annotation(SOURCE) class MyAnn
```

The following checks must be performed at compile time:
* `EXPRESSION`-targeted annotations can only have retention `SOURCE`

## Repeatable

> Java has `Repeatable` as an annotation, but we cannot map a Kotlin type to it, because it is only present since JDK 8, and cannot be written to class files with version lower than 8.

We make `repeatable` a boolean property of `kotlin.annotation`, with default value `false` (as in Java and C#).

If a non-repeatable annotation is used multiple times on the same element, it is a compile-time error.

If a repeatable annotation with binary or runtime retention is used multiple times on the same element, but the target byte code version is lower than Java 8, it is a compile-time error.

A repeatable annotation with source retention may be used multiple times on any platform. A repeatable annotation with any retention may be used multiple times on a non-JVM platform.

## Documented

We make `documented` a boolean property of `kotlin.annotation`, with default value `false`. This property is mapped to the same platform-specific annotation, if any.

## Inherited

This one is of rather unclear value, and we do not support it in Kotlin. One can use platform-specific annotation to express it.

## Appendix. Definition of kotlin.annotation

``` kotlin
package kotlin.annotation

target(ANNOTATION_CLASS)
annotation(SOURCE) class annotation(
    val retention: AnnotationRetention = RUNTIME,
    val repeatable: Boolean = false,
    val documented: Boolean = false
)
```
