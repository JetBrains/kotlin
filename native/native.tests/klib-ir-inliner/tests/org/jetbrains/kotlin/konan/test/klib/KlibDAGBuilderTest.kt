/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.klib

import org.jetbrains.kotlin.backend.konan.library.InternalKlibDAGApi
import org.jetbrains.kotlin.backend.konan.library.KlibDAG
import org.jetbrains.kotlin.backend.konan.library.KlibDAGBuilder
import org.jetbrains.kotlin.backend.konan.library.KlibDAGCyclicDependencyException
import org.jetbrains.kotlin.backend.konan.library.KlibDAGNode
import org.jetbrains.kotlin.backend.konan.library.deserialize
import org.jetbrains.kotlin.io.readProperties
import org.jetbrains.kotlin.io.writeProperties
import org.jetbrains.kotlin.konan.library.KlibNativeDistributionLibraryProvider
import org.jetbrains.kotlin.konan.library.SerializedKlibDAG
import org.jetbrains.kotlin.konan.library.isExplicitlySpecifiedByUserInCLIArgument
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
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.fail
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.io.File
import java.nio.file.Path
import kotlin.collections.set
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.name
import kotlin.io.path.pathString
import kotlin.io.path.relativeTo

@Tag("klib")
@Execution(ExecutionMode.SAME_THREAD)
class KlibDAGBuilderTest : AbstractNativeSimpleTest() {
    @ParameterizedTest
    @EnumSource
    fun `stdlib does not depend on anything`(mode: KlibDAGBuildingMode) = context(mode) {
        val libraries = loadLibraries()
        assertEquals(1, libraries.size)

        val stdlib = libraries[0]
        assertTrue(stdlib.isNativeStdlib)

        val dag = KlibDAGBuilder(buildParams(libraries)).build()
        assertEquals(1, dag.librariesReverseTopoSorted.size)
        assertEquals(stdlib, dag.librariesReverseTopoSorted.single())

        val stdlibNode = dag[dag.librariesReverseTopoSorted.single()]
        assertEquals(stdlib, stdlibNode.library)
        assertTrue(stdlibNode.directDependencies.isEmpty())
        assertTrue(stdlibNode.allDependencies.isEmpty())

        // Check serialization/deserialization.
        assertLosslessDeserialization(dag)
    }

    @ParameterizedTest
    @EnumSource
    fun `dependencies of user libraries are computed correctly (full DAG)`(mode: KlibDAGBuildingMode) =
        doTestDependenciesOfUserLibrariesComputedCorrectly(contractedDag = false, mode) { fail("Should not be called") }

    @ParameterizedTest
    @EnumSource
    fun `dependencies of user libraries are computed correctly (contracted DAG, roots = passed via CLI args)`(mode: KlibDAGBuildingMode) =
        doTestDependenciesOfUserLibrariesComputedCorrectly(contractedDag = true, mode) { isExplicitlySpecifiedByUserInCLIArgument }

    @ParameterizedTest
    @EnumSource
    fun `dependencies of user libraries are computed correctly (contracted DAG, root = test module)`(mode: KlibDAGBuildingMode) =
        doTestDependenciesOfUserLibrariesComputedCorrectly(contractedDag = true, mode) { path.last().toString() == "Test" }

