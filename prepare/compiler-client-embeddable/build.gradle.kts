import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

description = "Kotlin compiler client embeddable"

plugins {
    maven
    kotlin("jvm")
}

val jarContents by configurations.creating
val testRuntimeCompilerJar by configurations.creating
val testStdlibJar by configurations.creating
val testScriptRuntimeJar by configurations.creating
val archives by configurations

dependencies {
    jarContents(project(":compiler:cli-common")) { isTransitive = false }
    jarContents(project(":daemon-common")) { isTransitive = false }
    jarContents(project(":daemon-common-new")) { isTransitive = false }
    jarContents(projectRuntimeJar(":kotlin-daemon-client"))
    testCompile(project(":compiler:cli-common"))
    testCompile(project(":daemon-common"))
    testCompile(project(":daemon-common-new"))
    testCompile(projectRuntimeJar(":kotlin-daemon-client"))
    testCompile(commonDep("junit:junit"))
    testCompile(project(":kotlin-test:kotlin-test-jvm"))
    testCompile(project(":kotlin-test:kotlin-test-junit"))
    testRuntimeCompilerJar(project(":kotlin-compiler"))
    testStdlibJar(kotlinStdlib())
    testScriptRuntimeJar(project(":kotlin-script-runtime"))
}

sourceSets {
    "main" {}
    "test" {
        // TODO: move closer
        java.srcDir("../../libraries/tools/kotlin-compiler-client-embeddable-test/src")
    }
}

projectTest {
    dependsOn(":kotlin-compiler:dist",
              ":kotlin-stdlib:dist",
              ":kotlin-script-runtime:dist")
    workingDir = File(rootDir, "libraries/tools/kotlin-compiler-client-embeddable-test/src")
    doFirst {
        systemProperty("kotlin.test.script.classpath", testSourceSet.output.classesDirs.joinToString(File.pathSeparator))
        systemProperty("compilerJar", testRuntimeCompilerJar.singleFile.canonicalPath)
        systemProperty("stdlibJar", testStdlibJar.singleFile.canonicalPath)
        systemProperty("scriptRuntimeJar", testScriptRuntimeJar.singleFile.canonicalPath)
    }
}

publish()

noDefaultJar()

runtimeJar(task<ShadowJar>("shadowJar")) {
    from(jarContents)
}

sourcesJar()
javadocJar()
