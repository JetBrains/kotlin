## 코틀린 표준 라이브러리

이 모듈은 [코틀린 표준 라이브러리](http://kotlinlang.org/api/latest/jvm/stdlib/index.html)를 생성해줍니다.

### 참고

우리는 배열, 문자열,`Collection <T>`, `Sequence <T>`, `Map <K, V>`등과 같은 다양한 콜렉션 유형을 위한 다양한 유틸리티 확장 함수를 생성하기 위해 몇가지 코드 생성 요소를 사용합니다.

이 소스는 `generated` 폴더에 저장되며 그 이름 앞에 밑줄이 붙습니다 (예 :`generated / _Collections.kt`).

코드 생성기를 실행하려면 프로젝트의 루트 디렉토리에서 다음 명령을 사용하십시오.

     ./gradlew : tools : kotlin-stdlib-gen : run

>주의 : Windows에서는`./`을 사용하지 않고 `gradlew`를 입력하십시오

그런 다음 특별한 kotlin 기반 DSL로 저작 된 [templates] (../ tools / kotlin-stdlib-gen / src / templates)에서 stdlib 소스의 상당 부분을 생성하는 스크립트를 실행합니다.

### 사용 예시

표준 라이브러리의 샘플을 제작하려면 [샘플 readme] (samples / ReadMe.md)로 이동하십시오.
