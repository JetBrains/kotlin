import com.moowork.gradle.node.npm.NpmTask
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrLink
import java.io.FileOutputStream

plugins {
    kotlin("js")
    id("com.github.node-gradle.node") version "2.2.0"
}

description = "Kotlin-test integration tests for JS IR"

node {
    version = "16.13.0"
    download = true
}

val jsMainSources by task<Sync> {
    from("$rootDir/libraries/kotlin.test/js/it/src")
    into("$buildDir/jsMainSources")
}

val jsSources by task<Sync> {
    from("$rootDir/libraries/kotlin.test/js/it/js")
    into("$buildDir/jsSources")
}

val ignoreTestFailures by extra(project.kotlinBuildProperties.ignoreTestFailures)

kotlin {
    js(IR) {
        nodejs {
            testTask {
                enabled = false
            }
        }
    }

    sourceSets {
        val test by getting {
            kotlin.srcDir(jsMainSources.get().destinationDir)
        }
    }
}

val nodeModules by configurations.registering {
    extendsFrom(configurations["api"])
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class.java, KotlinUsages.KOTLIN_RUNTIME))
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.js)
    }
}

val compileTestDevelopmentExecutableKotlinJs = tasks.named<KotlinJsIrLink>("compileTestDevelopmentExecutableKotlinJs")

val populateNodeModules = tasks.register<Copy>("populateNodeModules") {
    dependsOn("compileTestDevelopmentExecutableKotlinJs")
    dependsOn(nodeModules)
    from(compileTestDevelopmentExecutableKotlinJs.map { it.destinationDirectory })

    from {
        nodeModules.get().map {
            // WORKAROUND: Some JS IR jars were absent and caused this task to fail.
            // They don't contain .js thus we can skip them.
            if (it.exists()) {
                zipTree(it.absolutePath).matching { include("*.js") }
            } else it
        }
    }

    into("${buildDir}/node_modules")
}

fun createFrameworkTest(name: String): TaskProvider<NpmTask> {
    return tasks.register("test$name", NpmTask::class.java) {
        dependsOn(compileTestDevelopmentExecutableKotlinJs, populateNodeModules, "npmInstall")
        val lowerName = name.toLowerCase()
        val tcOutput = "$buildDir/tc-${lowerName}.log"
        val stdOutput = "$buildDir/test-${lowerName}.log"
        val errOutput = "$buildDir/test-${lowerName}.err.log"
//        inputs.files(sourceSets.test.output)
        inputs.dir("${buildDir}/node_modules")
        outputs.files(tcOutput, stdOutput, errOutput)

        setArgs(listOf("run", "test-$lowerName"))
//        args("run")
//        args("test-$lowerName")
        group = "verification"

        setExecOverrides(closureOf<ExecSpec> {
            isIgnoreExitValue = true
            standardOutput = FileOutputStream(stdOutput)
            errorOutput = FileOutputStream(errOutput)
        })
        doLast {
            println(file(tcOutput).readText())
            if (result.exitValue != 0/* && !rootProject.ignoreTestFailures*/) {
                throw GradleException("$name integration test failed")
            }

        }
    }
}

val frameworkTests = listOf(
//    "Jest",
    "Jasmine",
    "Mocha",
    "Qunit",
//    "Tape"
).map {
    createFrameworkTest(it)
}

tasks.check {
    frameworkTests.forEach { dependsOn(it) }
}

dependencies {
    api(project(":kotlin-test:kotlin-test-js-ir"))
}

tasks.named("compileTestKotlinJs") {
    dependsOn(jsMainSources)
    dependsOn(jsSources)
}