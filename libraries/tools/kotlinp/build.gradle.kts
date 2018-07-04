import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

description = "kotlinp"

plugins {
    kotlin("jvm")
}

val shadows by configurations.creating

dependencies {
    compileOnly(project(":kotlinx-metadata"))
    compileOnly(project(":kotlinx-metadata-jvm"))
    compile("org.ow2.asm:asm:6.0")

    testCompileOnly(project(":kotlinx-metadata"))
    testCompileOnly(project(":kotlinx-metadata-jvm"))
    testCompile(commonDep("junit:junit"))
    testCompile(projectTests(":generators:test-generator"))

    testRuntime(project(":kotlinx-metadata-jvm", configuration = "runtime"))

    shadows(project(":kotlinx-metadata-jvm", configuration = "runtime"))
    shadows("org.ow2.asm:asm:6.0")
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
    classifier = "shadow"
    version = null
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
        dependsOn(":kotlin-reflect:dist")
        dependsOn(":kotlin-script-runtime:dist")
    }
}
