/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.klib

import org.jetbrains.kotlin.backend.common.LegacyKlibDependencies
import org.jetbrains.kotlin.backend.konan.library.KlibDAGBuilder
import org.jetbrains.kotlin.backend.konan.library.KlibDAGCyclicDependencyException
import org.jetbrains.kotlin.backend.konan.library.KlibDAGNode
import org.jetbrains.kotlin.io.readProperties
import org.jetbrains.kotlin.io.writeProperties
import org.jetbrains.kotlin.konan.library.KlibNativeDistributionLibraryProvider
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.test.blackbox.AbstractNativeSimpleTest
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeHome
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeTargets
import org.jetbrains.kotlin.konan.test.blackbox.support.util.mapToSet
import org.jetbrains.kotlin.library.KLIB_PROPERTY_DEPENDS
import org.jetbrains.kotlin.library.KLIB_PROPERTY_UNIQUE_NAME
import org.jetbrains.kotlin.library.KlibMockDSL.Companion.generateRandomName
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.isNativeStdlib
import org.jetbrains.kotlin.library.loader.KlibLoader
import org.jetbrains.kotlin.library.loader.reportLoadingProblemsIfAny
import org.jetbrains.kotlin.library.metadata.isCInteropLibrary
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.io.File
import java.nio.file.Path
import kotlin.collections.set
import kotlin.io.path.absolutePathString
import kotlin.io.path.pathString

@Tag("klib")
class KlibDAGBuilderTest : AbstractNativeSimpleTest() {
    @Test
    fun `stdlib does not depend on anything`() {
        val libraries = loadLibraries()
        assertEquals(1, libraries.size)

        val stdlib = libraries[0]
        assertTrue(stdlib.isNativeStdlib)

        val dag = KlibDAGBuilder.build(libraries)
        assertEquals(1, dag.size)
        assertEquals(stdlib, dag.keys.single())
        assertEquals(stdlib, dag.values.single().library)
        assertTrue(dag.values.single().directDependencies.isEmpty())
        assertTrue(dag.values.single().allDependencies.isEmpty())
    }

    @Test
    fun `dependencies of platform libs correlate to what is written in their manifest (unique_name, depends)`() {
        val libraries = loadLibraries(platformLibs = true)
        assertTrue(libraries.size > 1)

        val stdlib = libraries[0]
        assertTrue(stdlib.isNativeStdlib)
        libraries.drop(1).forEach { platformLib -> assertTrue(platformLib.isCInteropLibrary()) }

        // Compute direct dependencies set using the data from manifest (unique_name, depends).
        val directDependenciesByManifest: Map<KotlinLibrary, Set<KotlinLibrary>> = computeDirectDependenciesSetByManifest(libraries)

        // Compute all dependencies set using the data from manifest (unique_name, depends).
        val allDependenciesByManifest: Map<KotlinLibrary, Set<KotlinLibrary>> = computeFullDependenciesSet(directDependenciesByManifest)

        // Sanity check:
        assertDependenciesCorrelate(
            denseDependenciesSet = allDependenciesByManifest,
            looseDependenciesSet = directDependenciesByManifest
        )

        // Now, compute the DAG of dependencies by signatures.
        val dag = KlibDAGBuilder.build(libraries)

        // Direct dependencies computed by signatures.
        val directDependenciesByDAGBuilder: Map<KotlinLibrary, Set<KotlinLibrary>> = dag.values.associate { node ->
            node.library to node.directDependencies
        }

        // All dependencies computed by signatures.
        val allDependenciesByDAGBuilder: Map<KotlinLibrary, Set<KotlinLibrary>> = dag.values.associate { node ->
            node.library to node.allDependencies
        }

        // Sanity check:
        assertDependenciesCorrelate(
            denseDependenciesSet = allDependenciesByDAGBuilder,
            looseDependenciesSet = directDependenciesByDAGBuilder
        )

        // Main checks (yes, it is expected that DAG computed by signatures shows a looser graph of depenencies):
        assertDependenciesCorrelate(
            denseDependenciesSet = directDependenciesByManifest,
            looseDependenciesSet = directDependenciesByDAGBuilder
        )
        assertDependenciesCorrelate(
            denseDependenciesSet = allDependenciesByManifest,
            looseDependenciesSet = allDependenciesByDAGBuilder
        )
    }

