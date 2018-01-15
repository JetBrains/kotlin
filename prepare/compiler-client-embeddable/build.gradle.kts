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

dependencies {
    jarContents(project(":compiler:cli-common")) { isTransitive = false }
    jarContents(project(":compiler:daemon-common")) { isTransitive = false }
    jarContents(projectRuntimeJar(":kotlin-daemon-client"))
    testCompile(project(":compiler:cli-common"))
    testCompile(project(":compiler:daemon-common"))
    testCompile(project(":kotlin-daemon-client"))
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
    dependsOn(":kotlin-compiler:dist",
              ":kotlin-stdlib:dist",
              ":kotlin-script-runtime:dist")
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
