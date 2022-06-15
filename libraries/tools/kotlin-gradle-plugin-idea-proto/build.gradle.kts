@file:Suppress("HasPlatformType")

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm")
}

kotlin {
    sourceSets.all {
        languageSettings.optIn("org.jetbrains.kotlin.gradle.kpm.idea.InternalKotlinGradlePluginApi")
    }
}

val shadows by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = false
    isTransitive = false
    configurations.getByName("compileOnly").extendsFrom(this)
    configurations.getByName("testImplementation").extendsFrom(this)
}

val shadowsRuntime by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    isTransitive = false
    extendsFrom(shadows)
    attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
    attributes.attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
}

dependencies {
    api(project(":kotlin-gradle-plugin-idea"))
    shadows("com.google.protobuf:protobuf-java:3.19.4")
    shadows("com.google.protobuf:protobuf-kotlin:3.19.4")
    testImplementation(project(":kotlin-test:kotlin-test-junit"))
    testImplementation(testFixtures(project(":kotlin-gradle-plugin-idea")))
}


configureKotlinCompileTasksGradleCompatibility()

sourceSets.main.configure {
    java.srcDir("src/generated/java")
    java.srcDir("src/generated/kotlin")
}

javadocJar()
sourcesJar()
runtimeJar(tasks.register<ShadowJar>("shadowJar")) {
    exclude("**/*.proto")
    from(mainSourceSet.output)
    configurations = listOf(shadowsRuntime)
    relocate("com.google.protobuf", "org.jetbrains.kotlin.kpm.idea.proto.com.google.protobuf")
}

publish()

tasks.register<Exec>("protoc") {
    val protoSources = file("src/main/proto")
    val javaOutput = file("src/generated/java/")
    val kotlinOutput = file("src/generated/kotlin/")

    inputs.dir(protoSources)
    outputs.dir(javaOutput)
    outputs.dir(kotlinOutput)

    doFirst {
        javaOutput.deleteRecursively()
        kotlinOutput.deleteRecursively()
        javaOutput.mkdirs()
        kotlinOutput.mkdirs()
    }

    workingDir(project.projectDir)

    commandLine(
        *arrayOf(
            "protoc",
            "-I=$protoSources",
            "--java_out=${javaOutput.absolutePath}",
            "--kotlin_out=${kotlinOutput.absolutePath}"
        ) + protoSources.listFiles().orEmpty()
            .filter { it.extension == "proto" }
            .map { it.path },
    )

    doLast {
        kotlinOutput.walkTopDown()
            .filter { it.extension == "kt" }
            .forEach { file -> file.writeText(file.readText().replace("public", "internal")) }

        javaOutput.walkTopDown()
            .filter { it.extension == "java" }
            .forEach { file ->
                file.writeText(
                    file.readText()
                        .replace("public final class", "final class")
                        .replace("public interface", "interface")
                )
            }
    }
}