    @Test
    fun `dependencies of user libraries are computed correctly`() {
        // Define the user's project structure.
        // Note: stdlib & platform libraries are not reflected in this structure.
        val userProjectModules = newSourceModules {
            // Some regular (prefix 'R') modules:
            addRegularModule("RA")
            addRegularModule("RB") { dependsOn("RA") }
            addRegularModule("RC") { dependsOn("RB") }
            addRegularModule("RD") { dependsOn("RA") }
            addRegularModule("RE") { dependsOn("RC", "RD") }

            // Some C-interop (prefix 'I') modules:
            addCInteropModule("IA")
            addCInteropModule("IB") { dependsOn("IA") }
            addCInteropModule("IC") { dependsOn("IB") }
            addCInteropModule("ID") { dependsOn("IA") }
            addCInteropModule("IE") { dependsOn("IC", "ID") }

            // Few more modules:
            addRegularModule("Main") { dependsOn("RE", "IE") }
            addRegularModule("Test") { dependsOn("Main") }
        }

        // Compile all the KLIBs:
        val userLibraryPathToModuleName: MutableMap</* path of KLIB */ Path, /* name of test module */ String> = mutableMapOf()
        userProjectModules.compileToKlibsViaCli { module, successKlib ->
            userLibraryPathToModuleName[successKlib.resultingArtifact.klibFile.toPath()] = module.name
        }
        assertEquals(userProjectModules.modules.size, userLibraryPathToModuleName.size)

        // Spoil `depends` and `unique_name` in manifests to make this data unreliable:
        for (userLibraryPath in userLibraryPathToModuleName.keys) {
            val manifestPath = userLibraryPath.resolve("default/manifest")

            val manifest = manifestPath.readProperties()
            manifest[KLIB_PROPERTY_UNIQUE_NAME] = generateRandomName(20)
            manifest[KLIB_PROPERTY_DEPENDS] = (0..2).joinToString(separator = " ") { generateRandomName(20) }
            manifestPath.writeProperties(manifest)
        }

        // Load all libraries, including platform libraries:
        val allLibraries: List<KotlinLibrary> = loadLibraries(platformLibs = true, others = userLibraryPathToModuleName.keys)

        // Compute the DAG of dependencies by signatures.
        val dag = KlibDAGBuilder.build(allLibraries)

        val userLibraries: Map</* name of test module */ String, /* use library */ KotlinLibrary> = allLibraries.mapNotNull { library ->
            val moduleName = userLibraryPathToModuleName[library.path] ?: return@mapNotNull null
            moduleName to library
        }.toMap()
        assertEquals(userProjectModules.modules.size, userLibraries.size)

        fun assertUserLibraryDependencies(
            moduleName: String,
            expectedDirectDependencies: /* set of module names */ Set<String> = emptySet(),
            expectedAllDependencies: /* set of module names */ Set<String> = expectedDirectDependencies,
        ) {
            fun Set<KotlinLibrary>.excludeStdlib(): Set<KotlinLibrary> =
                mapNotNullTo(hashSetOf()) { library -> library.takeUnless { it.isNativeStdlib } }

            fun Set<KotlinLibrary>.toUserModuleNames(): Set<String> =
                mapToSet { userLibraryPathToModuleName.getValue(it.path) }

            val userLibrary = userLibraries.getValue(moduleName)
            val dagNode: KlibDAGNode = dag.getValue(userLibrary)

            val actualDirectDependencies = dagNode.directDependencies.excludeStdlib().toUserModuleNames()
            assertEquals(expectedDirectDependencies, actualDirectDependencies)

            val actualAllDependencies = dagNode.allDependencies.excludeStdlib().toUserModuleNames()
            assertEquals(expectedAllDependencies, actualAllDependencies)
        }

        assertUserLibraryDependencies(moduleName = "RA")

        assertUserLibraryDependencies(
            moduleName = "RB",
            expectedDirectDependencies = setOf("RA"),
        )

        assertUserLibraryDependencies(
            moduleName = "RC",
            expectedDirectDependencies = setOf("RB"),
            expectedAllDependencies = setOf("RA", "RB"),
        )

        assertUserLibraryDependencies(
            moduleName = "RD",
            expectedDirectDependencies = setOf("RA"),
        )

        assertUserLibraryDependencies(
            moduleName = "RE",
            expectedDirectDependencies = setOf("RC", "RD"),
            expectedAllDependencies = setOf("RA", "RB", "RC", "RD"),
        )

        assertUserLibraryDependencies(moduleName = "IA")

        assertUserLibraryDependencies(
            moduleName = "IB",
            expectedDirectDependencies = setOf("IA"),
        )

        assertUserLibraryDependencies(
            moduleName = "IC",
            expectedDirectDependencies = setOf("IB"),
            expectedAllDependencies = setOf("IB", "IA"),
        )

        assertUserLibraryDependencies(
            moduleName = "ID",
            expectedDirectDependencies = setOf("IA"),
        )

        assertUserLibraryDependencies(
            moduleName = "IE",
            expectedDirectDependencies = setOf("IC", "ID"),
            expectedAllDependencies = setOf("IA", "IB", "IC", "ID"),
        )

        assertUserLibraryDependencies(
            moduleName = "Main",
            expectedDirectDependencies = setOf("RE", "IE"),
            expectedAllDependencies = setOf("RA", "RB", "RC", "RD", "RE", "IA", "IB", "IC", "ID", "IE"),
        )

        assertUserLibraryDependencies(
            moduleName = "Test",
            expectedDirectDependencies = setOf("Main"),
            expectedAllDependencies = setOf("Main", "RA", "RB", "RC", "RD", "RE", "IA", "IB", "IC", "ID", "IE"),
        )
    }

