@file:Suppress("HasPlatformType")

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget.*

plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
}

kotlin {
    sourceSets.all {
        languageSettings.optIn("org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi")
    }
}

val embedded by configurations.getting {
    isTransitive = false
    configurations.getByName("compileOnly").extendsFrom(this)
    configurations.getByName("testImplementation").extendsFrom(this)
}

dependencies {
    api(project(":kotlin-gradle-plugin-idea"))
    embedded(libs.protobuf.java)
    embedded(libs.protobuf.kotlin)
    testImplementation(kotlinTest("junit"))
    testImplementation(kotlin("reflect"))
    testImplementation(testFixtures(project(":kotlin-gradle-plugin-idea")))
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
    exclude("**/*.proto")
    relocate("com.google.protobuf", "org.jetbrains.kotlin.gradle.idea.proto.com.google.protobuf")
}

/* Setup configuration for binary compatibility tests */
run {
    val binaryValidationApiJar = tasks.register<Jar>("binaryValidationApiJar") {
        this.archiveBaseName.set(project.name + "-api")
        from(mainSourceSet.output)
    }

    apiValidation {
        ignoredPackages += "org.jetbrains.kotlin.gradle.idea.proto.generated"
        nonPublicMarkers += "org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi"
    }

    tasks {
        apiBuild {
            inputJar.value(binaryValidationApiJar.flatMap { it.archiveFile })
        }
    }
}


/* Setup protoc */
run {
    val protoc = configurations.create("protoc") {
        isCanBeResolved = true
        isCanBeConsumed = false
    }

    dependencies {
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
    val setupProtoc = tasks.register("setupProtoc") {
        doFirst {
            val protocFile = protocExecutable.get().asFile
            protoc.files.single().copyTo(protocFile, overwrite = true)
            protocFile.setExecutable(true)
        }
    }

    tasks.register<Exec>("protoc") {
        dependsOn(setupProtoc)

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

        argumentProviders.add {
            listOf(
                protocExecutable.get().asFile.absolutePath,
                "-I=$protoSources",
                "--java_out=${javaOutput.absolutePath}",
                "--kotlin_out=${kotlinOutput.absolutePath}"
            ) + protoSources.listFiles().orEmpty()
                .filter { it.extension == "proto" }
                .map { it.path }
        }
    }
}


/* Setup backwards compatibility tests */
run {
    val compatibilityTestClasspath by configurations.creating {
        isCanBeResolved = true
        isCanBeConsumed = false
        attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attributes.attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
    }

    dependencies {
        compatibilityTestClasspath(project(":kotlin-gradle-plugin-idea-for-compatibility-tests"))
    }

    tasks.test {
        val capturedCompatibilityTestClasspath: FileCollection = compatibilityTestClasspath
        dependsOn(capturedCompatibilityTestClasspath)
        inputs.files(capturedCompatibilityTestClasspath)
        doFirst {
            systemProperty("compatibilityTestClasspath", capturedCompatibilityTestClasspath.files.joinToString(";") { it.absolutePath })
        }
    }
}
