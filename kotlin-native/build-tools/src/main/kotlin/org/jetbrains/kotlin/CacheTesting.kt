package org.jetbrains.kotlin

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Exec
import org.jetbrains.kotlin.konan.properties.resolvablePropertyList
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.util.visibleName

class CacheTesting(val buildCacheTask: Task, val compilerArgs: List<String>, val isDynamic: Boolean)

fun configureCacheTesting(project: Project): CacheTesting? {
    val cacheKindString = project.findProperty("test_with_cache_kind") as String? ?: return null
    val (cacheKind, makePerFileCache) = when (cacheKindString) {
        "dynamic" -> CompilerOutputKind.DYNAMIC_CACHE to false
        "static" -> CompilerOutputKind.STATIC_CACHE to false
        "static_per_file" -> CompilerOutputKind.STATIC_CACHE to true
        else -> error(cacheKindString)
    }
    val isDynamic = cacheKind == CompilerOutputKind.DYNAMIC_CACHE

    val target = project.testTarget
    val dist = project.kotlinNativeDist

    val cacheableTargets = Distribution(dist.absolutePath).properties
            .resolvablePropertyList("cacheableTargets", HostManager.hostName)
            .map { KonanTarget.predefinedTargets.getValue(it) }
            .toSet()

    check(target in cacheableTargets) {
        """
            No cache support for test target $target at host target ${HostManager.host}.
            $STDLIB_BUILD_CACHE_TASK_NAME Gradle task can't be created.
        """.trimIndent()

    }

    val cacheDir = project.file("${project.buildDir}/cache")
    val cacheFile = "$cacheDir/stdlib${if (makePerFileCache) "-per-file" else ""}-cache"
    val stdlib = "$dist/klib/common/stdlib"
    val compilerArgs = listOf("-Xcached-library=$stdlib,$cacheFile")

    val buildCacheTask = project.tasks.create(STDLIB_BUILD_CACHE_TASK_NAME, Exec::class.java) {
        doFirst {
            cacheDir.mkdirs()
        }

        dependsOnDist()

        val args = mutableListOf(
                "$dist/bin/konanc",
                "-p", cacheKind.visibleName,
                "-Xadd-cache=$stdlib", "-Xcache-directory=$cacheDir",
                "-no-default-libs", "-nostdlib",
                "-target", target,
                "-g"
        )
        if (makePerFileCache)
            args.add("-Xmake-per-file-cache")

        commandLine(args)
    }

    return CacheTesting(buildCacheTask, compilerArgs, isDynamic)
}

private const val STDLIB_BUILD_CACHE_TASK_NAME = "buildStdlibCache"
