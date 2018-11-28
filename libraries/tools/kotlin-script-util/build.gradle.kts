
description = "Kotlin scripting support utilities"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

repositories {
    maven(url = "https://oss.sonatype.org/content/repositories/snapshots") // for jcabi-aether:1.0-SNAPSHOT
}

dependencies {
    compile(project(":kotlin-stdlib"))
    compile(project(":kotlin-script-runtime"))
    compile(project(":kotlin-scripting-jvm"))
    compileOnly(project(":compiler:cli"))
    compileOnly(project(":compiler:daemon-common"))
    compile(projectRuntimeJar(":kotlin-daemon-client"))
    compileOnly("com.jcabi:jcabi-aether:1.0-SNAPSHOT")
    compileOnly("org.sonatype.aether:aether-api:1.13.1")
    compileOnly("org.apache.maven:maven-core:3.0.3")
    testCompileOnly(project(":compiler:cli"))
    testCompile(project(":kotlin-test:kotlin-test-junit"))
    testRuntime(project(":kotlin-reflect"))
    testCompile(commonDep("junit:junit"))
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

runtimeJar()
sourcesJar()
javadocJar()

publish()

ideaPlugin()
