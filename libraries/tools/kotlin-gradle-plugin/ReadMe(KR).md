## Gradle 플러그인

Gradle 플러그인 소스는 이 ([kotlin-gradle-plugin](./)) 모듈에서 찾을 수 있습니다.

Gradle 플러그인을 로컬 Maven 저장소에 설치하려면, Kotlin 프로젝트의 최상위에서 다음 명령어를 실행합니다:

    ./gradlew :kotlin-gradle-plugin:install
    
서브 플러그인 모듈은 `:kotlin-allopen`, `:kotlin-noarg`, `:kotlin-sam-with-receiver`입니다. 이들을 설치하려면, 다음을 실행합니다:

    ./gradlew :kotlin-allopen:install :kotlin-noarg:install :kotlin-sam-with-receiver:install

이 아티팩트에서 제공되는 플러그인과 그 역할에 대해서 더 알고싶다면, [Module.md](Module.md)를 참고하시기 바랍니다.

### Gradle 플러그인 병합 테스트

[`libraries/tools/kotlin-gradle-plugin-integration-tests`](https://github.com/JetBrains/kotlin/tree/master/libraries/tools/kotlin-gradle-plugin-integration-tests) 모듈을 확인하시길 바랍니다.
