import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

description = "Kotlin compiler client embeddable"

buildscript {
    repositories {
        jcenter()
    }

    dependencies {
        classpath("com.github.jengelman.gradle.plugins:shadow:${property("versions.shadow")}")
    }
}

plugins {
    maven
}

apply { plugin("kotlin") }

val jarContents by configurations.creating
val testRuntimeCompilerJar by configurations.creating
val testStdlibJar by configurations.creating
val testScriptRuntimeJar by configurations.creating
val archives by configurations

val projectsToInclude = listOf(
        ":compiler:cli-common",
        ":compiler:daemon-common",
        ":kotlin-daemon-client")

dependencies {
    projectsToInclude.forEach {
        jarContents(project(it)) { isTransitive = false }
        testCompile(project(it))
    }
    testCompile(commonDep("junit:junit"))
    testCompile(projectDist(":kotlin-test:kotlin-test-jvm"))
    testCompile(projectDist(":kotlin-test:kotlin-test-junit"))
    testRuntimeCompilerJar(projectDist(":kotlin-compiler"))
    testStdlibJar(projectDist(":kotlin-stdlib"))
    testScriptRuntimeJar(projectDist(":kotlin-script-runtime"))
}

sourceSets {
    "main" {}
    "test" {
        // TODO: move closer
        java.srcDir("../../libraries/tools/kotlin-compiler-client-embeddable-test/src")
    }
}

projectTest {
    dependsOnTaskIfExistsRec("dist", project = rootProject)
    workingDir = File(rootDir, "libraries/tools/kotlin-compiler-client-embeddable-test/src")
    doFirst {
        systemProperty("kotlin.test.script.classpath", the<JavaPluginConvention>().sourceSets.getByName("test").output.classesDirs.joinToString(File.pathSeparator))
        systemProperty("compilerJar", testRuntimeCompilerJar.singleFile.canonicalPath)
        systemProperty("stdlibJar", testStdlibJar.singleFile.canonicalPath)
        systemProperty("scriptRuntimeJar", testScriptRuntimeJar.singleFile.canonicalPath)
    }
}

archives.artifacts.let { artifacts ->
    artifacts.forEach {
        if (it.type == "jar") {
            artifacts.remove(it)
        }
    }
}

runtimeJar(task<ShadowJar>("shadowJar")) {
    from(jarContents)
}
sourcesJar()
javadocJar()

publish()
