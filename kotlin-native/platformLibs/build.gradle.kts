/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import org.gradle.kotlin.dsl.support.serviceOf
import org.jetbrains.kotlin.gradle.plugin.konan.tasks.KonanCacheTask
import org.jetbrains.kotlin.gradle.plugin.tasks.KonanInteropTask
import org.jetbrains.kotlin.PlatformInfo
import org.jetbrains.kotlin.kotlinNativeDist
import org.jetbrains.kotlin.konan.target.*
import org.jetbrains.kotlin.konan.util.*
import org.jetbrains.kotlin.platformManager
import org.jetbrains.kotlin.utils.capitalized
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Paths

plugins {
    id("base")
    id("platform-manager")
}

// region: Util functions.
fun KonanTarget.defFiles() = family.defFiles().map { DefFile(it, this) }

fun Family.defFiles() = project.fileTree("src/platform/${visibleName}")
        .filter { it.name.endsWith(".def") }

fun defFileToLibName(target: String, name: String) = "$target-$name"

private fun interopTaskName(libName: String, targetName: String) = "compileKonan${libName.capitalized}${targetName.capitalized}"

// endregion

if (HostManager.host == KonanTarget.MACOS_ARM64) {
    project.configureJvmToolchain(JdkMajorVersion.JDK_17_0)
}

val cacheableTargetNames = platformManager.hostPlatform.cacheableTargets

val defFileUpdates = mapOf(
        Family.IOS to listOf(KonanTarget.IOS_ARM64, KonanTarget.IOS_SIMULATOR_ARM64, KonanTarget.IOS_X64),
        Family.OSX to listOf(KonanTarget.MACOS_ARM64, KonanTarget.MACOS_X64),
        Family.WATCHOS to listOf(KonanTarget.WATCHOS_ARM32, KonanTarget.WATCHOS_ARM64, KonanTarget.WATCHOS_DEVICE_ARM64, KonanTarget.WATCHOS_SIMULATOR_ARM64, KonanTarget.WATCHOS_X64),
        Family.TVOS to listOf(KonanTarget.TVOS_ARM64, KonanTarget.TVOS_SIMULATOR_ARM64, KonanTarget.TVOS_X64),
).mapValues {
    val family = it.key
    val targets = it.value
    tasks.register("${family.visibleName}UpdateDefFileDependencies") {
        dependsOn(":kotlin-native:dist")
        val defFiles = family.defFiles().sorted()
        val hashedDefFiles = layout.buildDirectory.file("${family.visibleName}DefFileHashes")
        inputs.files(defFiles)
        inputs.file(
                providers.environmentVariable("KONAN_USE_INTERNAL_SERVER").orElse("").map {
                    if (it.isNotEmpty()) {
                        layout.projectDirectory.file("../konan/konan.properties").asFile
                    } else {
                        Files.readSymbolicLink(Paths.get("/var/db/xcode_select_link")).parent.resolve("version.plist").toFile()
                    }
                }
        )
        outputs.file(hashedDefFiles)
        val execOperations = serviceOf<ExecOperations>()
        val logger = logger
        val runKonan = File(kotlinNativeDist.absolutePath).resolve("bin/run_konan")
        val failIfDefFilesChanged = project.kotlinBuildProperties.isTeamcityBuild && !project.kotlinBuildProperties.getBoolean("kotlin.native.ignore-def-file-changes", false)
        doLast {
            val updateDefFileDependencies = {
                execOperations.exec {
                    commandLine(runKonan, "defFileDependencies", *targets.flatMap { listOf("-target", it.name) }.toTypedArray(), *defFiles.map { it.path }.toTypedArray())
                }
            }
            if (failIfDefFilesChanged) {
                val initialDefFiles = mutableMapOf<File, String>()
                defFiles.forEach {
                    initialDefFiles[it] = it.readText()
                }
                updateDefFileDependencies()
                val changedDefFiles = mutableListOf<File>()
                defFiles.sorted().forEach {
                    val finalContent = it.readText()
                    if (initialDefFiles[it] != finalContent) {
                        changedDefFiles.add(it)
                    }
                }
                if (changedDefFiles.isNotEmpty()) {
                    changedDefFiles.forEach { file ->
                        logger.error("Def file $file changed:")
                        execOperations.exec {
                            commandLine("/usr/bin/diff", "/dev/stdin", file.path)
                            standardInput = initialDefFiles[file]!!.encodeToByteArray().inputStream()
                            setIgnoreExitValue(true)
                        }
                    }
                    error("Changes in def files: $changedDefFiles")
                }
            } else {
                updateDefFileDependencies()
            }
            execOperations.exec {
                commandLine("/usr/bin/shasum", *defFiles.map { it.path }.toTypedArray())
                standardOutput = FileOutputStream(hashedDefFiles.get().asFile)
            }
        }
    }
}

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
            defFileUpdates[target.family]?.let { dependsOn(it) }

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
