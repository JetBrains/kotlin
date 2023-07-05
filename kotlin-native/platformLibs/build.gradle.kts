/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import org.jetbrains.kotlin.KonanKlibInstallTask
import org.jetbrains.kotlin.gradle.plugin.konan.tasks.KonanCacheTask
import org.jetbrains.kotlin.gradle.plugin.tasks.KonanInteropTask
import org.jetbrains.kotlin.konan.target.*
import org.jetbrains.kotlin.konan.util.*

// These properties are used by the 'konan' plugin, thus we set them before applying it.
val distDir: File by project
val konanHome: String by extra(distDir.absolutePath)
val jvmArgs: String by extra(
        mutableListOf<String>().apply {
            addAll(HostManager.defaultJvmArgs)
            add(project.findProperty("platformLibsJvmArgs") as? String ?: "-Xmx6G")
        }.joinToString(" ")
)

extra["org.jetbrains.kotlin.native.home"] = konanHome
extra["konan.jvmArgs"] = jvmArgs

plugins {
    id("konan")
}

val targetsWithoutZlib: List<KonanTarget> by project

// region: Util functions.
fun KonanTarget.defFiles() =
    project.fileTree("src/platform/${family.visibleName}")
            .filter { it.name.endsWith(".def") }
            // The libz.a/libz.so and zlib.h are missing in MIPS sysroots.
            // Just workaround it until we have sysroots corrected.
            .filterNot { (this in targetsWithoutZlib) && it.name == "zlib.def" }
            .map { DefFile(it, this) }


fun defFileToLibName(target: String, name: String) = "$target-$name"

// endregion

if (HostManager.host == KonanTarget.MACOS_ARM64) {
    project.configureJvmToolchain(JdkMajorVersion.JDK_17_0)
}

val konanTargetList: List<KonanTarget> by project
val targetList: List<String> by project
val cacheableTargets: List<KonanTarget> by project

konanTargetList.forEach { target ->
    val targetName = target.visibleName
    val installTasks = mutableListOf<TaskProvider<out Task>>()
    val cacheTasks = mutableListOf<TaskProvider<out Task>>()

    target.defFiles().forEach { df ->
        val libName = defFileToLibName(targetName, df.name)
        val fileNamePrefix = PlatformLibsInfo.namePrefix
        val artifactName = "${fileNamePrefix}${df.name}"

        konanArtifacts {
            interop(
                    args = mapOf("targets" to listOf(targetName)),
                    name = libName
            ) {
                df.file?.let { defFile(it) }
                artifactName(artifactName)
                noDefaultLibs(true)
                noEndorsedLibs(true)
                libraries {
                    klibs(df.config.depends.map { "${fileNamePrefix}${it}" })
                }
                extraOpts("-Xpurge-user-libs", "-Xshort-module-name", df.name, "-Xdisable-experimental-annotation")
                compilerOpts("-fmodules-cache-path=${project.buildDir}/clangModulesCache")
            }
        }

        @kotlin.Suppress("UNCHECKED_CAST")
        val libTask = konanArtifacts.getByName(libName).getByTarget(targetName) as TaskProvider<KonanInteropTask>
        libTask.configure {
            dependsOn(df.config.depends.map { defFileToLibName(targetName, it) })
            dependsOn(":kotlin-native:${targetName}CrossDist")

            enableParallel = project.findProperty("kotlin.native.platformLibs.parallel")?.toString()?.toBoolean() ?: true
        }

        val klibInstallTask = tasks.register(libName, KonanKlibInstallTask::class.java) {
            klib = libTask.map { it.artifact }
            repo = file("$konanHome/klib/platform/$targetName")
            this.target = targetName
            dependsOn(libTask)
        }
        installTasks.add(klibInstallTask)

        if (target in cacheableTargets) {
            val cacheTask = tasks.register("${libName}Cache", KonanCacheTask::class.java) {
                this.target = targetName
                originalKlib = klibInstallTask.get().installDir.get()
                klibUniqName = artifactName
                cacheRoot = file("$konanHome/klib/cache").absolutePath

                dependsOn(":kotlin-native:${targetName}StdlibCache")
                dependsOn(tasks.named(libName))
                dependsOn(df.config.depends.map {
                    val depName = defFileToLibName(targetName, it)
                    "${depName}Cache"
                })
            }
            cacheTasks.add(cacheTask)
        }
    }

    tasks.register("${targetName}Install") {
        dependsOn(installTasks)
    }

    if (target in cacheableTargets) {
        tasks.register("${targetName}Cache") {
            dependsOn(cacheTasks)

            group = BasePlugin.BUILD_GROUP
            description = "Builds the compilation cache for platform: $targetName"
        }
    }
}

val hostName: String by project

val hostInstall by tasks.registering {
    dependsOn("${hostName}Install")
}

val hostCache by tasks.registering {
    dependsOn("${hostName}Cache")
}

val cache by tasks.registering {
    dependsOn(tasks.withType(KonanCacheTask::class.java))

    group = BasePlugin.BUILD_GROUP
    description = "Builds all the compilation caches"
}
