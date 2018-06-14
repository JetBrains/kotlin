## Kotlin 라이브러리

프로젝트의 이 부분에는 다음 라이브러리의 소스를 포함하고 있습니다.:

  - [kotlin-stdlib](stdlib), Kotlin/JVM을 위한 기본 라이브러리, JDK 7와 JDK 8을 위한 추가적인 파트들과 Kotlin/JS입니다.
  - [kotlin-reflect](reflect), the library for full reflection support
  - [kotlin-test](kotlin.test), 멀티 플랫폼 유닛 테스팅을 위한 라이브러리 입니다.
  - [kotlin-annotations-jvm](tools/kotlin-annotations-jvm), 자바 코드의 형식을 코틀린에서 사용할 때 더욱 좋게 보이기 위한 주석입니다. 
  
<!--  - [kotlin-annotations-android](tools/kotlin-annotations-android) -->

이 라이브러리들은 [root](../) Gradle 프로젝트의 일부로 build 되었습니다.


## Kotlin Maven Tools

<!-- TODO: Move to another root -->

프로젝트의 이 부분은 Maven build를 위한 root입니다. 

당신은 이 [root IDEA project](../ReadMe.md#working-in-idea)의 IDEA를 사용한 maven 프로젝트를 maven 모듈을 통해 사용할 수 있습니다. Import후 당신은 maven 프로젝트들을 탐색하고 오른쪽 사이드바의 도구로 IDEA에서 바로 실행할 수 있습니다.

### Building

최신의(적어도 3.3) [Maven](http://maven.apache.org/) 배포판 설치가 필요합니다. 

이 Maven 프로젝트를 build하기 전에, 당신은 root 프로젝트에서 다음의 명령를 통해 Gradle로 빌드된 필수 아티팩트를 로컬 Maven 저장소에 빌드하고 설치해야 합니다.

    ./gradlew install

> Note: Windows 환경에서는 `./`없이 `gradlew`를 입력해야 합니다.

이 명령은 아티팩트들을 합쳐 후속 maven 빌드에서 사용될 로컬 maven 저장소에 삽입합니다.
[root ReadMe.md, section "Building"](../ReadMe.md#building)에서 추가정보를 확인하세요.

    
그후 Maven으로 maven 아티팩트들을 빌드할 수 있습니다.:

    mvn install

만약 당신의 maven 빌드가 Out-Of-Memory 에러로 실패한다면, maven을 위한 JVM설정에서 `MAVEN_OPTS`환경변수를 다음과 같이 바꾸세요.:

    MAVEN_OPTS="-Xmx2G"

