# 어노테이션 인자
**NOTE**: 이 문서는 오래된 언어 디자인 노트를 포함하고 있으며 최신 코틀린과 상응하지 않습니다. 이 주제의 최신 문제를 보고 싶으면 http://kotlinlang.org/docs/reference/annotations.html 를 방문해주십시오.

***

목표:
* 어노테이션에서 위치매개변수와 varargs의 문제점 나열
* \[TBD later] 어노테이션에서 배열 인자를 위한 더 나은 문법

관련 이슈:
* [KT-6652 Prohibit using java annotations with positional arguments](https://youtrack.jetbrains.com/issue/KT-6652)
* [KT-6220 Annotations: handling of "value" members](https://youtrack.jetbrains.com/issue/KT-6220)
* [KT-6641 Annotations with multiple array values](https://youtrack.jetbrains.com/issue/KT-6641)
* [KT-2576 Shortcut notation for annotations with single-value array elements](https://youtrack.jetbrains.com/issue/KT-2576)

## 문제 설명

자바에서 어노테이션 요소는 ( 어노테이션의 "field"/"attributes"/"properties" ) `@interface`에 상응하는 메소드로 정의됩니다. 따라서 자바 어노테이션에 대해 가상의 기본 생성자를 로드할 때 사용할 수 있는 순서규칙이 없습니다.

Example:

두가지 요소가 있는 자바 어노테이션:

``` java
@interface Ann {
    int foo();
    String bar();
}
```

코틀린에서 이것을 사용할때, 위치 인자를 사용할 수 있습니다:

``` kotlin
[Ann(10, "asd")]
class Baz
```

이제 자바 인터페이스에서 메소드를 재정렬하기 위해 소스 및 바이너리 호환이 가능합니다.

``` java
@interface Ann {
    String bar();
    int foo();
}
```

하지만 위의 코드는 동작하지 않습니다.

또한, 모든 배열 인자를 varargs로 로드하므로 같은 이유로 동작하지 않을 수 있습니다.

## 자바 어노테이션 로드

자바 어노테이션을 위한 가상의 생성자는 다음과 같이 빌드될 수 있습니다:
* 만약 `value`라는 이름의 요소가 있다면, 인자의 매개변수 리스트의 첫번째에 놓입니다.
* 만약 다른 모든 요소가 초기값을 가지고 있고 `value`가 배열이라면 가지고 있다면, `vararg`로 마크되고 배열요소의 타입을 가집니다.
* `value`를 제외한 모든 요소에 해당하는 매개변수는 위치를 사용할 수 없고, 오직 이름을 가진 인자만 가능합니다.( 이 경우 플랫폼 특정 검사를 `frontend.java`에 추가해야 합니다 )
* 초기값이 있는 요소는 초기값이 있는 매개변수로 변환해야 합니다.

>**NOTE**: `value` 매개변수가 `vararg`로 마크되고 인자가 전달되지 않으면, 동작은 매개변수의 초기값의 존재에 따라 결정됩니다:
* 초기값이 없으면 빈 배열이 바이트코드로 생성됩니다.
* 초기값이 존재하면 바이트 코드로 생성되는 값은 없고 초기값이 그대로 사용됩니다.

> 따라서, **같은 코드라도 초기값을 추가하고 소스를 재컴파일 했을때 동작은 바뀔 수 있습니다.**

## \[TBD later] 배열 문법 예시

**NOTE**: 스칼라는 아무리 엉망이라도 어노테이션에 여전히 `Array(...)`를 사용합니다.

Option 1: 배열 문자에 `[]` 사용

``` kotlin
@User(
  firstName = "John",
  names = ["Marie", "Spencer"],
  lastName = "Doe"
)
class JohnDoe

@Values([FOO, BAR]) // 엉망이지만, 다음의 자바 구문과 동일: @Ann({FOO, BAR})
class WithValues
```

Option 2: `@(...)` 사용

``` kotlin
@User(
  firstName = "John",
  names = @("Marie", "Spencer"),
  lastName = "Doe"
)
class JohnDoe

@Values(@(FOO, BAR)) // looks bad
class WithValues
```
