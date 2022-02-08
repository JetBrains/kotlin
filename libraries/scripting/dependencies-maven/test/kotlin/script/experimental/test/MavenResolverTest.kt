/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.test

import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Ignore
import java.io.File
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.reflect.full.primaryConstructor
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.valueOrThrow
import kotlin.script.experimental.dependencies.*
import kotlin.script.experimental.dependencies.impl.DependenciesResolverOptionsName
import kotlin.script.experimental.dependencies.impl.SimpleExternalDependenciesResolverOptionsParser
import kotlin.script.experimental.dependencies.impl.makeExternalDependenciesResolverOptions
import kotlin.script.experimental.dependencies.impl.set
import kotlin.script.experimental.dependencies.maven.MavenDependenciesResolver

@ExperimentalContracts
class MavenResolverTest : ResolversTestBase() {

    private fun resolveAndCheck(
        coordinates: String,
        options: ExternalDependenciesResolver.Options = ExternalDependenciesResolver.Options.Empty,
        checkBody: (Iterable<File>) -> Boolean = { true }
    ): List<File> {
        contract {
            callsInPlace(checkBody, InvocationKind.EXACTLY_ONCE)
        }
        val resolver = MavenDependenciesResolver()
        val result = runBlocking { resolver.resolve(coordinates, options) }
        if (result is ResultWithDiagnostics.Failure) {
            Assert.fail(result.reports.joinToString("\n") { it.exception?.toString() ?: it.message })
        }
        val files = result.valueOrThrow()
        if (!checkBody(files)) {
            Assert.fail("Unexpected resolving results:\n  ${files.joinToString("\n  ")}")
        }
        return files
    }

    private fun buildOptions(vararg options: Pair<DependenciesResolverOptionsName, String>): ExternalDependenciesResolver.Options {
        return makeExternalDependenciesResolverOptions(mutableMapOf<String, String>().apply {
            for (option in options) this[option.first] = option.second
        })
    }

    private val resolvedKotlinVersion = "1.5.31"

    fun testResolveSimple() {
        resolveAndCheck("org.jetbrains.kotlin:kotlin-annotations-jvm:$resolvedKotlinVersion") { files ->
            files.any { it.name.startsWith("kotlin-annotations-jvm") }
        }
    }

    fun testResolveWithRuntime() {
        // Need a minimal library with an extra runtime dependency
        val lib = "org.jetbrains.kotlin:kotlin-util-io:$resolvedKotlinVersion"
        val compileOnlyFiles = resolveAndCheck(lib, buildOptions(DependenciesResolverOptionsName.SCOPE to "compile"))
        val compileRuntimeFiles = resolveAndCheck(lib, buildOptions(DependenciesResolverOptionsName.SCOPE to "compile,runtime"))

        assertTrue(
            "Compile only dependencies count should be less than compile + runtime\n" +
                    "${compileOnlyFiles.joinToString(prefix = "Compile dependencies:\n\t", separator = "\n\t")}\n" +
                    compileRuntimeFiles.joinToString(prefix = "Compile + Runtime dependencies:\n\t", separator = "\n\t"),
            compileOnlyFiles.count() < compileRuntimeFiles.count()
        )
    }

    fun testTransitiveOption() {
        val dependency = "junit:junit:4.11"

        var transitiveFiles: Iterable<File>
        fun parseOptions(options: String) = SimpleExternalDependenciesResolverOptionsParser(options).valueOrThrow()

        resolveAndCheck(dependency, options = parseOptions("transitive=true")) { files ->
            transitiveFiles = files
            true
        }

        var nonTransitiveFiles: Iterable<File>
        resolveAndCheck(dependency, options = parseOptions("transitive=false")) { files ->
            nonTransitiveFiles = files
            true
        }

        val tCount = transitiveFiles.count()
        val ntCount = nonTransitiveFiles.count()
        val artifact = nonTransitiveFiles.single()

        assertTrue(ntCount < tCount)
        assertEquals("jar", artifact.extension)
    }

    fun testResolveVersionsRange() {
        resolveAndCheck("org.jetbrains.kotlin:kotlin-annotations-jvm:(1.3.40,$resolvedKotlinVersion)")
    }

    fun testResolveDifferentType() {
        resolveAndCheck("org.javamoney:moneta:pom:1.3") { files ->
            files.any { it.extension == "pom" }
        }
    }

    // Ignored - tests with custom repos often break the CI due to the caching issues
    // TODO: find a way to enable it back
    @Ignore
    fun ignore_testAuth() {
        val resolver = MavenDependenciesResolver()
        val options = buildOptions(
            DependenciesResolverOptionsName.USERNAME to "<FirstName.LastName>",
            DependenciesResolverOptionsName.PASSWORD to "<Space token>",
        )
        resolver.addRepository("https://packages.jetbrains.team/maven/p/crl/maven/", options)
        val files = runBlocking {
            resolver.resolve("com.jetbrains:space-sdk:1.0-dev")
        }.valueOrThrow()
        assertTrue(files.any { it.name.startsWith("space-sdk") })
    }

    // Ignored - tests with custom repos often break the CI due to the caching issues
    // TODO: find a way to enable it back
    @Ignore
    fun ignore_testCustomRepositoryId() {
        val resolver = MavenDependenciesResolver()
        resolver.addRepository("https://repo.osgeo.org/repository/release/")
        val files = runBlocking {
            resolver.resolve("org.geotools:gt-shapefile:[23,)")
        }.valueOrThrow()
        assertTrue(files.any { it.name.startsWith("gt-shapefile") })
    }

    // Ignored - tests with custom repos often break the CI due to the caching issues
    // TODO: find a way to enable it back
    @Ignore
    fun ignore_testResolveFromAnnotationsWillResolveTheSameRegardlessOfAnnotationOrder() {
        val dependsOnConstructor = DependsOn::class.primaryConstructor!!
        val repositoryConstructor = Repository::class.primaryConstructor!!

        // @DepensOn("eu.jrie.jetbrains:kotlin-shell-core:0.2")
        val dependsOn = dependsOnConstructor.callBy(
            mapOf(
                dependsOnConstructor.parameters.first() to arrayOf("eu.jrie.jetbrains:kotlin-shell-core:0.2")
            )
        )

        val repositories = repositoryConstructor.callBy(
            mapOf(
                repositoryConstructor.parameters.first() to arrayOf(
                    "TODO - REWRITE TEST TO OTHER REPOSITORY: https://dl.bbiintray.com/jakubriegel/kotlin-shell"
                )
            )
        )

        val annotationsWithReposFirst = listOf(repositories, dependsOn)
        val annotationsWithDependsOnFirst = listOf(dependsOn, repositories)

        val filesWithReposFirst = runBlocking {
            MavenDependenciesResolver().resolveFromAnnotations(annotationsWithReposFirst)
        }.valueOrThrow()

        val filesWithDependsOnFirst = runBlocking {
            MavenDependenciesResolver().resolveFromAnnotations(annotationsWithDependsOnFirst)
        }.valueOrThrow()

        // Tests that the jar was resolved
        assert(
            filesWithReposFirst.any { it.name.startsWith("kotlin-shell-core-") && it.extension == "jar" }
        )
        assert(
            filesWithDependsOnFirst.any { it.name.startsWith("kotlin-shell-core-") && it.extension == "jar" }
        )

        // Test that the the same files are resolved regardless of annotation order
        assertEquals(filesWithReposFirst.map { it.name }.sorted(), filesWithDependsOnFirst.map { it.name }.sorted())
    }
}
