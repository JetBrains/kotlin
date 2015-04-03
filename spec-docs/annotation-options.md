# Annotation Options

Goals:
* Support annotation options, such as retention policy and trageting

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

I.e.

``` kotlin
package kotlin

annotation(retention = SOURCE, targets = array(CLASSIFIER)) class annotation(
    val targets: Array<AnnotationTarget>,
    val retention: AnnotationRetention
)
```

Having option as separate annotation is what Java has and seems more extensible, although it actually isn't (adding new parameters to one annotation is no better or worse than adding new annotation recognized by the compiler).

Having those as parameters is more discoverable, but has some syntactic shortcomings: no varargs can be used.

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
| FUNCITON | METHOD |
| PROPERTY_GETTER | METHOD |
| PROPERTY_SETTER | METHOD |
| TYPE | TYPE_USE |
| EXPRESSION | \<no analog> |

Putting an annotation on an element that is not allowed by the specified target is a compile-time error.
No targets specified means that all targets are accepted.

> NOTE: Java has the following [targets](https://docs.oracle.com/javase/8/docs/api/java/lang/annotation/ElementType.html):


**TODO** Open question: what about traits/classes/objects?  
**TODO** local variables are just like properties, but local  

> Possbile platform-specific targets
* SINGLETON_FIELD for objects
* PROPERTY_FIELD
* (?) DEFAULT_FUNCTION
* (?) LAMBDA_METHOD
* PACKAGE_FACADE
* PACKAGE_PART

### Mapping onto Java

Kotlin has more possible targets than Java, so there's an issue of mapping back and forth. The table above give a correspondence.

When we compile a Kotlin class to Java, we write a `@java.lang.annotation.Target` annotation that reflects the targets. For targets having no correspondent ones in Java (e.g. `EXPRESSION`) nothing is written to `j.l.a.Target`. If the set of Java targets is empy, `j.l.a.Target` is not written to the class file. 

In addition to `java.lang.annotation.Target`, a Kotlin-specific annotation `kotlin.target` is written containing all the Kotlin targets listed:

``` kotlin
package kotlin

enum class AnnotationTarget {
    PACKAGE
    ...
}

// retention: RUNTIME
target(ANNOTATION_CLASS)
annotation class target(vararg targets: AnnotationTarget)
```

When loading an annotation, we only read `kotlin.target`. When `kotlin.target` is missing, on the JVM, we read `j.l.a.Target` and map its values to Kotlin ones according to the table above. This implies that we can load pure Java annotations that know nothing about Kotlin, and that an annotation written in Java can be targeted, e.g. for Kotlin expressions, because one can simply manually specify `kotlin.target` for it.

### Syntax

It makes sense to use `kotlin.target` explicitly in Kotlin code:

``` kotlin
target(EXPRESSION, TYPE)
annotation class MyAnn
```

> An alternative would be to make target a aproperty of `kotlin.annotation`, but then we'd
* lose the advantage of varargs, because there are more optional parameters
* be non-uniform with Java, thus making it harder to figure how to make a Java annotation Kotlin-friendly

## Retention

> NOTE: Retention is a Java-specific concern, more or less. CLR retains all attributes at runtime, and JS too

It makes a lot of sense to make `RUNTIME` the default retention.

Since `RetentionPolicy.CLASS` is not a good fit for Kotlin that has functions outside any class, it's better to have `BINARY` instead. 

## Repeatable

## Inherited and Documented

