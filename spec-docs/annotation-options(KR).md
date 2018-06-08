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

> NOTE: Java는 다음의 것을 타겟으로 사용합니다 [targets](https://docs.oracle.com/javase/8/docs/api/java/lang/annotation/ElementType.html)
기본적으로 Java는 무엇을 디폴트로 사용해야 하는지 불명확하게 만드는 Java8 특정 타겟을 제외한 모든 것을 가집니다.


나중에 타겟을 추가하는 문제를 해결하는 한 방법: 명시적인 `ALL` 타겟 사용. 그러나 Java와 매칭시키는 문제가 있습니다.

`TYPE` 의 경우, 아래의 옵션과 함께 `@typetarget` 어노테이션을 추가하는게 좋습니다:
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

또한 외부 타입에 사용되는 것과 같은 이색적인 용법이 있습니다: `@A (@B Outer).Inner`, `@A` 는 `Inner` 에 속하고, `@B` 는 `Outer` 에 속합니다.

**TODO** Open question: traits/classes/objects 는 어떠한가?
**TODO** 지역 변수들은 속성과 같습니다. 그러나


> 플랫폼 특정 타겟이 가능합니다
* SINGLETON_FIELD for objects
* PROPERTY_FIELD
* (?) DEFAULT_FUNCTION
* (?) LAMBDA_METHOD
* PACKAGE_FACADE
* PACKAGE_PART

### Java로 매핑

Kotlin은 Java보다 사용가능한 타겟이 많아서 매핑 문제가 발생합니다. 위의 표는 Kotlin과 Java의 대응관계를 보여줍니다.

Kotlin 클래스를 Java로 컴파일할때, 타겟을 반영하는 어노테이션 `@java.lang.annotation.Target` 을 씁니다. Java에 없는 타겟에는 (e.g. `EXPRESSION` ) `j.l.a.Target` 에 아무것도 쓰이지 않습니다. 만약 Java 타겟 집합이 비어있다면, `j.l.a.Target`는 클래스 파일로 작성되지 않습니다.

`java.lang.annotation.Target` 외에도 Kotlin 특유의 어노테이션 `kotlin.annotation.Target`은 나열된 모든 Kotlin 타겟을 포함하여 작성됩니다:

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

어노테이션을 불러올 때, `kotlin.annotation.Target` 만 읽습니다. `kotlin.annotation.Target` 가 없으면 JVM에서 `j.l.a.Target`를 읽고 위의 테이블대로 Kotlin에 매핑합니다. 이는 Kotlin을 전혀 모르는 순수 Java 어노테이션을 불러올 수 있고 Java에서 작성된 어노테이션도 대상으로 할 수 있습니다. Kotlin 표현에 대해서는 `kotlin.annotation.Target`을 수동으로 지정할 수 있기 때문입니다. 

### 문법

Kotlin 코드에서 `kotlin.annotation.Target`을 명시적으로 사용하는 것이 합리적입니다:

``` kotlin
@Target(EXPRESSION, TYPE)
annotation class MyAnn
```

> 타겟을 `kotlin.annotation.annotation`의 속성으로 만드는 것은 하나의 대안이 될 수 있지만,
* 더 많은 선택적 매개변수 때문에 varargs의 장점을 잃을 것입니다.
* Java와 다른 형태가 될 것이므로, Kotlin 친화적으로 Java 어노테이션을 만드는 법을 이해하기 어려워 질 것입니다.

## Retention (범위)

> NOTE: Retention은 Java와 약간 차이가 있습니다. CLR과 JS는 런타임에 모든 attribute를 유지합니다.

`RUNTIME`이 기본 retention인 것은 매우 합리적입니다.

`RetentionPolicy.CLASS`는 어떤 클래스 외부의 함수를 갖고 있는 Kotlin에 잘 어울리지 않기 때문에, `BINARY`를 대신 쓰는게 더 좋습니다. 또한 플랫폼에 따라 다르므로 결국 `java.lang.annotation.RetentionPolicy` 는 사용할 수 없습니다. 따라서 우리만의 enum을 가져야 합니다.


``` kotlin
package kotlin.annotation

enum class AnnotationRetention {
    SOURCE
    BINARY
    RUNTIME
}
```

우리는  `java.lang.annotation.Retention` 과 `RetentionPolicy` 를 `kotlin.annotation.Retention` 과 `kotlin.annotation.AnnotationRetention`로, `CLASS` 을 `BINARY`로 매핑했습니다.

``` kotlin
@Target(TYPE)
@Retention(SOURCE)
annotation class MyAnn
```

다음 검사는 반드시 컴파일 타임에 수행되어야 합니다:
* `EXPRESSION`-targeted annotations can only have retention `SOURCE`

## Repeatable (반복 선언)

> 자바는 어노테이션에 `Repeatable` 가 있지만 JDK 8부터 나오고 이전 버전의 클래스 파일에 쓰일 수 없기 때문에 Kotlin 타입을 이것에 매핑할 수 없습니다.

`kotlin.annotation.Repeatable`를 별도의 어노테이션으로 만들어 어노테이션이 그러하다면 반복될 수 있게 만들었습니다.

만약 반복 불가능한 어노테이션이 같은 요소에서 여러번 사용된다면, 컴파일 에러를 일으킵니다.

만약 Java 8 미만 버전에서  바이너리 및 런타임 retention의 반복가능한 어노테이션이 같은 요소에서 여러번 사용된다면, 컴파일 에러를 일으킵니다.

소스 retention의 반복가능한 어노테이션은 어떤 플랫폼에서라도 여러번 사용될 수 있습니다. 임의의 retention의 반복가능한 어노테이션은 non-JVM 플랫폼에서 여러번 사용될 수 있습니다.

## Documented (문서화)

`kotlin.annotation.MustBeDocumented`를 별도의 어노테이션으로 만들었습니다. 이 어노테이션은 동일한 플랫폼 별 어노테이션으로 매핑됩니다 (e.g. j.l.a.Documented).

## Inherited (상속)

다소 불명확하며 Kotlin에서는 지원하지 않습니다. 특정 플랫폼 어노테이션을 사용해 이것을 표현할 수 있습니다.

