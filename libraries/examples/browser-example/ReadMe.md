## Sample Application

This (really simple ;) application shows how to use Kotlin and the maven plugin to generate JavaScript and invoke it from inside a HTML web page.

The source [Hello.kt](https://github.com/JetBrains/kotlin/blob/master/libraries/examples/browser-example/src/main/kotlin/sample/Hello.kt) uses the [kotlin.browser](https://jetbrains.github.com/kotlin/versions/snapshot/apidocs/kotlin/browser/package-summary.html) API to access the *document* property to modify the HTML.

### Running the sample in a web browser

To run the example try:

    cd libraries/examples/browser-example
    mvn install
    open sample.html

This should open a browser which then shows some simple HTML which then includes some dynamically generated content.

## Running the sample on Java 7 with JavaFX and [kool.io](https://kool.io/)'s browser

You can also run the sample as Java code on a JVM using JavaFX (which includes its own webkit rendering engine for HTML / CSS / JS support) using the [kool.io JavaFX browser](https://github.com/koolio/kool/blob/master/samples/kool-template-sample/ReadMe.md).

First you need to install [Java 7 update 4](https://www.oracle.com/technetwork/java/javase/overview/index.html) or later which ships with JavaFX.

You will need to setup **JAVA_HOME** and **PATH** environment variables to point to the latest JDK. If you install Java 7 and use a Mac you might want to run this first...

    export JAVA_HOME=/Library/Java/JavaVirtualMachines/1.7.0.jdk/Contents/Home
    export PATH=$JAVA_HOME/bin:$PATH

You can check you have JavaFX in your JDK install via

    ls -l $JAVA_HOME/jre/lib/jfxrt.jar

which should find the JavaFX runtime jar (jfxrt.jar).

### Running the sample in JavaFX

To run the sample try...

    mvn test -Pjavafx

Assuming you've Java 7 enabled and JAVA_HOME points to the JRE/JDK for Java 7 or later which includes JavaFX.

This should popup a JVM process with an embedded webkit based browser running the application; using the compiled bytecode on the JVM rather than JavaScript.