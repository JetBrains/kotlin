## 예제 애플리케이션

이 (정말 쉬운) 애플리케이션은 Kotlin과 maven 플러그인을 사용하여 JavaScript를 생성하고 HTML 웹 페이지 내에서 불러내는(invoke) 방법을 보여줍니다.

[Hello.kt](https://github.com/JetBrains/kotlin/blob/master/libraries/examples/browser-example/src/main/kotlin/sample/Hello.kt)소스 파일은 HTML을 수정하기 위해  *document* property에 접근하려고 [kotlin.browser](http://jetbrains.github.com/kotlin/versions/snapshot/apidocs/kotlin/browser/package-summary.html) API를 사용합니다.

### 웹 브라우저에서 예제 실행하기

한 번 예제를 실행해보려면 다음과 같이 하시길 바랍니다:

    cd libraries/examples/browser-example
    mvn install
    open sample.html

이제 무작위로 생성된 컨텐츠를 포함하는 간단한 HTML을 보여주는 브라우저가 열릴 것입니다.

## JavaFX 와 [kool.io](http://kool.io/)의 브라우저를 이용해 예제를 JAVA 7으로 실행하기

예제를 JVM에서 JavaFX(HTML / CSS / JS를 지원하는 자체 렌더링 엔진 웹킷을 포함하고 있음)와 [kool.io JavaFX browser](https://github.com/koolio/kool/blob/master/samples/kool-template-sample/ReadMe.md)를 이용해서 JAVA 코드처럼 실행할 수 있습니다.

먼저, [Java 7 update 4](http://www.oracle.com/technetwork/java/javase/overview/index.html) 또는 그 이상의 JavaFX와 함께 제공되는 최신 버전을 설치합니다.

**JAVA_HOME** 과 **PATH** 환경변수들이 최신 JDK를 가리키도록 설정해야 합니다. 만약 Java 7를 설치하고 Mac을 사용한다면 이것을 먼저 해야할 것입니다...

    export JAVA_HOME=/Library/Java/JavaVirtualMachines/1.7.0.jdk/Contents/Home
    export PATH=$JAVA_HOME/bin:$PATH

다음을 실행하여 설치된 JDK에 JavaFX가 있는지 확인할 수 있습니다

    ls -l $JAVA_HOME/jre/lib/jfxrt.jar

JavaFX 런타임 jar (jfxrt.jar)를 찾아낼 것입니다.

### JavaFX로 예제 실행하기

예제를 실행하기 위해 다음을 시도합니다...

    mvn test -Pjavafx

Java 7을 활성화(enable)했고, JAVA_HOME이 JRE/JDK for Java 7(또는 JavaFX를 포함한 그 이상의 버전)의 JRE/JDK를 가리키고 있다면,

이것은 (JavaScript가 아닌 JVM 상에서 컴파일된 바이트코드(bytecode)를 사용하여) 애플리케이션을 실행하고 있는 내장된(embedded) 웹킷 기반 브라우저와 함께 JVM 프로세스를 띄울 것입니다.