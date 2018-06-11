# 구체화된 타입 매개변수 (Reified Type Parameters)

목표: 함수에 전달된 타입에 대한 런타임 액세스를 구체화(reified) 한 것처럼 지원(현재 인라인 함수에만 제한됨). 

## Syntax

함수의 타입 매개변수는 `reified`로 마크될 수 있습니다: 

``` kotlin

inline fun foo<reified T>() {}
```

## Semantics, Checks and Restrictions

**정의** 잘 만들어진 타입은 *runtime-available* 이라고 합니다. 만약
- 타입 파라미터가 없거나, 모든 타입 파라미터가 `Nothing`으로 예외처리되고 `reified`된 분류자(classifier (오브젝트, 클래스, 트레이트))인 형태 `C` 를 가지거나,
- `n` 가지 타입 파라미터의 각각 `Ti`마다 최소한 아래 조건 한가지 이상을 만족 시키는 분류자(classifier)인 형태 `G<A1, ..., An>`를 가지거나:
    - `Ti` 는 `reified` 타입 파라미터이고 상응하는 타입 인자 `A1`은 runtime-available 이거나,
    - `Ai` 은 *star-projection* (e.g. for `List<*>`, `A1` is a star-projection);
- `reified` 타입 파라미터 `T` 형태를 가지거나.

Examples:
- Runtime-available types: `String`, `Array<String>`, `List<*>`;
- Non-runtime-available types: `Nothing`, `List<String>`, `List<T>` (for any `T`)
- Conditional: `T` is runtime-available iff the type parameter `T` is `reified`, same for `Array<T>`

runtime-available 타입들만 허용됩니다.
- `is`, `!is`, `as`, `as?` 의 우변으로
- *호출의* `reified` 타입 파라미터의 인자 (타입에 대해 어떤 인자라도 허용됩니다, 예를 들어, `Array<List<String>>` 또한 여전히 유효합니다).

결과적으로, `T`가 `reified` 타입 매개변수라면, 다음과 같은 구성이 허용됩니다:
- `x is T`, `x !is T`
- `x as T`, `x as? T`
- `T`: `javaClass<T>()`, `T::class` 에서의 반영 접근 (reflection access) (지원한다면)

구체화된(reified) 타입 매개변수에 대한 제한:
- 오직 `inline` 함수의 타입 매개변수만  `reified`로 마크될 수 있습니다.
- 빌트-인 클래스 `Array`만 `reified`로 마크될 수 있습니다. 다른 클래스들은 `reified`로 선언되는 것이 허용되지 않습니다.
- runtime-available 타입만이 `reified` 타입 매개변수에 인자로 전달될 수 있습니다.

Notes:
- 함수 형의 인라인 가능 매개변수를 선언하지 않고 `reified` 타입 매개변수가 선언 된 `inline` 함수에 대해서는 경고가 표시되지 않습니다.

## Implementation notes for the JVM

인라인 함수에서 `reified` 타입 매개변수`T`의 발생은 실제 타입 인자로 대체됩니다.
실제 타입 인자가 기본 타입 (primitive) 인 경우 래퍼가 구체화 된 바이트 코드 내에서 사용됩니다.

``` kotlin
open class TypeLiteral<T> {
    val type: Type
        get() = (javaClass.getGenericSuperclass() as ParameterizedType).getActualTypeArguments()[0]
}

inline fun <reified T> typeLiteral(): TypeLiteral<T> = object : TypeLiteral<T>() {} // here T is replaced with the actual type

typeLiteral<String>().type // returns 'class java.lang.String'
typeLiteral<Int>().type // returns 'class java.lang.Integer'
typeLiteral<Array<String>>().type // returns '[Ljava.lang.String;'
typeLiteral<List<*>>().type // returns 'java.util.List<?>'
```
