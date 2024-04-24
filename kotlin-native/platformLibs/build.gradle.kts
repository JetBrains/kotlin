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

// These properties are used by the 'konan' plugin, thus we set them before applying it.
val konanHome: String by extra(kotlinNativeDist.absolutePath)
val jvmArgs: String by extra(
        mutableListOf<String>().apply {
            addAll(HostManager.defaultJvmArgs)
            add(project.findProperty("platformLibsJvmArgs") as? String ?: "-Xmx6G")
        }.joinToString(" ")
)

extra["org.jetbrains.kotlin.native.home"] = konanHome
extra["konan.jvmArgs"] = jvmArgs

plugins {
    id("platform-manager")
    id("konan")
}

// region: Util functions.
fun KonanTarget.defFiles() =
        project.fileTree("src/platform/${family.visibleName}")
                .filter { it.name.endsWith(".def") }
                .map { DefFile(it, this) }


fun defFileToLibName(target: String, name: String) = "$target-$name"

// endregion

if (HostManager.host == KonanTarget.MACOS_ARM64) {
    project.configureJvmToolchain(JdkMajorVersion.JDK_17_0)
}

val cacheableTargetNames = platformManager.hostPlatform.cacheableTargets

enabledTargets(platformManager).forEach { target ->
    val targetName = target.visibleName
    val installTasks = mutableListOf<TaskProvider<out Task>>()
    val cacheTasks = mutableListOf<TaskProvider<out Task>>()

    // First register all interop-libraries
    target.defFiles().forEach { df ->
        val libName = defFileToLibName(targetName, df.name)
        val fileNamePrefix = PlatformLibsInfo.namePrefix
        val artifactName = "${fileNamePrefix}${df.name}"

        konanArtifacts {
            interop(args = mapOf("targets" to listOf(targetName)), name = libName) {
                df.file?.let { defFile(it) }
                artifactName(artifactName)
                noDefaultLibs(true)
                noEndorsedLibs(true)
                noPack(true)
                libraries {
                    klibFiles(df.config.depends.map { layout.buildDirectory.dir("konan/libs/$targetName/${fileNamePrefix}${it}") })
                }
                extraOpts("-Xpurge-user-libs", "-Xshort-module-name", df.name, "-Xdisable-experimental-annotation")
                compilerOpts("-fmodules-cache-path=${project.layout.buildDirectory.dir("clangModulesCache").get().asFile}")
            }
        }
    }

    // After all interop-libraries are registered, configure tasks (need to do it after all interop-libraries
    // are registered, because they cross-reference each other)
    target.defFiles().forEach { df ->
        val libName = defFileToLibName(targetName, df.name)
        val fileNamePrefix = PlatformLibsInfo.namePrefix
        val artifactName = "${fileNamePrefix}${df.name}"

        @Suppress("UNCHECKED_CAST")
        val libTask = konanArtifacts.getByName(libName).getByTarget(targetName) as TaskProvider<KonanInteropTask>
        libTask.configure {
            dependsOn(
                    df.config.depends.map {
                        val dependencyLibName = defFileToLibName(targetName, it)
                        konanArtifacts.getByName(dependencyLibName).getByTarget(targetName)
                    }
            )
            dependsOn(":kotlin-native:${targetName}CrossDist")

            enableParallel = project.findProperty("kotlin.native.platformLibs.parallel")?.toString()?.toBoolean() ?: true
        }

        val klibInstallTask = tasks.register(libName, Sync::class.java) {
            from(libTask.map { it.artifact })
            into("$konanHome/klib/platform/$targetName/$artifactName")
        }
        installTasks.add(klibInstallTask)

        if (target.name in cacheableTargetNames) {
            val cacheTask = tasks.register("${libName}Cache", KonanCacheTask::class.java) {
                notCompatibleWithConfigurationCache("project used in execution time")
                this.target = targetName
                originalKlib.fileProvider(libTask.map {
                    it.artifactDirectory ?: error("Artifact wasn't set for ${it.name}")
                })
                klibUniqName = artifactName
                cacheRoot = file("$konanHome/klib/cache").absolutePath

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
