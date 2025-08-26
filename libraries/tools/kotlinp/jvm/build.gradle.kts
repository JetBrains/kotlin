import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

description = "kotlinp-jvm"

plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("project-tests-convention")
    id("test-inputs-check")
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

projectTests {
    testData(isolated, "testData")
    testData(project(":compiler").isolated, "testData/loadJava")
    testData(project(":compiler").isolated, "testData/serialization")

    testTask(jUnitMode = JUnitMode.JUnit5)

    testGenerator("org.jetbrains.kotlin.kotlinp.jvm.test.GenerateKotlinpTestsKt", doNotSetFixturesSourceSetDependency = true)

    withJvmStdlibAndReflect()
    withScriptRuntime()
    withMockJdkAnnotationsJar()
    withTestJar()
    withMockJdkRuntime()
    withAnnotations()
    withScriptingPlugin()
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
}

testsJar()
