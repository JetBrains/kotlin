
description = "Kotlin scripting support utilities"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

repositories {
    maven(url = "https://oss.sonatype.org/content/repositories/snapshots") // for jcabi-aether:1.0-SNAPSHOT
}

dependencies {
    compile(kotlinStdlib())
    compile(project(":kotlin-script-runtime"))
    compile(project(":kotlin-scripting-jvm"))
    compile(commonDep("org.jetbrains.intellij.deps", "trove4j"))
    compileOnly(project(":compiler:cli"))
    compileOnly(project(":compiler:daemon-common"))
    compileOnly(project(":kotlin-scripting-compiler"))
    compile(projectRuntimeJar(":kotlin-daemon-client"))
    compileOnly("com.jcabi:jcabi-aether:1.0-SNAPSHOT")
    compileOnly("org.sonatype.aether:aether-api:1.13.1")
    compileOnly("org.apache.maven:maven-core:3.0.3")
    testCompileOnly(project(":compiler:cli"))
    testCompile(project(":kotlin-test:kotlin-test-junit"))
    testRuntime(project(":kotlin-reflect"))
    testCompile(commonDep("junit:junit"))
    testCompile(project(":kotlin-scripting-compiler"))
    testRuntimeOnly(projectRuntimeJar(":kotlin-compiler"))
    testRuntime("com.jcabi:jcabi-aether:1.0-SNAPSHOT")
    testRuntime("org.sonatype.aether:aether-api:1.13.1")
    testRuntime("org.apache.maven:maven-core:3.0.3")
    compileOnly(intellijDep()) { includeJars("openapi", "util") }
    testCompile(intellijDep()) { includeJars("openapi", "platform-api", "util") }

}

projectTest {
    workingDir = rootDir
}

publish()

runtimeJar()
sourcesJar()
javadocJar()
