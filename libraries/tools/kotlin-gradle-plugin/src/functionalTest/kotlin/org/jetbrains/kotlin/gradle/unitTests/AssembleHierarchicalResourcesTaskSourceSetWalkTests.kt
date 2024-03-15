/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UNCHECKED_CAST")

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.plugin.mpp.resources.AssembleHierarchicalResourcesTask
import org.jetbrains.kotlin.utils.addToStdlib.popLast
import org.junit.Test
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.test.assertEquals

class AssembleHierarchicalResourcesTaskSourceSetWalkTests {

    @Test
    fun `test simple resource root`() {
        assertEquals(
            AssembleHierarchicalResourcesTask.SourceSetWalk.Result.ToCopy(
                listOf(
                    absolutePath("commonMain")
                )
            ),
            directoriesToCopy(
                hashMapOf(
                    "commonMain" to mapOf(
                        "res" to mapOf(
                            "foo" to null,
                        )
                    )
                ),
                listOf(
                    listOf(
                        absolutePath("commonMain")
                    )
                )
            ),
        )
    }

    @Test
    fun `test collision on the top level`() {
        assertEquals(
            AssembleHierarchicalResourcesTask.SourceSetWalk.Result.Collision(
                absolutePath("commonMain2", "res", "collision").toFile(),
                absolutePath("commonMain", "res", "collision").toFile(),
            ),
            directoriesToCopy(
                hashMapOf(
                    "commonMain" to mapOf(
                        "res" to mapOf(
                            "collision" to null,
                        )
                    ),
                    "commonMain2" to mapOf(
                        "res" to mapOf(
                            "collision" to null,
                        )
                    ),
                ),
                listOf(
                    listOf(
                        absolutePath("commonMain"),
                        absolutePath("commonMain2"),
                    )
                )
            ),
        )
    }

    @Test
    fun `test collision across levels`() {
        assertEquals(
            AssembleHierarchicalResourcesTask.SourceSetWalk.Result.ToCopy(
                listOf(
                    absolutePath("iosMain"),
                    absolutePath("commonMain"),
                )
            ),
            directoriesToCopy(
                hashMapOf(
                    "commonMain" to mapOf(
                        "res" to mapOf(
                            "collision" to null,
                        )
                    ),
                    "iosMain" to mapOf(
                        "res" to mapOf(
                            "collision" to null,
                        )
                    )
                ),
                listOf(
                    listOf(
                        absolutePath("iosMain"),
                    ),
                    listOf(
                        absolutePath("commonMain"),
                    ),
                )
            ),
        )
    }

    @Test
    fun `test collision only in directories`() {
        assertEquals(
            AssembleHierarchicalResourcesTask.SourceSetWalk.Result.ToCopy(
                listOf(
                    absolutePath("commonMain"),
                    absolutePath("commonMain2"),
                )
            ),
            directoriesToCopy(
                hashMapOf(
                    "commonMain" to mapOf(
                        "res" to mapOf(
                            "foo" to null,
                        )
                    ),
                    "commonMain2" to mapOf(
                        "res" to mapOf(
                            "bar" to null,
                        )
                    ),
                ),
                listOf(
                    listOf(
                        absolutePath("commonMain"),
                        absolutePath("commonMain2"),
                    )
                )
            ),
        )
    }

    @Test
    fun `test collision at higher level`() {
        assertEquals(
            AssembleHierarchicalResourcesTask.SourceSetWalk.Result.Collision(
                absolutePath("commonMain2", "res", "collision").toFile(),
                absolutePath("commonMain", "res", "collision").toFile(),
            ),
            directoriesToCopy(
                hashMapOf(
                    "commonMain" to mapOf(
                        "res" to mapOf(
                            "collision" to null,
                        )
                    ),
                    "commonMain2" to mapOf(
                        "res" to mapOf(
                            "collision" to null,
                        )
                    ),
                    "iosMain" to mapOf(
                        "res" to mapOf(
                            "collision" to null,
                        )
                    ),
                ),
                listOf(
                    listOf(
                        absolutePath("iosMain")
                    ),
                    listOf(
                        absolutePath("commonMain"),
                        absolutePath("commonMain2"),
                    )
                )
            ),
        )
    }

    @Test
    fun `test non-existent directories`() {
        assertEquals(
            AssembleHierarchicalResourcesTask.SourceSetWalk.Result.ToCopy(
                emptyList()
            ),
            directoriesToCopy(
                hashMapOf(),
                listOf(
                    listOf(
                        absolutePath("commonMain")
                    ),
                )
            ),
        )
    }

