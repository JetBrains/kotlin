package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.file.FileTree
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.logging.Logger
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.gradle.plugin.kotlinDebug
import org.jetbrains.kotlin.incremental.isJavaFile
import org.jetbrains.kotlin.incremental.isKotlinFile
import java.io.File
import java.util.*

internal sealed class SourceRoots(val kotlinSourceFiles: List<File>) {
    private companion object {
        fun dumpPaths(files: Iterable<File>): String =
                "[${files.map { it.canonicalPath }.sorted().joinToString(prefix = "\n\t", separator = ",\n\t")}]"
    }

    open fun log(taskName: String, logger: Logger) {
        logger.kotlinDebug { "$taskName source roots: ${dumpPaths(kotlinSourceFiles)}" }
    }

    class ForJvm(kotlinSourceFiles: List<File>, val javaSourceRoots: Set<File>) : SourceRoots(kotlinSourceFiles) {
        companion object {
            fun create(taskSource: FileTree, sourceRoots: FilteringSourceRootsContainer): ForJvm {
                val kotlinSourceFiles = (taskSource as Iterable<File>).filter(File::isKotlinFile)
                val javaSourceRoots = findRootsForSources(
                        sourceRoots.sourceRoots, taskSource.filter(File::isJavaFile))
                return ForJvm(kotlinSourceFiles, javaSourceRoots)
            }

            private fun findRootsForSources(allSourceRoots: Iterable<File>, sources: Iterable<File>): Set<File> {
                val resultRoots = HashSet<File>()
                val sourceDirs = sources.mapTo(HashSet()) { it.parentFile }

                for (sourceDir in sourceDirs) {
                    for (sourceRoot in allSourceRoots) {
                        if (FileUtil.isAncestor(sourceRoot, sourceDir, /* strict = */false)) {
                            resultRoots.add(sourceRoot)
                        }
                    }
                }

                return resultRoots
            }
        }

        override fun log(taskName: String, logger: Logger) {
            super.log(taskName, logger)
            logger.kotlinDebug { "$taskName java source roots: ${dumpPaths(javaSourceRoots)}" }
        }
    }

    class KotlinOnly(kotlinSourceFiles: List<File>) : SourceRoots(kotlinSourceFiles) {
        companion object {
            fun create(taskSource: FileTree) = KotlinOnly((taskSource as Iterable<File>).filter(File::isKotlinFile))
        }
    }
}

internal class FilteringSourceRootsContainer(roots: List<File> = emptyList(), val filter: (File) -> Boolean = { true }) {
    private val mutableSourceRoots = roots.filterTo(mutableListOf(), filter)

    val sourceRoots: List<File>
        get() = mutableSourceRoots

    fun clear() {
        mutableSourceRoots.clear()
    }

    fun set(source: Any?): List<File> {
        clear()
        return add(source)
    }

    fun add(vararg sources: Any?): List<File> {
        val filteredDirs = mutableListOf<File>()
        for (source in sources) {
            when (source) {
                is SourceDirectorySet -> filteredDirs += source.srcDirs.filter { filter(it) }
                is File -> if (filter(source)) filteredDirs.add(source)
                is Collection<*> -> source.forEach { filteredDirs += add(it) }
                is Array<*> -> source.forEach { filteredDirs += add(it) }
            }
        }

        mutableSourceRoots += filteredDirs
        return filteredDirs
    }
}