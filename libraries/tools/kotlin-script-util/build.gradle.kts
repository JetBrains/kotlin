
description = "Kotlin scripting support utilities"

apply { plugin("kotlin") }

dependencies {
    compile(project(":kotlin-stdlib"))
    compile(project(":kotlin-script-runtime"))
    compile(projectRuntimeJar(":kotlin-compiler"))
    compile(projectRuntimeJar(":kotlin-daemon-client"))
    compileOnly("com.jcabi:jcabi-aether:0.10.1")
    compileOnly("org.sonatype.aether:aether-api:1.13.1")
    compileOnly("org.apache.maven:maven-core:3.0.3")
    testCompile(project(":kotlin-test:kotlin-test-junit"))
    testRuntime(project(":kotlin-reflect"))
    testRuntime("com.jcabi:jcabi-aether:0.10.1")
    testRuntime("org.sonatype.aether:aether-api:1.13.1")
    testRuntime("org.apache.maven:maven-core:3.0.3")
}

projectTest()

runtimeJar()
sourcesJar()
javadocJar()

publish()
