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
import java.util.concurrent.Callable

internal sealed class SourceRoots(val kotlinSourceFiles: FileCollection) {
    private companion object {
        fun dumpPaths(files: Iterable<File>): String =
            "[${files.map { it.canonicalPath }.sorted().joinToString(prefix = "\n\t", separator = ",\n\t")}]"
    }

    open fun log(taskName: String, logger: Logger) {
        logger.kotlinDebug { "$taskName source roots: ${dumpPaths(kotlinSourceFiles.files)}" }
    }

    class ForJvm constructor(
        kotlinSourceFiles: FileCollection,
        val javaSourceRoots: FileCollection
    ) : SourceRoots(kotlinSourceFiles) {

        companion object {
            fun create(
                taskSource: FileTree,
                sourceRoots: FilteringSourceRootsContainer,
                sourceFilesExtensions: List<String>,
                fileCollectionFactory: (Set<File>) -> FileCollection
            ): ForJvm {
                val javaSourceFiles = taskSource.filter { it.isJavaFile() }.files
                val sourceRootsFiles = sourceRoots.sourceRoots.files
                val javaSourceRoots = sourceRootsFiles.filterJavaRoots(javaSourceFiles)

                return ForJvm(
                    taskSource.filter { it.isKotlinFile(sourceFilesExtensions) },
                    fileCollectionFactory(javaSourceRoots)
                )
            }

            private fun Set<File>.filterJavaRoots(
                sourceDirs: Set<File>
            ): Set<File> = filter { sourceRoot ->
                sourceDirs.map { it.parentFile }.any { sourceRoot.isParentOf(it) }
            }.toSet()
        }

        override fun log(taskName: String, logger: Logger) {
            super.log(taskName, logger)
            logger.kotlinDebug { "$taskName java source roots: ${dumpPaths(javaSourceRoots.files)}" }
        }
    }

    class KotlinOnly(kotlinSourceFiles: FileCollection) : SourceRoots(kotlinSourceFiles) {
        companion object {
            fun create(
                taskSource: FileTree,
                sourceFilesExtensions: List<String>
            ) = KotlinOnly(
                taskSource.filter { it.isKotlinFile(sourceFilesExtensions) }
            )
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