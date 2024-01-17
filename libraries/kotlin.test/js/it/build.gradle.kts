import com.github.gradle.node.npm.task.NpmTask
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsCompilerAttribute
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrLink
import java.io.FileOutputStream

plugins {
    kotlin("js")
    alias(libs.plugins.gradle.node)
    idea
}

description = "Kotlin-test integration tests for JS IR"

node {
    version.set(nodejsVersion)
    download.set(true)
}

idea {
    module.excludeDirs.add(file("node_modules"))
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

    }
}

val nodeModules by configurations.registering {
    extendsFrom(configurations["api"])
    attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(KotlinUsages.KOTLIN_RUNTIME))
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.js)
        attribute(KotlinJsCompilerAttribute.jsCompilerAttribute, KotlinJsCompilerAttribute.ir)
    }
}

val compileTestDevelopmentExecutableKotlinJs = tasks.named<KotlinJsIrLink>("compileTestDevelopmentExecutableKotlinJs") {
    compilerOptions.moduleName = "kotlin-kotlin-test-js-it-test"
}

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

    into(layout.buildDirectory.dir("node_modules"))
}

fun createFrameworkTest(name: String): TaskProvider<NpmTask> {
    return tasks.register("test$name", NpmTask::class.java) {
        dependsOn(compileTestDevelopmentExecutableKotlinJs, populateNodeModules, "npmInstall")
        val testName = name
        val lowerName = name.lowercase()
        val tcOutput = layout.buildDirectory.file("tc-${lowerName}.log")
        val stdOutput = layout.buildDirectory.file("test-${lowerName}.log")
        val errOutput = layout.buildDirectory.file("test-${lowerName}.err.log")
        val exitCodeFile = layout.buildDirectory.file("test-${lowerName}.exit-code")
//        inputs.files(sourceSets.test.output)
        inputs.dir(layout.buildDirectory.dir("node_modules"))
        outputs.files(tcOutput, stdOutput, errOutput, exitCodeFile)

        args.set(listOf("run", "test-$lowerName"))
//        args("run")
//        args("test-$lowerName")
        group = "verification"

        execOverrides {
            isIgnoreExitValue = true
            standardOutput = FileOutputStream(stdOutput.get().asFile)
            errorOutput = FileOutputStream(errOutput.get().asFile)
        }
        doLast {
            println(tcOutput.get().asFile.readText())
            if (exitCodeFile.get().asFile.readText() != "0" /* && !rootProject.ignoreTestFailures*/) {
                throw GradleException("$testName integration test failed")
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
    api(project(":kotlin-test"))
}

