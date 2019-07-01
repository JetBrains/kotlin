description = "Kotlin compiler client embeddable"

plugins {
    maven
    kotlin("jvm")
}

val testRuntimeCompilerJar by configurations.creating
val testStdlibJar by configurations.creating
val testScriptRuntimeJar by configurations.creating

dependencies {
    embedded(project(":compiler:cli-common")) { isTransitive = false }
    embedded(project(":daemon-common")) { isTransitive = false }
    embedded(project(":daemon-common-new")) { isTransitive = false }
    embedded(projectRuntimeJar(":kotlin-daemon-client"))
    
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
    dependsOn(":dist")
    workingDir = File(rootDir, "libraries/tools/kotlin-compiler-client-embeddable-test/src")
    doFirst {
        systemProperty("kotlin.test.script.classpath", testSourceSet.output.classesDirs.joinToString(File.pathSeparator))
        systemProperty("compilerJar", testRuntimeCompilerJar.singleFile.canonicalPath)
        systemProperty("stdlibJar", testStdlibJar.singleFile.canonicalPath)
        systemProperty("scriptRuntimeJar", testScriptRuntimeJar.singleFile.canonicalPath)
    }
}

publish()

runtimeJar()

sourcesJar()

javadocJar()