    @Test
    fun `cycling dependency is an error`() {
        val moduleNameToLibraryPath: MutableMap</* name of test module */ String, /* path of KLIB */ Path> = mutableMapOf()

        /*
         * A <-- B <-- C
         */
        newSourceModules {
            addRegularModule("A")
            addRegularModule("B") { dependsOn("A") }
            addRegularModule("C") { dependsOn("B") }
        }.compileToKlibsViaCli { module, successKlib ->
            moduleNameToLibraryPath[module.name] = successKlib.resultingArtifact.klibFile.toPath()
        }
        assertEquals(3, moduleNameToLibraryPath.size)

        /*
         * Now adding one more edge between A and C:
         * +-- A <-- B <-- C <--+
         * |                    |
         * +--------------------+
         */
        newSourceModules {
            addRegularModule("A") {
                sourceFileAddend("fun A2() { C.C(0) }")
            }
        }.compileToKlibsViaCli(
            extraCliArgs = listOf("-l", moduleNameToLibraryPath["C"]!!.absolutePathString())
        ) { module, successKlib ->
            assertEquals("A", module.name)
            moduleNameToLibraryPath["A"] = successKlib.resultingArtifact.klibFile.toPath()
        }
        assertEquals(3, moduleNameToLibraryPath.size)

        val libraries = loadLibraries(stdlib = false, others = moduleNameToLibraryPath.values)

        val anyLibraryNode: KlibDAGNode = KlibDAGBuilder.build(libraries).values.first()
        anyLibraryNode.directDependencies // that should be successful

        try {
            anyLibraryNode.allDependencies // that should fail
            fail("DAG dependencies should have failed because of the cycle")
        } catch (_: KlibDAGCyclicDependencyException) {
            // OK
        }
    }

