package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.logging.Logger
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.logging.kotlinDebug
import org.jetbrains.kotlin.gradle.utils.isJavaFile
import org.jetbrains.kotlin.gradle.utils.isKotlinFile
import org.jetbrains.kotlin.gradle.utils.isParentOf
import java.io.File
import java.util.*
import java.util.concurrent.Callable

internal sealed class SourceRoots(val kotlinSourceFiles: List<File>) {
    private companion object {
        fun dumpPaths(files: Iterable<File>): String =
            "[${files.map { it.canonicalPath }.sorted().joinToString(prefix = "\n\t", separator = ",\n\t")}]"
    }

    open fun log(taskName: String, logger: Logger) {
        logger.kotlinDebug { "$taskName source roots: ${dumpPaths(kotlinSourceFiles)}" }
    }

    class ForJvm constructor(
        kotlinSourceFiles: List<File>,
        private val javaSourceRootsProvider: () -> Set<File>
    ) : SourceRoots(kotlinSourceFiles) {
        val javaSourceRoots: Set<File>
            get() = javaSourceRootsProvider()

        constructor(kotlinSourceFiles: List<File>, allSourceRoots: FileCollection, taskSource: FileCollection) : this(kotlinSourceFiles, {
            findRootsForSources(allSourceRoots, taskSource.filter(File::isJavaFile)).toSet()
        })

        companion object {
            fun create(taskSource: FileTree, sourceRoots: FilteringSourceRootsContainer, sourceFilesExtensions: List<String>): ForJvm {
                val kotlinSourceFiles = (taskSource as Iterable<File>).filter { it.isKotlinFile(sourceFilesExtensions) }
                return ForJvm(kotlinSourceFiles, sourceRoots.sourceRoots, taskSource)
            }

            private fun findRootsForSources(allSourceRoots: Iterable<File>, sources: Iterable<File>): Set<File> {
                val resultRoots = HashSet<File>()
                val sourceDirs = sources.mapTo(HashSet()) { it.parentFile }

                for (sourceDir in sourceDirs) {
                    for (sourceRoot in allSourceRoots) {
                        if (sourceRoot.isParentOf(sourceDir)) {
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
            fun create(taskSource: FileTree, sourceFilesExtensions: List<String>) =
                KotlinOnly((taskSource as Iterable<File>).filter { it.isKotlinFile(sourceFilesExtensions) })
        }
    }
}

internal class FilteringSourceRootsContainer(
    private val objectFactory: ObjectFactory,
    val filter: (File) -> Boolean = { true }
) {
    private val sourceContainers: MutableList<Any> = mutableListOf()

    private fun getFilteredSourceRootsFrom(any: Any) = objectFactory.fileCollection().from(Callable {
        val resultItems = mutableListOf<Any>()
        fun getRootsFrom(item: Any?) {
            when (item) {
                is SourceDirectorySet -> resultItems.add(item.sourceDirectories)
                is Callable<*> -> getRootsFrom(item.call())
                is Provider<*> -> if (item.isPresent) getRootsFrom(item.get())
                is FileCollection -> resultItems.add(item)
                is Iterable<*> -> item.forEach { getRootsFrom(it) }
                is Array<*> -> item.forEach { getRootsFrom(it) }
                is Any /* not null */ -> resultItems.add(item)
            }
        }
        getRootsFrom(any)
        resultItems
    }).filter(filter)

    val sourceRoots: FileCollection
        get() = getFilteredSourceRootsFrom(sourceContainers)

    fun clear() {
        sourceContainers.clear()
    }

    fun set(source: Any): FileCollection {
        clear()
        return add(source)
    }

    fun add(vararg sources: Any): FileCollection {
        sourceContainers.addAll(sources.toList())
        return getFilteredSourceRootsFrom(sources)
    }
}