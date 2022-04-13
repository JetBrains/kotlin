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
    val isDynamic = when (cacheKindString) {
        "dynamic" -> true
        "static" -> false
        else -> error(cacheKindString)
    }

    val cacheKind = if (isDynamic) {
        CompilerOutputKind.DYNAMIC_CACHE
    } else {
        CompilerOutputKind.STATIC_CACHE
    }

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
    val cacheFile = "$cacheDir/stdlib-cache"
    val stdlib = "$dist/klib/common/stdlib"
    val compilerArgs = listOf("-Xcached-library=$stdlib,$cacheFile")

    val buildCacheTask = project.tasks.create(STDLIB_BUILD_CACHE_TASK_NAME, Exec::class.java) {
        doFirst {
            cacheDir.mkdirs()
        }

        dependsOnDist()

        commandLine(
                "$dist/bin/konanc",
                "-p", cacheKind.visibleName,
                "-o", "$cacheDir/stdlib-cache",
                "-Xmake-cache=$stdlib",
                "-no-default-libs", "-nostdlib",
                "-target", target,
                "-g"
        )
    }

    return CacheTesting(buildCacheTask, compilerArgs, isDynamic)
}

private const val STDLIB_BUILD_CACHE_TASK_NAME = "buildStdlibCache"
