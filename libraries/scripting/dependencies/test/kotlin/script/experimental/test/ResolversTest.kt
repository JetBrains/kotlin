/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.test

import java.io.File
import kotlin.contracts.ExperimentalContracts
import kotlin.script.experimental.dependencies.*
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.dependencies.impl.makeResolveFailureResult

@ExperimentalContracts
class ResolversTest : ResolversTestBase() {

    private fun <T> withTempFile(body: (file: File) -> T): T {
        createTempFile()
        val file = createTempFile()
        file.deleteOnExit()
        try {
            return body(file)
        } finally {
            file.delete()
        }
    }

    private fun getNonExistingFile() = withTempFile { it }.also { assertFalse(it.exists()) }

    fun testFileSystemResolver() {
        withTempFile { file ->

            val resolver = FileSystemDependenciesResolver()

            resolver.assertNotResolve(1, file.name)
            resolver.assertResolve(file, file.canonicalPath)

            val dir = file.parent!!
            resolver.assertAcceptsRepository(dir)
            resolver.addRepository(dir)

            resolver.assertResolve(file, file.name)
        }
    }

    fun testFileSystemResolverFail() {
        val file = getNonExistingFile()

        val resolver = FileSystemDependenciesResolver()
        resolver.assertAcceptsArtifact(file.path)
        resolver.assertNotResolve(1, file.path)

        resolver.addRepository(file.parent)

        resolver.assertNotResolve(2, file.name)
        resolver.assertNotResolve(2, file.absolutePath)

        resolver.addRepository(file.parentFile.parent)
        resolver.assertNotResolve(3, file.path)
    }

    fun testFileSystemAcceptsLinuxPath() {
        val resolver = FileSystemDependenciesResolver()
        resolver.assertAcceptsArtifact("/usr/local/bin/temp")
    }

    fun testFileSystemAcceptsWindowsPath() {
        val resolver = FileSystemDependenciesResolver()
        resolver.assertAcceptsArtifact("C:\\temp\\myfile")
    }

    fun testFileSystemNotAcceptsMavenPath() {
        val resolver = FileSystemDependenciesResolver()
        resolver.assertNotAcceptsArtifact("  ")
    }

    class TestDependenciesResolver(
        val acceptsArt: (String) -> Boolean,
        val doResolve: (String) -> File?,
        val acceptsRepo: (String) -> Boolean,
        val addRepo: (String) -> Unit
    ) : ExternalDependenciesResolver {

        override fun acceptsArtifact(artifactCoordinates: String): Boolean = acceptsArt(artifactCoordinates)

        override suspend fun resolve(artifactCoordinates: String): ResultWithDiagnostics<List<File>> {
            if (!acceptsArtifact(artifactCoordinates)) throw Exception("Path is invalid")
            val file = doResolve(artifactCoordinates) ?: return makeResolveFailureResult("Failed to resolve '$artifactCoordinates'")
            return ResultWithDiagnostics.Success(listOf(file))
        }

        override fun addRepository(repositoryCoordinates: RepositoryCoordinates) {
            addRepo(repositoryCoordinates.string)
        }

        override fun acceptsRepository(repositoryCoordinates: RepositoryCoordinates): Boolean {
            return acceptsRepo(repositoryCoordinates.string)
        }
    }

    fun testCompoundResolver() {
        val file1 = getNonExistingFile()
        val file2 = getNonExistingFile()
        val resolver1 = TestDependenciesResolver(acceptsArt = { it.startsWith("file") },
                                                 doResolve = { if (it == "file1") file1 else null },
                                                 acceptsRepo = { false },
                                                 addRepo = {})

        var prefix: String? = null
        val resolver2 = TestDependenciesResolver(acceptsArt = { a -> prefix?.let { a.startsWith(it) } ?: false },
                                                 doResolve = { if (it.contains(".")) file2 else null },
                                                 acceptsRepo = { true },
                                                 addRepo = { prefix = it })

        val resolver = CompoundDependenciesResolver(resolver1, resolver2)

        resolver.assertNotResolve(1, "abc")
        resolver.assertNotResolve(1, "file2")
        resolver.assertResolve(file1, "file1")
        resolver.addRepository("fil")
        resolver.assertNotResolve(2, "file2")
        resolver.assertResolve(file2, "file2.txt")
        resolver.assertResolve(file1, "file1")
        resolver.assertResolve(file2, "fil1.txt")
    }
}
