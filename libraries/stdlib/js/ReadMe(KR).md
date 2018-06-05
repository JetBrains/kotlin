## JS를 위한 Kotlin 표준 라이브러리

이 모듈은 런타임을 위해 필요한 모든 Kotlin 소스 코드와 JavaScript로 컴파일 된 표준 Kotlin 라이브러리 코드(definition와 implementation 코드)를 포함하는 `kotlin-stdlib-js` jar를 선보입니다.

이 모듈은 아무 브라우저에서 이 디렉토리의 **web/index.html** 파일을 실행하여 ([Qunit](http://qunitjs.com/)을 통해) 테스트 케이스를 실행하거나 디버그할 수 있습니다.

이 테스트를 시도하기 전에 `karmaDependencies` gradle 태스크를 실행하여 필요한 Qunit 의존성(dependency)를 fetch 해야 합니다.

CI 빌드 중에도 PhantomJS 브라우저에서 [gradle karma plugin](https://github.com/craigburke/karma-gradle)을 통해 이 테스트들을 할 수 있습니다.