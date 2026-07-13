import org.jetbrains.kotlin.testFederation.SmokeTestConfig
import org.jetbrains.kotlin.testFederation.smokeTestConfig

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("project-tests-convention")
}

val jdkVersion = JdkMajorVersion.JDK_17_0
configureJvmToolchain(jdkVersion)

dependencies {
    implementation(kotlinStdlib())
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.core.jvm)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.schema.generator.json)
    implementation(libs.jgit)
    implementation("org.slf4j:slf4j-nop:1.7.36") // Because jgit uses slf4j.

    testImplementation(kotlin("test-junit5"))
    testImplementation(platform(libs.junit.bom))
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.junit.jupiter.api)
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

abstract class CodeReviewTask : JavaExec() {
    @set:Option(
        "base",
        "The base git revision to compare the sources against. For example, origin/master (default) or HEAD~2"
    )
    @get:Input
    @get:Optional
    var base: String? = null
}

tasks.register<CodeReviewTask>("reviewCode") {
    description = "Review the code change automatically using an AI agent"

    classpath(sourceSets.named("main").flatMap { it.kotlin.classesDirectory })
    classpath(sourceSets.named("main").map { it.compileClasspath })
    mainClass.set("org.jetbrains.kotlin.code.review.LocalKt")

    val output = layout.buildDirectory.file("review.md")
    val rootDir = rootDir

    outputs.file(output)
    outputs.upToDateWhen { false }

    argumentProviders.add {
        listOf(
            output.get().asFile.path,
            rootDir.absolutePath
        ) + listOfNotNull(base)
    }
}

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit5, javaLauncher = jdkVersion) {
        systemProperty("kotlin.repo.auto-code-review.rootDir", rootDir.absolutePath)

        // One of the tests traverses all files in the repo. And the tests are fairly quick.
        // It is therefore reasonable to make it always rerun instead of defining its inputs:
        smokeTestConfig = SmokeTestConfig.RunAllTests
        outputs.upToDateWhen { false }
    }
}

tasks.withType<JavaExec> {
    // Even in JDK 25, ProcessBuilder still doesn't properly support arguments with quotes inside on Windows.
    // This module uses such arguments, e.g. when running `claude` and passing `--settings` or `--json-schema`.
    // This system property fixes the problem:
    systemProperty("jdk.lang.Process.allowAmbiguousCommands", false)
}
