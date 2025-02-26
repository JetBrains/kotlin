import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.ideaExt.idea

description = "kotlinp-jvm"

plugins {
    kotlin("jvm")
    id("jps-compatible")
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
    testApi(projectTests(":compiler:tests-common-new"))
    testImplementation(projectTests(":generators:test-generator"))

    testRuntimeOnly(project(":kotlin-metadata-jvm"))

    shadows(project(":kotlin-metadata-jvm"))
    shadows(project(":tools:kotlinp"))
    shadows(libs.intellij.asm)
}

val generationRoot = projectDir.resolve("tests-gen")

sourceSets {
    "main" { projectDefault() }
    "test" {
        projectDefault()
        this.java.srcDir(generationRoot.name)
    }
}

if (kotlinBuildProperties.isInJpsBuildIdeaSync) {
    apply(plugin = "idea")
    idea {
        this.module.generatedSourceDirs.add(generationRoot)
    }
}

projectTest(parallel = true, jUnitMode = JUnitMode.JUnit5) {
    workingDir = rootDir
    useJUnitPlatform()
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
