/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("LibrariesCommon")

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.java.archives.Manifest
import org.gradle.api.plugins.BasePluginExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.*
import org.gradle.process.CommandLineArgumentProvider
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

@JvmOverloads
fun Project.configureJava9Compilation(
    moduleName: String,
    moduleOutputs: Collection<FileCollection> = setOf(sourceSets["main"].output),
    compileClasspathConfiguration: Configuration = configurations["compileClasspath"],
    sourceSetName: String = "java9"
) {
    val sourceSetNameC = sourceSetName.capitalize()
    val java9CompileClasspath = configurations["${sourceSetName}CompileClasspath"]
    java9CompileClasspath.extendsFrom(compileClasspathConfiguration)

    val kotlinCompileTaskNames = setOf("compile${sourceSetNameC}Kotlin", "compile${sourceSetNameC}KotlinJvm")

    tasks.withType<KotlinJvmCompile>().configureEach {
        if (name in kotlinCompileTaskNames) {
            configureTaskToolchain(JdkMajorVersion.JDK_17_0)
            compilerOptions.jvmTarget.set(JvmTarget.fromTarget(JdkMajorVersion.JDK_9_0.targetName))
            compilerOptions.freeCompilerArgs.add("-Xjdk-release=${JdkMajorVersion.JDK_9_0.targetName}")
        }
    }

    tasks.named("compile${sourceSetNameC}Java", JavaCompile::class.java) {
        dependsOn(moduleOutputs)

        targetCompatibility = JavaVersion.VERSION_1_9.toString()
        sourceCompatibility = JavaVersion.VERSION_1_9.toString()
        configureTaskToolchain(JdkMajorVersion.JDK_17_0)

        // module-info.java should be in java9 source set by convention
        val java9SourceSet = sourceSets[sourceSetName].java
        destinationDirectory.set(java9SourceSet.destinationDirectory.asFile.get().resolve("META-INF/versions/9"))
        options.sourcepath = files(java9SourceSet.srcDirs)
        val compileClasspath = java9CompileClasspath
        val moduleFiles = objects.fileCollection().from(moduleOutputs)
        val modulePath = compileClasspath.filter { it !in moduleFiles.files }
        dependsOn(modulePath)
        classpath = objects.fileCollection().from()
        options.compilerArgumentProviders.add(
            Java9AdditionalArgumentsProvider(
                moduleName,
                moduleFiles,
                modulePath
            )
        )
    }
}

private class Java9AdditionalArgumentsProvider(
    private val moduleName: String,
    private val moduleFiles: FileCollection,
    private val modulePath: FileCollection
) : CommandLineArgumentProvider {
    override fun asArguments(): Iterable<String> = listOf(
        "--module-path", modulePath.asPath,
        "--patch-module", "$moduleName=${moduleFiles.asPath}",
        "--release", "9"
    )
}

@JvmOverloads
fun Project.manifestAttributes(
    manifest: Manifest,
    component: String? = null,
    multiRelease: Boolean = false
) {
    manifest.attributes(
        "Implementation-Vendor" to "JetBrains",
        "Implementation-Title" to project.extensions.getByType<BasePluginExtension>().archivesName,
        "Implementation-Version" to project.rootProject.extra["buildNumber"] as String
    )

    if (component != null) {
        val kotlinLanguageVersion: String by rootProject.extra
        manifest.attributes(
            "Kotlin-Runtime-Component" to component,
            "Kotlin-Version" to kotlinLanguageVersion
        )
    }

    if (multiRelease) {
        manifest.attributes(
            "Multi-Release" to "true"
        )
    }
}
