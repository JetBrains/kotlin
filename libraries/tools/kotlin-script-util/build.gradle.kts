
description = "Kotlin scripting support utilities"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(kotlinStdlib())
    compile(project(":kotlin-script-runtime"))
    compile(project(":kotlin-scripting-jvm"))
    compile(commonDep("org.jetbrains.intellij.deps", "trove4j"))
    compileOnly(project(":compiler:cli"))
    compileOnly(project(":daemon-common"))
    compileOnly(project(":kotlin-scripting-compiler"))
    compile(projectRuntimeJar(":kotlin-daemon-client"))
    compileOnly("org.jetbrains.kotlin:jcabi-aether:1.0-dev-3")
    compileOnly("org.sonatype.aether:aether-api:1.13.1")
    compileOnly("org.apache.maven:maven-core:3.0.3")
    testCompileOnly(project(":compiler:cli"))
    testCompile(project(":kotlin-test:kotlin-test-junit"))
    testRuntime(project(":kotlin-reflect"))
    testCompile(commonDep("junit:junit"))
    testCompile(project(":kotlin-scripting-compiler"))
    testRuntimeOnly(project(":kotlin-compiler"))
    testRuntime("org.jetbrains.kotlin:jcabi-aether:1.0-dev-3")
    testRuntime("org.sonatype.aether:aether-api:1.13.1")
    testRuntime("org.apache.maven:maven-core:3.0.3")
    Platform[193].orLower {
        compileOnly(intellijDep()) { includeJars("openapi") }
    }
    compileOnly(intellijDep()) { includeJars("util") }
    Platform[193].orLower {
        testCompile(intellijDep()) { includeJars("openapi") }
    }
    testCompile(intellijDep()) { includeJars("platform-api", "util") }
    testCompile(intellijCoreDep()) { includeJars("intellij-core") }

}

projectTest {
    workingDir = rootDir
}

publish()

runtimeJar()
sourcesJar()
javadocJar()
