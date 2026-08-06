@file:Suppress("HasPlatformType")

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget.*

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
    id("project-tests-convention")
    id("test-inputs-check-v2")
}

val embedded = configurations.embedded.get().apply {
    isTransitive = false
    configurations.compileOnly.get().extendsFrom(this)
    configurations.testImplementation.get().extendsFrom(this)
}

dependencies {
    val coreDepsVersion = libs.versions.kotlin.`for`.gradle.plugins.compilation.get()

    embedded(libs.protobuf.java)
    embedded(libs.protobuf.kotlin)
    embedded(libs.protobuf.java.util)

    api(kotlin("stdlib", coreDepsVersion))
    runtimeOnly(libs.guava)
    runtimeOnly(commonDependency("com.google.code.gson:gson"))

    testImplementation(kotlin("stdlib", coreDepsVersion))
    testImplementation(kotlin("test-junit5", coreDepsVersion))
    testImplementation(commonDependency("com.google.code.gson:gson"))
}

configureKotlinCompileTasksGradleCompatibility()

sourceSets.main.configure {
    java.srcDir("src/generated/java")
    java.srcDir("src/generated/kotlin")
}

publish()
javadocJar()
sourcesJar()

runtimeJar(tasks.register<ShadowJar>("embeddable")) {
    from(mainSourceSet.output)
    from("src/main/proto") {
        into("META-INF/proto")
    }
    exclude("**/google/protobuf/**/*.proto")
    relocate(
        "com.google.protobuf",
        "org.jetbrains.kotlin.importmodels.internal.protobuf.com.google.protobuf",
    )
}

projectTests {
    testTask()
}

run {
    val binaryValidationApiJar = tasks.register<Jar>("binaryValidationApiJar") {
        archiveBaseName.set(project.name + "-api")
        from(mainSourceSet.output)
    }

    apiValidation {
        ignoredPackages += "org.jetbrains.kotlin.importmodels.proto"
        ignoredPackages += "org.jetbrains.kotlin.importmodels.internal"
    }

    tasks {
        apiBuild {
            inputJar.value(binaryValidationApiJar.flatMap { it.archiveFile })
        }
    }
}

run {
    val protoc = configurations.create("protoc") {
        isCanBeResolved = true
        isCanBeConsumed = false
    }
    val protobufIncludes = configurations.create("protobufIncludes") {
        isCanBeResolved = true
        isCanBeConsumed = false
        isTransitive = false
    }

    dependencies {
        protobufIncludes(libs.protobuf.java)
        protoc(libs.protoc) {
            artifact {
                type = "exe"
                classifier = when (HostManager.host) {
                    MACOS_ARM64 -> "osx-aarch_64"
                    MACOS_X64 -> "osx-x86_64"
                    MINGW_X64 -> "windows-x86_64"
                    LINUX_X64 -> "linux-x86_64"
                    else -> null
                }
            }
        }

        val protocVersion = libs.versions.protobuf.get()
        implicitDependencies("com.google.protobuf:protoc:$protocVersion:linux-x86_64@exe")
        implicitDependencies("com.google.protobuf:protoc:$protocVersion:osx-aarch_64@exe")
        implicitDependencies("com.google.protobuf:protoc:$protocVersion:osx-x86_64@exe")
        implicitDependencies("com.google.protobuf:protoc:$protocVersion:windows-x86_64@exe")
    }

    val protocExecutable = layout.buildDirectory.file("protoc/bin")
    val protobufIncludeDirectory = layout.buildDirectory.dir("protoc/include")
    val setupProtoc = tasks.register("setupProtoc") {
        notCompatibleWithConfigurationCache("Resolves the protoc configuration during task execution")

        doFirst {
            val protocFile = protocExecutable.get().asFile
            protoc.files.single().copyTo(protocFile, overwrite = true)
            protocFile.setExecutable(true)
        }
    }
    val setupProtobufIncludes = tasks.register<Sync>("setupProtobufIncludes") {
        from({ protobufIncludes.map { zipTree(it) } }) {
            include("google/protobuf/*.proto")
        }
        into(protobufIncludeDirectory)
    }

    tasks.register<Exec>("protoc") {
        dependsOn(setupProtoc, setupProtobufIncludes)

        val protoSources = file("src/main/proto")
        val javaOutput = file("src/generated/java")
        val kotlinOutput = file("src/generated/kotlin")

        inputs.dir(protoSources)
        inputs.files(protoc).withPropertyName("protocExecutable")
        inputs.files(protobufIncludes).withPropertyName("protobufIncludes")
        outputs.dir(javaOutput)
        outputs.dir(kotlinOutput)

        doFirst {
            javaOutput.deleteRecursively()
            kotlinOutput.deleteRecursively()
            javaOutput.mkdirs()
            kotlinOutput.mkdirs()
        }

        workingDir(project.projectDir)
        executable = protocExecutable.get().asFile.absolutePath
        argumentProviders.add {
            listOf(
                "-I=$protoSources",
                "-I=${protobufIncludeDirectory.get().asFile.absolutePath}",
                "--java_out=${javaOutput.absolutePath}",
                "--kotlin_out=${kotlinOutput.absolutePath}",
            ) + protoSources.listFiles().orEmpty()
                .filter { it.extension == "proto" }
                .map { it.path }
        }
    }
}
