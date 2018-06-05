## 표준 라이브러리를 위한 코드 생성

표준 라이브러리의 일부 코드는 템플릿을 기반으로 해서 생성된 코드입니다.
예를 들어, 많은 `Array` 메서드는 `Array<T>`, `ByteArray`, `ShortArray`, `IntArray`, 등과 개별적으로 구현되어야 합니다. 

코드 생성기를 실행하려면 다음 명령어를 프로젝트의 최상위 디렉토리에서 실행합니다:

    ./gradlew :tools:kotlin-stdlib-gen:run

> 참고: Windows에서는 맨 앞의 `./` 대신 `gradlew`를 입력하세요.

이 명령어는 특별한 Kotlin 기반 DSL로 만들어진 [템플릿](src/templates)에서 표준 라이브러리 소스의 중요한 부분을 생성하는 스크립트를 실행합니다.
