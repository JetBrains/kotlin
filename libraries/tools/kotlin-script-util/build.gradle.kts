
description = "Kotlin scripting support utilities"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(kotlinStdlib())
    api(project(":kotlin-script-runtime"))
    api(project(":kotlin-scripting-jvm"))
    api(commonDep("org.jetbrains.intellij.deps", "trove4j"))
    compileOnly(project(":compiler:cli"))
    compileOnly(project(":daemon-common"))
    compileOnly(project(":kotlin-scripting-compiler"))
    api(project(":kotlin-daemon-client"))
    compileOnly("org.jetbrains.kotlin:jcabi-aether:1.0-dev-3")
    compileOnly("org.sonatype.aether:aether-api:1.13.1")
    compileOnly("org.apache.maven:maven-core:3.0.3")
    testCompileOnly(project(":compiler:cli"))
    testApi(project(":kotlin-test:kotlin-test-junit"))
    testImplementation(project(":kotlin-reflect"))
    testApi(commonDep("junit:junit"))
    testApi(project(":kotlin-scripting-compiler"))
    testRuntimeOnly(project(":kotlin-compiler"))
    testImplementation("org.jetbrains.kotlin:jcabi-aether:1.0-dev-3")
    testImplementation("org.sonatype.aether:aether-api:1.13.1")
    testImplementation("org.apache.maven:maven-core:3.0.3")
    compileOnly(intellijDep()) { includeJars("util") }
    testApi(intellijDep()) { includeJars("platform-api", "util") }
    testApi(intellijCoreDep()) { includeJars("intellij-core") }

}

projectTest {
    workingDir = rootDir
}

publish()

runtimeJar()
sourcesJar()
javadocJar()
