import com.github.gradle.node.npm.task.NpmTask
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrLink
import java.io.FileOutputStream

plugins {
    kotlin("multiplatform")
    alias(libs.plugins.gradle.node)
    idea
}

description = "Kotlin-test integration tests for JS"

node {
    version.set(nodejsVersion)
    download.set(true)
}

idea {
    module.excludeDirs.add(file("node_modules"))
}

kotlin {
    js {
        nodejs {
            testTask {
                enabled = false
            }
        }
    }

    sourceSets {
        val jsMain by getting {
            kotlin.srcDir("src/main/kotlin")
            dependencies {
                implementation(project(":kotlin-stdlib"))
            }
        }
        val jsTest by getting {
            kotlin.srcDir("src/test/kotlin")
            dependencies {
                implementation(project(":kotlin-test"))
            }
        }
    }
}

val compileTestDevelopmentExecutableKotlinJs = tasks.named<KotlinJsIrLink>("compileTestDevelopmentExecutableKotlinJs") {
    compilerOptions.moduleName = "kotlin-kotlin-test-js-it-test"
}

val populateNodeModules = tasks.register<Copy>("populateNodeModules") {
    dependsOn("compileTestDevelopmentExecutableKotlinJs")
    from(compileTestDevelopmentExecutableKotlinJs.map { it.destinationDirectory })

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