    @Test
    fun `test empty directories`() {
        assertEquals(
            AssembleHierarchicalResourcesTask.SourceSetWalk.Result.ToCopy(
                listOf(
                    absolutePath("commonMain")
                )
            ),
            directoriesToCopy(
                hashMapOf(
                    "commonMain" to emptyMap<String, Any>()
                ),
                listOf(
                    listOf(
                        absolutePath("commonMain")
                    ),
                )
            ),
        )
    }

    @Test
    fun `test not a directory`() {
        assertEquals(
            AssembleHierarchicalResourcesTask.SourceSetWalk.Result.NotDirectory(
                absolutePath("commonMain").toFile(),
            ),
            directoriesToCopy(
                hashMapOf(
                    "commonMain" to null,
                ),
                listOf(
                    listOf(
                        absolutePath("commonMain")
                    ),
                )
            ),
        )
    }

    private fun absolutePath(first: String, vararg next: String): Path {
        return Paths.get("").toAbsolutePath().root.resolve(Paths.get(first, *next))
    }

    private fun directoriesToCopy(
        fileSystem: HashMap<String, Any?>,
        resourcesSplitByLevel: List<List<Path>>,
    ): AssembleHierarchicalResourcesTask.SourceSetWalk.Result<List<Path>> {
        val fakeFs = buildFakeFileSystem(contents = fileSystem)
        return AssembleHierarchicalResourcesTask.SourceSetWalk<File, Path>(
            fileSystem = object : AssembleHierarchicalResourcesTask.FileSystem<File> {
                override fun walk(root: File): Sequence<File> = fakeFs.walkFileSystemFrom(root.toPath()).map {
                    it.path.toFile()
                }
                override fun exists(file: File): Boolean = fakeFs.exists(file.toPath())
                override fun isDirectory(file: File): Boolean = fakeFs.fileSystemAt(file.toPath()) is FakeFileSystem.FakeDirectory
            },
            basePathFromResource = { it },
            fileTreeToCopyFromResource = { it.toPath() }
        ).directoriesToCopy(
            resourcesSplitByLevel.map { it.map { it.toFile() } },
        )
    }

    fun buildFakeFileSystem(
        path: Path = Paths.get("").toAbsolutePath().root,
        contents: Map<String, Any?>
    ): FakeFileSystem {
        return FakeFileSystem.FakeDirectory(
            path,
            contents.mapValues {
                val value = it.value
                return@mapValues when (value) {
                    is Map<*, *> -> buildFakeFileSystem(
                        path.resolve(it.key),
                        value as Map<String, Any>,
                    )
                    null -> FakeFileSystem.FakeFile(path.resolve(it.key))
                    else -> error("Unexpected component ${value} in fake fs")
                }
            }
        )
    }

    sealed class FakeFileSystem(val path: Path) {
        class FakeFile(path: Path) : FakeFileSystem(path)
        class FakeDirectory(path: Path, val contents: Map<String, FakeFileSystem>) : FakeFileSystem(path)

        fun fileSystemAt(path: Path): FakeFileSystem? {
            if (this is FakeFile) error("File has no sub filesystem")
            (this as FakeDirectory)

            val components: MutableList<String> = path.map { it.fileName?.toString().orEmpty() }.reversed().toMutableList()
            var current = contents
            var value: FakeFileSystem? = null

            while (components.isNotEmpty()) {
                value = current[components.popLast()]

                when (value) {
                    is FakeDirectory -> current = value.contents
                    is FakeFile -> if (components.isNotEmpty()) return null
                    null -> return null
                }
            }
            return value
        }

        fun subdirectoryAt(path: Path): FakeDirectory {
            when (val directory = fileSystemAt(path)) {
                is FakeDirectory -> return directory
                is FakeFile -> error("Path $path is a file")
                null -> error("Subdirectory $path doesn't exist")
            }
        }

        fun exists(path: Path): Boolean = fileSystemAt(path) != null

        fun walkFileSystemFrom(path: Path): Sequence<FakeFileSystem> {
            return sequence {
                walkFileSystem(subdirectoryAt(path))
            }
        }

        private suspend fun SequenceScope<FakeFileSystem>.walkFileSystem(
            file: FakeFileSystem,
        ) {
            if (file is FakeFile) return yield(file)
            (file as FakeDirectory)
            yield(file)
            file.contents.values.forEach {
                walkFileSystem(it)
            }
        }
    }

}