    /**
     * A helper to avoid boilerplate code for loading KLIBs.
     */
    private fun loadLibraries(
        stdlib: Boolean = true,
        platformLibs: Boolean = false,
        others: Collection<Path> = emptyList(),
    ): List<KotlinLibrary> {
        val loadingResult = KlibLoader {
            libraryProviders(
                KlibNativeDistributionLibraryProvider(nativeHome) {
                    runIf(stdlib) { withStdlib() }
                    runIf(platformLibs) { withPlatformLibs(currentTarget) }
                }
            )
            libraryPaths(others.map { it.pathString })
        }.load()

        loadingResult.reportLoadingProblemsIfAny { _, message -> fail { message } }
        assertFalse(loadingResult.hasProblems)

        return loadingResult.librariesStdlibFirst
    }

    private val nativeHome: File
        get() = testRunSettings.get<KotlinNativeHome>().dir

    private val currentTarget: KonanTarget
        get() = testRunSettings.get<KotlinNativeTargets>().testTarget

    /**
     * Note: This is suitable only for the libraries from the Kotlin/Native distribution,
     * where we can guarantee that `depends` in manifest of one library points exactly to `unique_name` in
     * manifest of another library (and such library exists), and this dependency is valid.
     *
     * We cannot guarantee this for user libraries.
     */
    private fun computeDirectDependenciesSetByManifest(libraries: Collection<KotlinLibrary>): Map<KotlinLibrary, Set<KotlinLibrary>> {
        val resolver = LegacyKlibDependencies(libraries)

        return buildMap {
            for (library in libraries) {
                this[library] = resolver.getDependenciesFor(library).toSet()
            }
        }
    }

    private fun computeFullDependenciesSet(directDependenciesSet: Map<KotlinLibrary, Set<KotlinLibrary>>): Map<KotlinLibrary, Set<KotlinLibrary>> {
        val result = hashMapOf<KotlinLibrary, Set<KotlinLibrary>>()

        fun recurse(library: KotlinLibrary): Set<KotlinLibrary> {
            result[library]?.let { return it }

            val directDependencies = directDependenciesSet.getValue(library)

            return buildSet {
                addAll(directDependencies)
                directDependencies.forEach { directDependency -> addAll(recurse(directDependency)) }
            }
        }

        for (library in directDependenciesSet.keys) {
            result[library] = recurse(library)
        }

        return result
    }

    /**
     * Check that the two sets of dependencies correlate between each other.
     *
     * @param denseDependenciesSet The set of dependencies computed in a more dense way. I.e. there may be excessive dependencies because,
     *   for example, of excessive/unnecessary edges specified in `depends` in manifest files if this set of dependencies was computed
     *   by the manifest data. Or because this set of dependencies includes all transitive dependencies, while [looseDependenciesSet] does not.
     * @param looseDependenciesSet The set of dependencies computed in a more loose way. Normally, there are less dependency libraries for
     *   a library than in [denseDependenciesSet]. However, this set of dependencies should agree (= not contradict) with [denseDependenciesSet].
     */
    private fun assertDependenciesCorrelate(
        denseDependenciesSet: Map<KotlinLibrary, Set<KotlinLibrary>>,
        looseDependenciesSet: Map<KotlinLibrary, Set<KotlinLibrary>>,
    ) {
        assertEquals(denseDependenciesSet.keys, looseDependenciesSet.keys)

        for (library in denseDependenciesSet.keys) {
            val denseDependencies: Set<KotlinLibrary> = denseDependenciesSet.getValue(library)
            val looseDependencies: Set<KotlinLibrary> = looseDependenciesSet.getValue(library)

            assertTrue(denseDependencies.containsAll(looseDependencies)) {
                buildString {
                    appendLine("Uncorrelated dependencies for library ${library.path}:")
                    appendLine()
                    appendLine("Dense dependencies: (${denseDependencies.size}):")
                    denseDependencies.map { it.path.pathString }.sorted().forEach { append("- ").appendLine(it) }
                    appendLine()
                    appendLine("Loose dependencies: (${looseDependencies.size}):")
                    looseDependencies.map { it.path.pathString }.sorted().forEach { append("- ").appendLine(it) }
                }
            }
        }
    }
}
