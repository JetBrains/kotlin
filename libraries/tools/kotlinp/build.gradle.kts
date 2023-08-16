import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

description = "kotlinp"

plugins {
    kotlin("jvm")
}

val kotlinpAsmVersion = "9.0"
val shadows by configurations.creating

dependencies {
    compileOnly(project(":kotlinx-metadata"))
    compileOnly(project(":kotlinx-metadata-jvm"))

    implementation("org.jetbrains.intellij.deps:asm-all:$kotlinpAsmVersion")

    testApi(intellijCore())

    testCompileOnly(project(":kotlinx-metadata"))
    testCompileOnly(project(":kotlinx-metadata-jvm"))

    testImplementation(libs.junit4)
    testImplementation(projectTests(":compiler:tests-common"))
    testImplementation(projectTests(":generators:test-generator"))

    testRuntimeOnly(project(":kotlinx-metadata-jvm"))

    shadows(project(":kotlinx-metadata-jvm"))
    shadows("org.jetbrains.intellij.deps:asm-all:$kotlinpAsmVersion")
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTest {
    workingDir = rootDir
}

val generateTests by generator("org.jetbrains.kotlin.kotlinp.test.GenerateKotlinpTestsKt")

val shadowJar by task<ShadowJar> {
    archiveClassifier.set("shadow")
    archiveVersion.set("")
    configurations = listOf(shadows)
    from(mainSourceSet.output)
    manifest {
        attributes["Main-Class"] = "org.jetbrains.kotlin.kotlinp.Main"
    }
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

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += listOf("-opt-in=kotlin.RequiresOptIn")
    }
}
