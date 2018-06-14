# Enums

목표:
* enum 상수를 정의 할 때 생성자 매개변수를 위한 문법 개선 
* enum 상수의 어노테이션 문법 문제 해결

## Example

Simple enum:
``` kotlin
enum class Foo {
    A
    B
    C {
        override fun foo() { ... }
    }
  
    open fun foo() {}
}
```

Enum with constructor:
``` kotlin
enum class Foo(val s: String) {
    A("a")
    B("b")
    C("c") {
        override fun foo() { ... }
    }
  
    open fun foo() {}
}
```

Issues
* Enum 구문과 어노테이션 구문 충돌
    * Option 1.1: enum 내부의 짧은 어노테이션 구문 금지. **단점**: enum 내에서 functions/properties/classes 어노테이션 불가
    * Option 1.2: enum 상수와 멤버 구분자 추가 및 enum 자체의 어노테이션 구문 금지. **단점**: 비록 오류메세지는 다소 명확하고 유익할지라도 구분자는 직관적이지 않고 처음에 생각하기 어려움
    * Option 1.3: 각 항목에 soft-keyword로 접두사 달기 e.g. `entry`. **단점**: 코드가 장황해짐
    * Option 1.4 **선택**: 마지막 enum 상수 뒤에 ; 구분자 추가 **및** 다른 enum 상수 사이에 , 구분자 추가 
* 상수에 다른 supertype을 지정하는 방법 (있는 경우)
    * Option 2.1 **선택**: 용례가 매우 적은 것들을 놔두고, Java도 지원하지 그것들을 지원하지 않음
    * Option 2.2: `A("s"): OtherType`

option 1.4 예시:

``` kotlin
enum class Foo(val s: String) {
    A("a"), // semicolon CAN NOT be used here!
    B("b"), // comma is MANDATORY after each enum constant except the last one
    C("c") {
        override fun foo() { ... }
    }; // semicolon is MANDATORY here, even if no member follows
  
    open fun foo() {}
}
```

Notes:
* 멤버가 없는 일반적인 경우에는 오버헤드가 거의 없음: 
    * `enum class E {A, B, C; }`
* 분명한 에러 메세지: 구분자가 마지막 상수 뒤에 ;를 찾지 못한 경우:
    * 에러 메세지 보고: "There must be a semicolon after the last enum entry"
    * 대부분의 경우 quick fix 또한 ;의 올바른 위치를 추측할 수 있음.
* 오늘날, enum 요소를 `public` 이나 제어자로 사용되는 다른 soft-keyword로 지정할 수 없습니다.