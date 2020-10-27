package org.jetbrains.kotlin

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Exec
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
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

    val cacheDir = project.file("${project.buildDir}/cache")
    val cacheFile = "$cacheDir/stdlib-cache"
    val dist = project.kotlinNativeDist
    val stdlib = "$dist/klib/common/stdlib"
    val compilerArgs = listOf("-Xcached-library=$stdlib,$cacheFile")

    val buildCacheTask = project.tasks.create("buildStdlibCache", Exec::class.java) {
        it.doFirst {
            cacheDir.mkdirs()
        }

        if (!(project.property("useCustomDist") as Boolean)) {
            val tasks = listOf(
                    "${target}CrossDist",
                    "${target}CrossDistRuntime",
                    "distCompiler"
            ).map { task -> project.rootProject.tasks.getByName(task) }

            it.dependsOn(tasks)
        }

        it.commandLine(
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