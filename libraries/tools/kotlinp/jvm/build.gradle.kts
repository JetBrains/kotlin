import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

description = "kotlinp-jvm"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

val kotlinpAsmVersion = "9.0"
val shadows by configurations.creating

dependencies {
    compileOnly(project(":kotlin-metadata"))
    compileOnly(project(":kotlin-metadata-jvm"))

    api(project(":tools:kotlinp"))
    implementation("org.jetbrains.intellij.deps:asm-all:$kotlinpAsmVersion")

    testApi(intellijCore())

    testCompileOnly(project(":kotlin-metadata"))
    testCompileOnly(project(":kotlin-metadata-jvm"))

    testImplementation(libs.junit4)
    testImplementation(projectTests(":compiler:tests-common"))
    testImplementation(projectTests(":generators:test-generator"))

    testRuntimeOnly(project(":kotlin-metadata-jvm"))

    shadows(project(":kotlin-metadata-jvm"))
    shadows(project(":tools:kotlinp"))
    shadows("org.jetbrains.intellij.deps:asm-all:$kotlinpAsmVersion")
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTest {
    workingDir = rootDir
}

val generateTests by generator("org.jetbrains.kotlin.kotlinp.jvm.test.GenerateKotlinpTestsKt")

val shadowJar by task<ShadowJar> {
    archiveClassifier.set("shadow")
    archiveVersion.set("")
    configurations = listOf(shadows)
    from(mainSourceSet.output)
    manifest {
        attributes["Main-Class"] = "org.jetbrains.kotlin.kotlinp.jvm.Main"
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
