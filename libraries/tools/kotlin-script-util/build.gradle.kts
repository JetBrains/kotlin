
description = "Kotlin scripting support utilities"

apply { plugin("kotlin") }

dependencies {
    compile(projectDist(":kotlin-stdlib"))
    compile(projectDist(":kotlin-script-runtime"))
    compile(projectRuntimeJar(":kotlin-compiler"))
    compile(projectRuntimeJar(":kotlin-daemon-client"))
    compileOnly("com.jcabi:jcabi-aether:0.10.1")
    compileOnly("org.sonatype.aether:aether-api:1.13.1")
    compileOnly("org.apache.maven:maven-core:3.0.3")
    testCompile(projectDist(":kotlin-test:kotlin-test-junit"))
    testRuntime(projectDist(":kotlin-reflect"))
    testCompile(commonDep("junit:junit"))
    testRuntime("com.jcabi:jcabi-aether:0.10.1")
    testRuntime("org.sonatype.aether:aether-api:1.13.1")
    testRuntime("org.apache.maven:maven-core:3.0.3")
}

projectTest()

runtimeJar()
sourcesJar()
javadocJar()

publish()
