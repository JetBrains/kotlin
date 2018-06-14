## Motivation

#### 로컬은 가장 높은 우선순위를 가짐

함수 타입의 변수는 멤버보다 우선합니다:
 
```
class A { fun foo() = 1 }

fun test(a: A, foo: () -> Int) {
    with (a) {
        foo()
    }
}
```

익명 오브젝트에서 멤버가 아닌 로컬 변수가 선택됩니다:

```
interface A {
    val foo: Int
}

fun createA(foo: Int) = object : A {
    override val foo = foo
}
```

#### 최상위 체인 스코프 (Top-level chain scope)
 
우선 순위: 명시적 임포트; 같은 패키지 내 함수; star-import; stdlib 함수.

명시적 임포트는 `*` 로 임포트된 디스크립터를 숨겨야 합니다.

같은 패키지에서 다른 파일로 함수를 옮기는 것이 해석(resolution)을 바꿔서는 안되기 때문에 파일의 스코프는 없습니다.

명시적으로 임포트된 함수는 같은 패키지의 함수보다 우선합니다; 후자는 다른 파일에 있을 수 있습니다. 

#### 암시적 수신자의 순서 ( The order of implicit receivers )

See the discussion here: https://youtrack.jetbrains.com/issue/KT-10510.

## Technical notes

`foo()` 를 호출하기 위해 속성 `foo` 를 해석할 때, 첫번째 속성에서 멈추지 않고 `foo` 라는 이름의 모든 변수를 수집하고 각각에서 `invoke` 함수를 찾으려고 합니다:
 
``` 
class A {
    val foo: () -> Unit = { println("Hello world!") }
}

fun test(foo: Int) {
    with(A()) {
        foo   // parameter
        foo() // property + invoke
    }
}
```