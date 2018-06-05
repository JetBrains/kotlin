## 표준 라이브러리 예시

이 프로젝트는 표준 라이브러리 함수들의 예시를 포함합니다. 
이들은 [테스트](test) 소스 최상위(root)에 위치하며, 각 예제는 소규모 단위 테스트와 비슷하게 짜여있습니다.
그리고 이 예제들은 [`@sample`](http://kotlinlang.org/docs/reference/kotlin-doc.html#block-tags) 태그로 표준 라이브러리 함수 문서에서 참조되며, 생성된 문서에 실행가능한 예제로써 내장됩니다. 


### 예제 제작 가이드

단위 테스트로 작성되었더라도, 샘플은 단위 테스트와 개념적으로 같지 않다는 것을 참고하시기 바랍니다. 
주로 (테스트되는 함수의) 취약 상황을 확인하는 단위 테스트와 다르게, 예제의 목적은 함수의 일반적인 목적에 맞는 상황을 확인하는 것입니다.

Please see the existing samples for an inspiration on authoring new ones.

- Do not add `Test` postfix or prefix to the name of a sample container class or sample method.

- There's no hard restriction that each function should get its own sample. Several closely related functions can be illustrated with one sample, 
for example one sample can show the usage of `nullsFirst` and `nullsLast` comparators.
  
- For the functions that are generated from a template (usually they are placed in the `generated` directory) the sample reference should be placed
in the template, and then all specializations should be regenerated. See [the standard library generator](https://github.com/JetBrains/kotlin/tree/master/libraries/tools/kotlin-stdlib-gen) for details.
 
- It's possible to provide a single sample for all primitive specializations of a function in case if its usage doesn't change significantly
depending on the specialization. 

- Each sample should be self contained, but you can introduce local classes and functions in it.
Do not use external references, other than the Standard Library itself and JDK.

- Use only the following subset of assertions:

    - `assertPrints` to show any printable value,
    - `assertTrue`/`assertFalse` to show a boolean value,
    - `assertFails` / `assertFailsWith` to show that some invocation will fail.
  
  When a sample is compiled and run during the build these assertions work as usual test assertions.
  When the sample is transformed to be embedded in docs, these assertions are either replaced with `println` with the comment showing its 
  expected output, or commented out with `//` — this is used for `assertFails` / `assertFailsWith` to prevent execution of its failing block 
  of code. 
