# 주석 인자
**NOTE**: 이 문서는 오래된 언어 디자인 노트를 포함하고 있으며 최신 코틀린과 상응하지 않습니다. 이 주제의 최신 문제를 보고 싶으면 http://kotlinlang.org/docs/reference/annotations.html 를 방문해주십시오.

***

목표:
* Sort out problems of positional parameters and varargs in annotations
* \[TBD later] Better syntax for array arguments to annotations

관련 이슈:
* [KT-6652 Prohibit using java annotations with positional arguments](https://youtrack.jetbrains.com/issue/KT-6652)
* [KT-6220 Annotations: handling of "value" members](https://youtrack.jetbrains.com/issue/KT-6220)
* [KT-6641 Annotations with multiple array values](https://youtrack.jetbrains.com/issue/KT-6641)
* [KT-2576 Shortcut notation for annotations with single-value array elements](https://youtrack.jetbrains.com/issue/KT-2576)

## 문제 설명

자바에서 주석요소는 ( 주석의 "field"/"attributes"/"properties" ) `@interface`에 상응하는 메소드로 정의됩니다. 따라서 자바 주석에 대해 가상의 기본 생성자를 로드할 때 사용할 수 있는 순서규칙이 없습니다.

Example:

두가지 요소가 있는 자바 주석:

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

## 자바 주석 로드

자바 주석을 위한 가상의 생성자는 다음과 같이 빌드될 수 있습니다:
* if there is an element named `value`, it is put first on the parameter list
* if all other elements have default values, and `value` has an array type, it is marked `vararg` and has the type of the elements of the array
* parameters corresponding to all elements but `value` can not be used positionally, only named arguments are allowed for them (this requires adding a platform-specific check to `frontend.java`)
* note that elements with default values should be transformed to parameters with default values

>**NOTE**: when `value` parameter is marked `vararg` and no arguments are passed, behavior will depend on presence of parameter's default value:
* if it has no default value, an empty array is emitted in the byte code
* if it has a default value, then no value is emitted in the byte code, so the default value will be used

> Thus, **behavior of the same code can change after adding a default value to parameter and recompiling kotlin
sources**

## \[TBD later] Array Syntax Examples

**NOTE**: Scala still uses `Array(...)` in annotations, no matter how ugly it is

Option 1: Use `[]` for array literal

``` kotlin
@User(
  firstName = "John",
  names = ["Marie", "Spencer"],
  lastName = "Doe"
)
class JohnDoe

@Values([FOO, BAR]) // ugly, but it's the same in Java: @Ann({FOO, BAR})
class WithValues
```

Option 2: Use `@(...)`

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
