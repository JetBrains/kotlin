# Support for JVM's ACC_NATIVE flag

목표 : JNI interop를 Kotlin에서 사용할 수 있게 하고, Java의 JNI로 동작하는 것들을 Kotlin에서 1대1로 재생산 할 수 있게 합니다.
Goal: enable JNI interop for Kotlin, so that anything that works through JNI in Java could be reproduced 1-to-1 in Kotlin

## 문법

라이브러리에 다음의 정의가 있습니다:

``` kotlin
package kotlin.jvm

Retention(RetentionPolicy.SOURCE)
public annotation class native
```

이 어노테이션은 다음에 사용 가능합니다.
 - 함수
 - 속성 접근자

이 어노테이션으로 마크된 정의를 *native declaration( 네이티브 선언 )* 이라고 합니다.

점검사항:
 - 추상화할 수 없다.
 - 함수 바디를 가질 수 없다.
 - `inline` 을 사용할 수 없다.
 - `reified` 형 매개변수를 사용할 수 없다. 
   노트 : 이것은 `inline` 을 금지하므로써 실현됩니다. `reified` 는 인라인 함수에서만 허용됩니다.
 - 멤버는 네이티브일 수 없다.

노트: 네이티브 멤버는 상위유형의 열린 또는 추상화 멤버를 오버라이드 할 수 있습니다.

## Semantics on the JVM

Intuition: 소스 선언이 네이티브인 JVM 메소드는 `ACC_NATIVE`로 표시되고 `CODE` 속성을 갖지 않습니다.

### \[platformStatic\] 상호작용

`native` 와 `platformStatic` 로 마크된 오브젝트의 네이티브 멤버는 직접 번역됩니다: 이것에 상응하는 JVM 메소드는 하나밖에 없고, `ACC_NATIVE` 로 마크됩니다.


A member of a companion object of class `C` marked `native` 와 `platformStatic` 로 마크된 클래스 `C` 의 companion object의 멤버는 두가지 JVM 메소드를 만듭니다:
 - `ACC_NATIVE`로 마크된 `C` 의 정적 멤버;
 - `ACC_NATIVE`로 마크되지 않은 `C$object` 의 인스턴스 멤버 그리고 바디는 네이티브 정적 메소드로 옮깁니다.

### 최상위 레벨 선언

패키지 `p` 의 네이티브 멤버는 하나의 JV 메소드를 만듭니다:
 - `ACC_NATIVE` 플래그로 마크된 패키지 퍼사드 클래스 `PPackage` 의 멤버.

### 네이티브 속성 접근자 (native property accessor)

속성은 `native` 로 마크될 수 없습니다.
바디가 없다면 *속성 접근자* 는 `native` 로 마크될 수 있습니다 (i.e. it is a *default accessor*).
이런 경우, 생성된 코드는 속성과 동일한 컨텍스트 내에 정의된 네이티브 함수와 같습니다.

Example:
``` kotlin
val foo: Int
  [native] get
```

## Not implemented (yet)

- 네이티브 속성 접근자
 - 프론트엔드: 접근자가 디폴트, 네이티브일때, 이니셜라이저를 요구하지 않고 backing field를 허용하지 않습니다.
 - 백엔드: 접근자가 디폴트, 네이티브일때, backing field를 만들지 않습니다.
- 적용가능성 점검: 함수 및 속성 접근자