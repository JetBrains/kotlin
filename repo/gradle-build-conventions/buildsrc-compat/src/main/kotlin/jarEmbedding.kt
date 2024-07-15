import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.internal.jvm.Jvm
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.*
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/**
 * Resolves [sourceConfiguration], searches for [dependenciesToInherit] in this configuration and puts the found dependency versions in
 * [targetConfiguration]. Also creates and returns a task that fails if dependencies were not found in the [sourceConfiguration]
 */
fun Project.inheritAndValidateExternalDependencies(
    sourceConfiguration: Configuration,
    targetConfiguration: Configuration,
    dependenciesToInherit: Map<String, String>,
): TaskProvider<*> {
    val inheritedDependencies = sourceConfiguration.incoming.resolutionResult.allComponents.filter {
        val moduleVersion = it.moduleVersion ?: return@filter false
        dependenciesToInherit[moduleVersion.group] == moduleVersion.name
    }.map { it.moduleVersion!! }

    inheritedDependencies.forEach {
        targetConfiguration.dependencies.add(
            dependencies.create(group = it.group, name = it.name, version = it.version)
        )
    }

    return tasks.register("validateInheritedDependencies") {
        doLast {
            if (inheritedDependencies.size != dependenciesToInherit.size) {
                error("Actual inherited dependencies: $inheritedDependencies")
            }
        }
    }
}

/**
 * This task runs ProGuard on the embedded jar with a certain runtime classpath. ProGuard will fail the task if the embedded jar references
 * classes that are not present in libraryjars.
 */
fun Project.validateEmbeddedJarClasspathUsingProguard(
    embeddedJar: TaskProvider<out Jar>,
    runtimeClasspath: ConfigurableFileCollection,
    proguardConfiguration: RegularFile,
) = tasks.register("validateEmbeddedJarClasspathUsingProguard", CacheableProguardTask::class) {
    outputs.cacheIf { false }
    outputs.upToDateWhen { false }

    dependsOn(runtimeClasspath)
    dependsOn(embeddedJar)
    javaLauncher.set(project.getToolchainLauncherFor(JdkMajorVersion.JDK_1_8))

    configuration(proguardConfiguration)

    injars(files(embeddedJar))
    outjars(layout.buildDirectory.file("proguard/output.jar"))

    libraryjars(runtimeClasspath)
    libraryjars(
        files(
            javaLauncher.map {
                firstFromJavaHomeThatExists(
                    "jre/lib/rt.jar",
                    "../Classes/classes.jar",
                    jdkHome = it.metadata.installationPath.asFile
                )!!
            },
            javaLauncher.map {
                firstFromJavaHomeThatExists(
                    "jre/lib/jsse.jar",
                    "../Classes/jsse.jar",
                    jdkHome = it.metadata.installationPath.asFile
                )!!
            },
            javaLauncher.map {
                Jvm.forHome(it.metadata.installationPath.asFile).toolsJar!!
            }
        )
    )
}

/**
 * This task makes sure there are no duplicates in the runtime classpath; i.e. runtimeOnly dependencies are correctly specified and
 * there is no collision with embedded classes
 */
fun Project.validateEmbeddedJarRuntimeClasspathHasNoDuplicates(
    embeddedJar: TaskProvider<out Jar>,
    runtimeClasspath: ConfigurableFileCollection,
) = tasks.register("validateEmbeddedJarRuntimeClasspathHasNoDuplicates") {
    dependsOn(embeddedJar)
    dependsOn(runtimeClasspath)

    fun String.isImplementationClassFile() = endsWith(".class") && !endsWith("module-info.class")
    fun File.forEachClassFileInAJar(action: (ZipEntry) -> (Unit)) = ZipFile(this).use { zip ->
        zip.entries().asSequence().filter {
            it.name.isImplementationClassFile()
        }.forEach { action(it) }
    }
    val embeddedJarFiles = files(embeddedJar)

    doLast {
        val duplicates = mutableMapOf<String, MutableList<File>>()
        (embeddedJarFiles + runtimeClasspath).forEach { jar ->
            jar.forEachClassFileInAJar { entry ->
                duplicates.getOrPut(entry.name, ::mutableListOf).add(jar)
            }
        }
        val duplicateClassfiles = duplicates.filter { it.value.size > 1 }
        if (duplicateClassfiles.isNotEmpty()) {
            error(
                """
                    |Duplicates in runtime classpath:
                    |${duplicateClassfiles.map { "${it.key}: ${it.value}" }.joinToString("\n")}
                """.trimMargin()
            )
        }
    }
}