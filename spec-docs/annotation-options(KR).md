# Annotation Options

Goals:
* 보존정책 및 타겟팅과 같은 어노테이션 옵션 지원

See [related discussion about Scala](http://lampwww.epfl.ch/~mihaylov/attributes.html).

## Discussion

주석의 각 옵션에는 코드에서 지정하는 방법에 대한 일반적인 이분법이 있습니다.

Option 0 (final): 자바와 같은 방식의 어노테이션 분리

``` kotlin
@Retention(SOURCE)
@Target(CLASS, FIELD)
annotation class example
```

Option 1: 어노테이션 안에 `annotation`을 만들고 속성 사용하기 

``` kotlin
@annotation(
    retention = SOURCE,
    targets = array(CLASS, FIELD)
)
class example
```

위 코드의 다른 형태

``` kotlin
@annotation(target(CLASS, FIELD), retention = SOURCE) class example
```

어노테이션은 다른 어노테이션의 매개변수가 될 수 있습니다.

별개의 주석으로 옵션을 갖는 것은 자바에도 있고 확장성 있어보이지만 실제로는 그렇지 않습니다(한 어노테이션에 새 매개변수를 추가하는 것은 컴파일러가 인식하는 새 어노테이션을 추가하는 것과 다를바 없습니다).

어노테이션을 매개변수로 갖는 것은 더 발견하기 쉽지만 몇가지 단점을 갖고 있습니다: varargs를 사용할 수 없습니다.

Option 2: 결합

``` kotlin
@Target(CLASS, FIELD) @annotation(retention = SOURCE, repeatable = false, documented = false) class example
```

타겟이 vararg를 인자로 가질수 있다는 의의가 있고, 그 외의 것들(보존, 반복, 문서화, 상속)은 하나의 주석으로 결합됩니다.

중요한 질문이 하나 있습니다: 어노테이션이 어노테이션일 경우 어노테이션은 무엇입니까? 다음과 같은 답이 가능합니다:
* 해석에 관계없이 어노테이션된 모든것
* kotlin.annotation.annotation 과 (정확히 해석된) kotlin.annotation.annotation로 어노테이션 된 모든것

두번째 답이 더 정확해 보입니다.

## Targeting

적용 가능성을 확인하기 위해 다음 상수들을 사용할 수 있습니다:

| Kotlin constant | Java constant |
|-----------------|---------------|
| CLASS | TYPE |
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

지정된 대상에서 허용되지 않는 요소에 어노테이션을 넣는 것은 컴파일 타임 에러입니다. 지정된 대상이 없으면 Java6에 있는 모든 대상이 허용됩니다.(TYPE_PARAMETER, TYPE, EXPRESSION, FILE을 제외한).

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

When we compile a Kotlin class to Java, we write a `@java.lang.annotation.Target` annotation that reflects the targets. For targets having no correspondent ones in Java (e.g. `EXPRESSION`) nothing is written to `j.l.a.Target`. If the set of Java targets is empty, `j.l.a.Target` is not written to the class file.

In addition to `java.lang.annotation.Target`, a Kotlin-specific annotation `kotlin.annotation.Target` is written containing all the Kotlin targets listed:

``` kotlin
package kotlin.annotation

enum class AnnotationTarget {
    CLASS,
    ANNOTATION_CLASS,
    ...
}

@Target(ANNOTATION_CLASS)
@Retention(RUNTIME)
annotation class target(vararg allowedTargets: AnnotationTarget)
```

When loading an annotation, we only read `kotlin.annotation.Target`. When `kotlin.annotation.Target` is missing, on the JVM, we read `j.l.a.Target` and map its values to Kotlin ones according to the table above. This implies that we can load pure Java annotations that know nothing about Kotlin, and that an annotation written in Java can be targeted, e.g. for Kotlin expressions, because one can simply manually specify `kotlin.annotation.Target` for it.

### Syntax

It makes sense to use `kotlin.annotation.Target` explicitly in Kotlin code:

``` kotlin
@Target(EXPRESSION, TYPE)
annotation class MyAnn
```

> An alternative would be to make target a property of `kotlin.annotation.annotation`, but then we'd
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

We map `java.lang.annotation.Retention` and `RetentionPolicy` to `kotlin.annotation.Retention` and `kotlin.annotation.AnnotationRetention`, and `CLASS` to `BINARY`.

``` kotlin
@Target(TYPE)
@Retention(SOURCE)
annotation class MyAnn
```

The following checks must be performed at compile time:
* `EXPRESSION`-targeted annotations can only have retention `SOURCE`

## Repeatable

> Java has `Repeatable` as an annotation, but we cannot map a Kotlin type to it, because it is only present since JDK 8, and cannot be written to class files with version lower than 8.

We make `kotlin.annotation.Repeatable` a separate annotation which makes annotation repeatable if presents.

If a non-repeatable annotation is used multiple times on the same element, it is a compile-time error.

If a repeatable annotation with binary or runtime retention is used multiple times on the same element, but the target byte code version is lower than Java 8, it is a compile-time error.

A repeatable annotation with source retention may be used multiple times on any platform. A repeatable annotation with any retention may be used multiple times on a non-JVM platform.

## Documented

We make `kotlin.annotation.MustBeDocumented` a separate annotation. This annotation is mapped to the same platform-specific annotation, if any (e.g. j.l.a.Documented).

## Inherited

This one is of rather unclear value, and we do not support it in Kotlin. One can use platform-specific annotation to express it.

