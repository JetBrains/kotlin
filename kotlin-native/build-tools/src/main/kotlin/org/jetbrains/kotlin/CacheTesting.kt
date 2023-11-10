package org.jetbrains.kotlin

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.konan.properties.resolvablePropertyList
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget

class CacheTesting(val buildCacheTask: TaskProvider<Task>, val compilerArgs: List<String>, val isDynamic: Boolean)

fun configureCacheTesting(project: Project): CacheTesting? {
    val cacheKindString = project.findProperty("test_with_cache_kind") as String? ?: return null
    val (cacheKind, makePerFileCache) = when (cacheKindString) {
        "dynamic" -> CompilerOutputKind.DYNAMIC_CACHE to false
        "static", "static_everywhere" -> CompilerOutputKind.STATIC_CACHE to false
        "static_per_file" -> CompilerOutputKind.STATIC_CACHE to true
        else -> error(cacheKindString)
    }

    val target = project.testTarget
    val distribution = Distribution(project.kotlinNativeDist.absolutePath)
    val cacheableTargets = distribution.properties
            .resolvablePropertyList("cacheableTargets", HostManager.hostName)
            .map { KonanTarget.predefinedTargets.getValue(it) }
            .toSet()

    check(target in cacheableTargets) {
        "No cache support for test target $target at host target ${HostManager.host}"
    }

    val cacheDir = "${distribution.klib}/cache/$target-gSTATIC"
    val cacheFile = "$cacheDir/stdlib${if (makePerFileCache) "-per-file" else ""}-cache"
    val stdlib = distribution.stdlib

    return CacheTesting(
            buildCacheTask = project.project(":kotlin-native").tasks.named("${target}StdlibCache"),
            compilerArgs = listOf("-Xcached-library=$stdlib,$cacheFile"),
            isDynamic = cacheKind == CompilerOutputKind.DYNAMIC_CACHE
    )
}