    private fun doTestDependenciesOfUserLibrariesComputedCorrectly(
        contractedDag: Boolean,
        mode: KlibDAGBuildingMode,
        isRoot: KotlinLibrary.() -> Boolean,
    ) = context(mode) {
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
        val dag = KlibDAGBuilder(buildParams(allLibraries) { !contractedDag || isRoot(it) }).build()

        if (contractedDag) {
            // Only the necessary (used) libraries should be present in the DAG.
            assertEquals(userProjectModules.modules.size + /* stdlib */ 1, dag.librariesReverseTopoSorted.size)
        } else {
            // All libraries should be present in the DAG.
            assertEquals(allLibraries.size, dag.librariesReverseTopoSorted.size)
        }

        // Check serialization/deserialization.
        assertLosslessDeserialization(dag)

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
            val dagNode: KlibDAGNode = dag[userLibrary]

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

    @ParameterizedTest
    @EnumSource
    fun `cycling dependency is an error`(mode: KlibDAGBuildingMode) = context(mode) {
        /*
         * First compiling:
         * A <-- B <-- C
         *
         * Then adding one more edge between A and C:
         * +-- A <-- B <-- C <--+
         * |                    |
         * +--------------------+
         */
        val moduleNameToLibraryPath = compileModulesWithBackEdges(
            backEdges = listOf("A" to "C"),
        ) {
            addRegularModule("A")
            addRegularModule("B") { dependsOn("A") }
            addRegularModule("C") { dependsOn("B") }
        }

        // A depends on C, C depends on B, B depends on A.
        assertCycleReported(
            moduleNameToLibraryPath,
            loadOrder = listOf("A", "B", "C"),
            expectedCycle = listOf("A", "C", "B"),
        )
    }

    @ParameterizedTest
    @EnumSource
    fun `cycling dependency of two libraries is an error`(mode: KlibDAGBuildingMode) = context(mode) {
        /*
         * First compiling:
         * A <-- B
         *
         * Then adding one more edge between A and B:
         * +-- A <-- B <--+
         * |              |
         * +--------------+
         */
        val moduleNameToLibraryPath = compileModulesWithBackEdges(
            backEdges = listOf("A" to "B"),
        ) {
            addRegularModule("A")
            addRegularModule("B") { dependsOn("A") }
        }

        // A depends on B, B depends on A.
        assertCycleReported(
            moduleNameToLibraryPath,
            loadOrder = listOf("A", "B"),
            expectedCycle = listOf("A", "B"),
        )
    }

    @ParameterizedTest
    @EnumSource
    fun `library that depends on a cycle is not reported as a part of the cycle`(mode: KlibDAGBuildingMode) = context(mode) {
        /*
         * First compiling:
         * A <-- B <-- C
         * ^
         * +---- D
         *
         * Then adding one more edge between A and C:
         * +-- A <-- B <-- C <--+
         * |   ^                |
         * |   +---- D          |
         * +--------------------+
         */
        val moduleNameToLibraryPath = compileModulesWithBackEdges(
            backEdges = listOf("A" to "C"),
        ) {
            addRegularModule("A")
            addRegularModule("B") { dependsOn("A") }
            addRegularModule("C") { dependsOn("B") }
            addRegularModule("D") { dependsOn("A") }
        }

        // D comes first in the load order, so the walk starts from D, but D is not a part of the cycle.
        // A depends on C, C depends on B, B depends on A.
        assertCycleReported(
            moduleNameToLibraryPath,
            loadOrder = listOf("D", "A", "B", "C"),
            expectedCycle = listOf("A", "C", "B"),
        )
    }

    @ParameterizedTest
    @EnumSource
    fun `cycle is chosen by the load order when a library has multiple dependencies in cycles`(
        mode: KlibDAGBuildingMode,
    ) = context(mode) {
        /*
         * First compiling:
         * A <-- B
         * ^
         * +---- C
         *
         * Then making A depend on both B and C:
         * +---> B ---+
         * |          |
         * A <--------+
         * |          |
         * +---> C ---+
         */
        val moduleNameToLibraryPath = compileModulesWithBackEdges(
            backEdges = listOf("A" to "B", "A" to "C"),
        ) {
            addRegularModule("A")
            addRegularModule("B") { dependsOn("A") }
            addRegularModule("C") { dependsOn("A") }
        }

        // A depends on both B and C, so the walk from A proceeds to whichever of them comes first in the load order.
        // Note: Checking both load orders makes it unlikely that the test passes by chance if the choice is not stable.
        assertCycleReported(
            moduleNameToLibraryPath,
            loadOrder = listOf("A", "B", "C"),
            expectedCycle = listOf("A", "B"),
        )
        assertCycleReported(
            moduleNameToLibraryPath,
            loadOrder = listOf("A", "C", "B"),
            expectedCycle = listOf("A", "C"),
        )
    }

    @ParameterizedTest
    @EnumSource
    fun `only the first cycle in the load order is reported`(mode: KlibDAGBuildingMode) = context(mode) {
        /*
         * First compiling:
         * A <-- B
         * C <-- D
         *
         * Then adding one more edge between A and B, and one more edge between C and D:
         * +-- A <-- B <--+
         * |              |
         * +--------------+
         * +-- C <-- D <--+
         * |              |
         * +--------------+
         */
        val moduleNameToLibraryPath = compileModulesWithBackEdges(
            backEdges = listOf("A" to "B", "C" to "D"),
        ) {
            addRegularModule("A")
            addRegularModule("B") { dependsOn("A") }
            addRegularModule("C")
            addRegularModule("D") { dependsOn("C") }
        }

        // C comes first in the load order, so only the cycle of C and D is reported.
        // C depends on D, D depends on C.
        assertCycleReported(
            moduleNameToLibraryPath,
            loadOrder = listOf("C", "A", "D", "B"),
            expectedCycle = listOf("C", "D"),
        )
    }

    @ParameterizedTest
    @EnumSource
    fun `compilation of KLIB with dependency with the same unique_name does not fail (KT-91046)`(mode: KlibDAGBuildingMode) = context(mode) {
            val moduleNameToLibraryPath: MutableMap<String, Path> = mutableMapOf()

            /*
             * A1 <-- A2, both with unique_name=A2
             */
            newSourceModules {
                addRegularModule("A1")
                addRegularModule("A2") { dependsOn("A1") }
            }.compileToKlibsViaCli { module, successKlib ->
                val libraryPath = successKlib.resultingArtifact.klibFile.toPath()
                moduleNameToLibraryPath[module.name] = libraryPath

                if (module.name == "A1") {
                    val manifestPath = libraryPath.resolve("default/manifest")
                    val manifest = manifestPath.readProperties()
                    manifest[KLIB_PROPERTY_UNIQUE_NAME] = "A2"
                    manifestPath.writeProperties(manifest)
                }
            }
            assertEquals(2, moduleNameToLibraryPath.size)

            val a2Manifest = moduleNameToLibraryPath.getValue("A2").resolve("default/manifest").readProperties()
            assertEquals("A2", a2Manifest[KLIB_PROPERTY_UNIQUE_NAME])
            assertNull(a2Manifest.getProperty(KLIB_PROPERTY_DEPENDS))

            run {
                val libraries = loadLibraries(others = listOf(moduleNameToLibraryPath.getValue("A2")))
                val [stdlib, a2] = libraries

                val dag = KlibDAGBuilder(buildParams(libraries)).build()
                assertEquals(listOf(stdlib, a2), dag.librariesReverseTopoSorted)
                assertEquals(setOf(stdlib), dag[a2].directDependencies)
                assertEquals(setOf(stdlib), dag[a2].allDependencies)

                assertLosslessDeserialization(dag)
            }

            run {
                val libraries =
                    loadLibraries(others = listOf(moduleNameToLibraryPath.getValue("A2"), moduleNameToLibraryPath.getValue("A1")))
                val [stdlib, a2, a1] = libraries

                val dag = KlibDAGBuilder(buildParams(libraries)).build()
                assertEquals(listOf(stdlib, a1, a2), dag.librariesReverseTopoSorted)
                assertEquals(setOf(stdlib, a1), dag[a2].directDependencies)
                assertEquals(setOf(stdlib, a1), dag[a2].allDependencies)
                assertEquals(setOf(stdlib), dag[a1].directDependencies)

                assertLosslessDeserialization(dag)
            }
        }

    @ParameterizedTest
    @EnumSource
    fun `the same KLIB passed several times by absolute and relative paths is a single DAG node`(mode: KlibDAGBuildingMode) =
        context(mode) {
            val moduleNameToLibraryPath: MutableMap<String, Path> = mutableMapOf()

            /*
             * A <-- B, A is passed twice by the absolute path and once by the relative path
             */
            newSourceModules {
                addRegularModule("A")
                addRegularModule("B") { dependsOn("A") }
            }.compileToKlibsViaCli { module, successKlib ->
                moduleNameToLibraryPath[module.name] = successKlib.resultingArtifact.klibFile.toPath()
            }
            assertEquals(2, moduleNameToLibraryPath.size)

            val aAbsolutePath = moduleNameToLibraryPath.getValue("A")
            val aRelativePath = aAbsolutePath.relativeTo(Path(System.getProperty("user.dir")))
            assertNotEquals(aAbsolutePath, aRelativePath)

            val libraries =
                loadLibraries(others = listOf(moduleNameToLibraryPath.getValue("B"), aAbsolutePath, aAbsolutePath, aRelativePath))
            assertEquals(3, libraries.size)
            val [stdlib, b, a] = libraries

            val dag = KlibDAGBuilder(buildParams(libraries)).build()
            assertEquals(listOf(stdlib, a, b), dag.librariesReverseTopoSorted)
            assertEquals(setOf(stdlib, a), dag[b].directDependencies)
            assertEquals(setOf(stdlib), dag[a].directDependencies)

            assertLosslessDeserialization(dag)
        }

    @ParameterizedTest
    @EnumSource
    fun `missing transitive dependency is not added to DAG`(mode: KlibDAGBuildingMode) =
        context(mode) {
            val moduleNameToLibraryPath: MutableMap<String, Path> = mutableMapOf()

            /*
             * A <-- B <-- C, A is not passed
             */
            newSourceModules {
                addRegularModule("A")
                addRegularModule("B") { dependsOn("A") }
                addRegularModule("C") { dependsOn("B") }
            }.compileToKlibsViaCli { module, successKlib ->
                moduleNameToLibraryPath[module.name] = successKlib.resultingArtifact.klibFile.toPath()
            }
            assertEquals(3, moduleNameToLibraryPath.size)

            val bManifest = moduleNameToLibraryPath.getValue("B").resolve("default/manifest").readProperties()
            assertNull(bManifest.getProperty(KLIB_PROPERTY_DEPENDS))

            val libraries = loadLibraries(others = listOf(moduleNameToLibraryPath.getValue("B"), moduleNameToLibraryPath.getValue("C")))
            assertEquals(3, libraries.size)
            val [stdlib, b, c] = libraries

            val dag = KlibDAGBuilder(buildParams(libraries)).build()
            assertEquals(listOf(stdlib, b, c), dag.librariesReverseTopoSorted)
            assertEquals(setOf(stdlib), dag[b].directDependencies)
            assertEquals(setOf(stdlib, b), dag[c].directDependencies)
            assertEquals(setOf(stdlib, b), dag[c].allDependencies)

            assertLosslessDeserialization(dag)
        }

    @Test
    fun `SerializedKlibDAG creation sanity test (positive)`() {
        val foo = Path("__foo__")
        val bar = Path("__bar__")
        val baz = Path("__baz__")

        val dag: Map<Path, Set<Path>> = buildMap {
            this[foo] = setOf(bar)
            this[bar] = setOf(baz)
            this[baz] = setOf()
        }

        SerializedKlibDAG(dag)
    }

    @Test
    fun `SerializedKlibDAG creation sanity test (negative)`() {
        val foo = Path("__foo__")
        val bar = Path("__bar__")
        val baz = Path("__baz__")

        val dag: Map<Path, Set<Path>> = buildMap {
            this[foo] = setOf(bar)
            this[bar] = setOf(baz)
            // baz is missing in the graph
        }

        try {
            SerializedKlibDAG(dag)
            fail { "Normally unreachable" }
        } catch (e: Exception) {
            assertEquals(
                "There is a direct dependency __baz__ of library __bar__ that is not in DAG",
                e.message
            )
        }
    }

    @ParameterizedTest
    @EnumSource
    fun `DAG deserialization fails if there are libraries in current compilation that are missing in SerializedKlibDAG`(
        mode: KlibDAGBuildingMode,
    ) = context(mode) {
        val libraries: List<KotlinLibrary> = loadLibraries(platformLibs = true)

        val dag: KlibDAG = KlibDAGBuilder(buildParams(libraries)).build()
        assertEquals(libraries.size, dag.librariesReverseTopoSorted.size)

        val serializedOriginal: SerializedKlibDAG = dag.serialize()
        assertEquals(libraries.size, serializedOriginal.dag.size)

        fun Path.isStdlibOrPosix(): Boolean =
            name == "stdlib" || name.endsWith(".posix")

        val serializedDagWithOnlyStdlibAndPosix = SerializedKlibDAG(
            serializedOriginal.dag.filterKeys { it.isStdlibOrPosix() }
        )
        assertEquals(2, serializedDagWithOnlyStdlibAndPosix.dag.size)

        // Pass the exact number of libraries:
        val deserializedDag: KlibDAG = serializedDagWithOnlyStdlibAndPosix.deserialize(libraries.filter { it.path.isStdlibOrPosix() })
        assertEquals(2, deserializedDag.librariesReverseTopoSorted.size)
        assertTrue(deserializedDag.librariesReverseTopoSorted.all { it.canonicalPath.isStdlibOrPosix() })

        // Pass more libraries as the input that there are actually required:
        try {
            serializedDagWithOnlyStdlibAndPosix.deserialize(libraries)
            fail { "Normally unreachable" }
        } catch (e: Exception) {
            val message = e.message.orEmpty()
            assertTrue(message.startsWith("There are libraries that are used in the current compilation but are missing in the deserialized DAG:"))

            val libraryPaths = message.substringAfter(':').split(',').mapToSet { Path(it.trim()) }
            assertTrue(libraryPaths.none { it.isStdlibOrPosix() })
            assertEquals(libraries.size - 2, libraryPaths.size)
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

    /**
     * Compile the modules defined in [init] to KLIBs. Then, for each `from -> to` pair from [backEdges] recompile
     * the `from` module with one more edge to the `to` module, making a cycle in the graph of dependencies.
     *
     * Note: The `from` module is recompiled with only the new edge, so it should not have any other dependencies in [init].
     *
     * @return The mapping from the name of a module to the path of its KLIB.
     */
    private fun compileModulesWithBackEdges(
        backEdges: List<Pair</* from */ String, /* to */ String>>,
        init: KlibTestSourceModulesBuilder.() -> Unit,
    ): Map</* name of test module */ String, /* path of KLIB */ Path> {
        val moduleNameToLibraryPath: MutableMap<String, Path> = mutableMapOf()

        val modules = newSourceModules(init = init)
        modules.compileToKlibsViaCli { module, successKlib ->
            moduleNameToLibraryPath[module.name] = successKlib.resultingArtifact.klibFile.toPath()
        }
        assertEquals(modules.modules.size, moduleNameToLibraryPath.size)

        // Each module is recompiled only once, with all its back edges at once.
        for ([from, tos] in backEdges.groupBy(keySelector = { it.first }, valueTransform = { it.second })) {
            newSourceModules {
                addRegularModule(from) {
                    sourceFileAddend("fun ${from}2() { ${tos.joinToString("; ") { to -> "$to.$to(0)" }} }")
                }
            }.compileToKlibsViaCli(
                extraCliArgs = tos.flatMap { to -> listOf("-l", moduleNameToLibraryPath.getValue(to).absolutePathString()) }
            ) { module, successKlib ->
                assertEquals(from, module.name)
                moduleNameToLibraryPath[from] = successKlib.resultingArtifact.klibFile.toPath()
            }
        }
        assertEquals(modules.modules.size, moduleNameToLibraryPath.size)

        return moduleNameToLibraryPath
    }

    /**
     * Load libraries in the [loadOrder] and check that building the DAG fails with exactly the [expectedCycle] reported.
     *
     * Note: The load order matters, as the cycle is reported starting from the first library in the load order.
     */
    context(mode: KlibDAGBuildingMode)
    private fun assertCycleReported(
        moduleNameToLibraryPath: Map</* name of test module */ String, /* path of KLIB */ Path>,
        loadOrder: List</* name of test module */ String>,
        expectedCycle: List</* name of test module */ String>,
    ) {
        val libraries = loadLibraries(stdlib = false, others = loadOrder.map(moduleNameToLibraryPath::getValue))

        val exception = assertThrows<KlibDAGCyclicDependencyException> {
            KlibDAGBuilder(buildParams(libraries)).build()
        }

        val expectedMessage = (expectedCycle + expectedCycle.first()).joinToString(
            separator = " ->\n",
            prefix = "Cyclic dependency detected while computing DAG of KLIB dependencies:\n",
        ) { moduleNameToLibraryPath.getValue(it).pathString }

        assertEquals(expectedMessage, exception.message)
    }

    private val nativeHome: File
        get() = testRunSettings.get<KotlinNativeHome>().dir

    private val currentTarget: KonanTarget
        get() = testRunSettings.get<KotlinNativeTargets>().testTarget

    private fun assertLosslessDeserialization(dag: KlibDAG) {
        val serializedOnce = dag.serialize()
        val serializedTwice = serializedOnce.deserialize(dag.librariesReverseTopoSorted).serialize()

        assertEquals(serializedOnce, serializedTwice)
    }

    context(mode: KlibDAGBuildingMode)
    private fun buildParams(
        libraries: List<KotlinLibrary>,
        isRoot: (KotlinLibrary) -> Boolean = { true },
    ) = KlibDAGBuilder.Parameters(
        libraries = libraries,
        isRoot = isRoot,
    ).apply {
        @OptIn(InternalKlibDAGApi::class)
        useSignatureIndices = mode.useSignatureIndices
    }
}

@Suppress("unused") // Enum entries are not used directly in the source code.
enum class KlibDAGBuildingMode(val useSignatureIndices: Boolean) {
    NO_INDICES(useSignatureIndices = false),
    WITH_INDICES(useSignatureIndices = true);

    override fun toString() = if (useSignatureIndices) "with-indices" else "no-indices"
}
