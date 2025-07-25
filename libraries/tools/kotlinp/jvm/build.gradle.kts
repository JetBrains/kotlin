import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.ideaExt.idea

description = "kotlinp-jvm"

plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("compiler-tests-convention")
}

val shadows by configurations.creating

dependencies {
    compileOnly(project(":kotlin-metadata"))
    compileOnly(project(":kotlin-metadata-jvm"))

    api(project(":tools:kotlinp"))
    implementation(libs.intellij.asm)

    testApi(intellijCore())

    testCompileOnly(project(":kotlin-metadata"))
    testCompileOnly(project(":kotlin-metadata-jvm"))

    testApi(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testApi(testFixtures(project(":compiler:tests-common-new")))
    testImplementation(testFixtures(project(":generators:test-generator")))

    testRuntimeOnly(project(":kotlin-metadata-jvm"))

    shadows(project(":kotlin-metadata-jvm"))
    shadows(project(":tools:kotlinp"))
    shadows(libs.intellij.asm)
}

sourceSets {
    "main" { projectDefault() }
    "test" {
        projectDefault()
        generatedTestDir()
    }
}

compilerTests {
    testTask(jUnitMode = JUnitMode.JUnit5) {
        workingDir = rootDir
    }

    testGenerator("org.jetbrains.kotlin.kotlinp.jvm.test.GenerateKotlinpTestsKt")
}

val shadowJar by task<ShadowJar> {
    archiveClassifier.set("shadow")
    archiveVersion.set("")
    configurations = listOf(shadows)
    from(mainSourceSet.output)
    manifest {
        attributes["Main-Class"] = "org.jetbrains.kotlin.kotlinp.jvm.Main"
    }
    mergeServiceFiles()
}

tasks {
    "assemble" {
        dependsOn(shadowJar)
    }
    "test" {
        // These dependencies are needed because ForTestCompileRuntime loads jars from dist
        dependsOn(rootProject.tasks.named("dist"))
    }
}

testsJar()
