/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import org.jetbrains.kotlin.gradle.plugin.konan.tasks.KonanCacheTask
import org.jetbrains.kotlin.gradle.plugin.tasks.KonanInteropTask
import org.jetbrains.kotlin.PlatformInfo
import org.jetbrains.kotlin.kotlinNativeDist
import org.jetbrains.kotlin.konan.target.*
import org.jetbrains.kotlin.konan.util.*
import org.jetbrains.kotlin.platformManager
import org.jetbrains.kotlin.utils.capitalized

plugins {
    id("base")
    id("platform-manager")
}

// region: Util functions.
fun KonanTarget.defFiles() =
        project.fileTree("src/platform/${family.visibleName}")
                .filter { it.name.endsWith(".def") }
                .map { DefFile(it, this) }


fun defFileToLibName(target: String, name: String) = "$target-$name"

private fun interopTaskName(libName: String, targetName: String) = "compileKonan${libName.capitalized}${targetName.capitalized}"

// endregion

if (HostManager.host == KonanTarget.MACOS_ARM64) {
    project.configureJvmToolchain(JdkMajorVersion.JDK_17_0)
}

val cacheableTargetNames = platformManager.hostPlatform.cacheableTargets

enabledTargets(platformManager).forEach { target ->
    val targetName = target.visibleName
    val installTasks = mutableListOf<TaskProvider<out Task>>()
    val cacheTasks = mutableListOf<TaskProvider<out Task>>()

    target.defFiles().forEach { df ->
        val libName = defFileToLibName(targetName, df.name)
        val fileNamePrefix = PlatformLibsInfo.namePrefix
        val artifactName = "${fileNamePrefix}${df.name}"

        val libTask = tasks.register(interopTaskName(libName, targetName), KonanInteropTask::class.java) {
            group = BasePlugin.BUILD_GROUP
            description = "Build the Kotlin/Native platform library '$libName' for '$target'"

            this.compilerDistributionPath.set(kotlinNativeDist.absolutePath)
            dependsOn(":kotlin-native:${targetName}CrossDist")

            this.konanTarget.set(target)
            this.outputDirectory.set(
                    layout.buildDirectory.dir("konan/libs/$targetName/${fileNamePrefix}${df.name}")
            )
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
            this.compilerOpts.addAll(
                    "-fmodules-cache-path=${project.layout.buildDirectory.dir("clangModulesCache").get().asFile}"
            )
            this.enableParallel.set(project.findProperty("kotlin.native.platformLibs.parallel")?.toString()?.toBoolean() ?: true)
        }

        val klibInstallTask = tasks.register(libName, Sync::class.java) {
            from(libTask)
            into(kotlinNativeDist.resolve("klib/platform/$targetName/$artifactName"))
        }
        installTasks.add(klibInstallTask)

        if (target.name in cacheableTargetNames) {
            val cacheTask = tasks.register("${libName}Cache", KonanCacheTask::class.java) {
                notCompatibleWithConfigurationCache("project used in execution time")
                this.target = targetName
                originalKlib.fileProvider(libTask.map { it.outputs.files.singleFile })
                klibUniqName = artifactName
                cacheRoot = kotlinNativeDist.resolve("klib/cache").absolutePath

                dependsOn(":kotlin-native:${targetName}StdlibCache")

                // Make it depend on platform libraries defined in def files and their caches
                df.config.depends.map {
                    defFileToLibName(targetName, it)
                }.forEach {
                    dependsOn(it)
                    dependsOn("${it}Cache")
                }
            }
            cacheTasks.add(cacheTask)
        }
    }

    tasks.register("${targetName}Install") {
        dependsOn(installTasks)
    }

    if (target.name in cacheableTargetNames) {
        tasks.register("${targetName}Cache") {
            dependsOn(cacheTasks)

            group = BasePlugin.BUILD_GROUP
            description = "Builds the compilation cache for platform: $targetName"
        }
    }
}

val hostInstall by tasks.registering {
    dependsOn("${PlatformInfo.hostName}Install")
}

val hostCache by tasks.registering {
    dependsOn("${PlatformInfo.hostName}Cache")
}

val cache by tasks.registering {
    dependsOn(tasks.withType(KonanCacheTask::class.java))

    group = BasePlugin.BUILD_GROUP
    description = "Builds all the compilation caches"
}
