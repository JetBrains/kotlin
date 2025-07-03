/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import org.jetbrains.kotlin.gradle.plugin.konan.tasks.KonanCacheTask
import org.jetbrains.kotlin.gradle.plugin.konan.tasks.KonanInteropTask
import org.jetbrains.kotlin.PlatformInfo
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.konan.target.*
import org.jetbrains.kotlin.konan.util.*
import org.jetbrains.kotlin.nativeDistribution.nativeDistribution
import org.jetbrains.kotlin.platformLibs.*
import org.jetbrains.kotlin.platformManager
import org.jetbrains.kotlin.utils.capitalized

plugins {
    id("base")
    id("platform-manager")
}

// region: Util functions.
fun KonanTarget.defFiles() = familyDefFiles(family).map { DefFile(it, this) }

fun defFileToLibName(target: String, name: String) = "$target-$name"

private fun interopTaskName(libName: String, targetName: String) = "compileKonan${libName.capitalized}${targetName.capitalized}"
private fun cacheTaskName(target: String, name: String) = "${defFileToLibName(target, name)}Cache"

private abstract class CompilePlatformLibsSemaphore : BuildService<BuildServiceParameters.None>
private abstract class CachePlatformLibsSemaphore : BuildService<BuildServiceParameters.None>

private val compilePlatformLibsSemaphore = gradle.sharedServices.registerIfAbsent("compilePlatformLibsSemaphore", CompilePlatformLibsSemaphore::class.java) {
    if (kotlinBuildProperties.limitPlatformLibsCompilationConcurrency) {
        maxParallelUsages.set(1)
    }
}

private val cachePlatformLibsSemaphore = gradle.sharedServices.registerIfAbsent("cachePlatformLibsSemaphore", CachePlatformLibsSemaphore::class.java) {
    if (kotlinBuildProperties.limitPlatformLibsCacheBuildingConcurrency) {
        maxParallelUsages.set(1)
    }
}

// endregion

if (HostManager.host == KonanTarget.MACOS_ARM64) {
    project.configureJvmToolchain(JdkMajorVersion.JDK_17_0)
}

val cacheableTargetNames = platformManager.hostPlatform.cacheableTargets

val updateDefFileDependenciesTask = tasks.register("updateDefFileDependencies")
val updateDefFileTasksPerFamily = if (HostManager.hostIsMac) {
    registerUpdateDefFileDependenciesForAppleFamiliesTasks(updateDefFileDependenciesTask)
} else {
    emptyMap()
}

val platformLibsElements by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named("native-platform-libs"))
        attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.DIRECTORY_TYPE)
    }
}

val platformLibsCacheElements by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named("native-platform-libs-caches"))
        attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.DIRECTORY_TYPE)
    }
}

enabledTargets(platformManager).forEach { target ->
    val targetName = target.visibleName

    val libTasks = mutableListOf<TaskProvider<KonanInteropTask>>()
    val cacheTasks = mutableListOf<TaskProvider<KonanCacheTask>>()

    target.defFiles().forEach { df ->
        val libName = defFileToLibName(targetName, df.name)
        val fileNamePrefix = PlatformLibsInfo.namePrefix
        val artifactName = "${fileNamePrefix}${df.name}"

        val libTask = tasks.register(interopTaskName(libName, targetName), KonanInteropTask::class.java) {
            group = BasePlugin.BUILD_GROUP
            description = "Build the Kotlin/Native platform library '$libName' for '$target'"

            updateDefFileTasksPerFamily[target.family]?.let { dependsOn(it) }

            // Requires Native distribution with compiler JARs and stdlib klib.
            this.compilerDistribution.set(nativeDistribution)
            dependsOn(":kotlin-native:prepare:kotlin-native-distribution:distStdlib")

            this.target.set(targetName)
            this.outputDirectory.set(layout.buildDirectory.dir("konan/libs/$targetName/${fileNamePrefix}${df.name}"))
            df.file?.let { this.defFile.set(it) }
            df.config.depends.forEach { defName ->
                this.klibFiles.from(tasks.named(interopTaskName(defFileToLibName(targetName, defName), targetName)))
            }
            this.extraOpts.addAll(
                    "-Xpurge-user-libs",
                    "-Xshort-module-name", df.name,
                    "-Xdisable-experimental-annotation",
                    "-no-default-libs",
                    "-no-endorsed-libs",
            )
            if (target.family.isAppleFamily) {
                // Platform Libraries for Apple targets use modules. Use shared cache for them.
                // Keep the path relative to hit the build cache.
                val fmodulesCache = project.layout.buildDirectory.dir("clangModulesCache").get().asFile.toRelativeString(project.layout.projectDirectory.asFile)
                this.extraOpts.addAll("-compiler-option", "-fmodules-cache-path=$fmodulesCache")
            }

            usesService(compilePlatformLibsSemaphore)
        }
        libTasks.add(libTask)

        if (target.name in cacheableTargetNames) {
            val cacheTask = tasks.register(cacheTaskName(targetName, df.name), KonanCacheTask::class.java) {
                group = BasePlugin.BUILD_GROUP
                description = "Build compilation cache of the Kotlin/Native platform library '$libName' for '$target'"

                val dist = nativeDistribution

                // Requires Native distribution with stdlib klib and its cache for `targetName`.
                this.compilerDistribution.set(dist)
                dependsOn(":kotlin-native:prepare:kotlin-native-distribution:crossDist${targetName.capitalized}")

                dependency {
                    klib.set(dist.map { it.stdlib })
                    cache.set(dist.map { it.stdlibCache(targetName) })
                }
                df.config.depends.forEach { dep ->
                    dependency {
                        klib.set(tasks.named<KonanInteropTask>(interopTaskName(defFileToLibName(targetName, dep), targetName)).map { it.outputDirectory.get() })
                        cache.set(tasks.named<KonanCacheTask>(cacheTaskName(targetName, dep)).map { it.outputDirectory.get() })
                    }
                }

                this.klib.fileProvider(libTask.map { it.outputs.files.singleFile })
                this.target.set(targetName)
                this.outputDirectory.set(layout.buildDirectory.dir("konan/cache/$targetName/$targetName-gSTATIC-system/${artifactName}-cache"))

                usesService(cachePlatformLibsSemaphore)
            }
            cacheTasks.add(cacheTask)
        }
    }

    val installPlatformLibs = tasks.register("installPlatformLibs${targetName.capitalized}", Sync::class) {
        libTasks.forEach { libTask ->
            from(libTask) {
                into(libTask.map { it.outputDirectory.get().asFile.name })
            }
        }
        into(layout.buildDirectory.dir("platform/$targetName"))
    }
    platformLibsElements.outgoing.variants.create(target.name).apply {
        attributes {
            attribute(TargetWithSanitizer.TARGET_ATTRIBUTE, target.withSanitizer())
        }
    }.artifact(installPlatformLibs)

    if (target.name in cacheableTargetNames) {
        val installPlatformLibsCaches = tasks.register("installPlatformLibsCaches${targetName.capitalized}", Sync::class) {
            cacheTasks.forEach { cacheTask ->
                from(cacheTask) {
                    into(cacheTask.map { it.outputDirectory.get().asFile.name })
                }
            }
            into(layout.buildDirectory.dir("cache/$targetName/$targetName-gSTATIC-system"))
        }
        platformLibsCacheElements.outgoing.variants.create(target.name).apply {
            attributes {
                attribute(TargetWithSanitizer.TARGET_ATTRIBUTE, target.withSanitizer())
            }
        }.artifact(installPlatformLibsCaches)
    }
}
