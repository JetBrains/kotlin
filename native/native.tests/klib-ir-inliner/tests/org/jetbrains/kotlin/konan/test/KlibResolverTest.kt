/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test

import org.jetbrains.kotlin.konan.test.blackbox.AbstractNativeSimpleTest
import org.jetbrains.kotlin.konan.test.klib.compileToKlibsViaCli
import org.jetbrains.kotlin.konan.test.klib.newSourceModules
import org.jetbrains.kotlin.library.KLIB_PROPERTY_DEPENDENCY_VERSION
import org.jetbrains.kotlin.library.KLIB_PROPERTY_LIBRARY_VERSION
import org.jetbrains.kotlin.test.services.JUnit5Assertions
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertTrue
import org.jetbrains.kotlin.test.utils.patchManifestAsMap
import org.junit.jupiter.api.Test

// TODO (KT-61096): Remove this obsolete test runner together with the KLIB resolver.
class KlibResolverTest : AbstractNativeSimpleTest() {
    @Test
    fun `Compiler ignores dependency versions in manifest`() {
        val modules = newSourceModules {
            addRegularModule("liba")
            addRegularModule("libb") { dependsOn("liba") }
            addRegularModule("libc") { dependsOn("liba", "libb") }
        }

        // Control compilation -- should finish successfully.
        modules.compileToKlibsViaCli()

        // Compilation with patched manifest -- should finish successfully too.
        modules.compileToKlibsViaCli { module, successKlib ->
            when (module.name) {
                "liba" -> {
                    // set the library version = 1.0
                    patchManifestAsMap(JUnit5Assertions, successKlib.resultingArtifact.klibFile) { properties ->
                        @Suppress("DEPRECATION")
                        properties[KLIB_PROPERTY_LIBRARY_VERSION] = "1.0"
                    }
                }
                "libb" -> {
                    // pretend it depends on liba v2.0
                    patchManifestAsMap(JUnit5Assertions, successKlib.resultingArtifact.klibFile) { properties ->
                        // first, check:
                        val dependencyVersionPropertyNames: Set<String> =
                            properties.keys.filter { @Suppress("DEPRECATION") it.startsWith(KLIB_PROPERTY_DEPENDENCY_VERSION) }.toSet()

                        assertTrue(dependencyVersionPropertyNames.isEmpty()) {
                            "Unexpected properties in manifest: ${dependencyVersionPropertyNames.joinToString()}"
                        }

                        // then, patch:
                        @Suppress("DEPRECATION")
                        properties[KLIB_PROPERTY_DEPENDENCY_VERSION + "_liba"] = "2.0"
                    }
                }
            }
        }
    }

    @Test
    fun `Compiler does not add dependency versions to manifest`() {
        val modules = newSourceModules {
            addRegularModule("liba")
            addRegularModule("libb") { dependsOn("liba") }
        }

        // Control compilation -- should finish successfully.
        modules.compileToKlibsViaCli()

        // Compilation with patched manifest -- should finish successfully too.
        modules.compileToKlibsViaCli { module, successKlib ->
            when (module.name) {
                "liba" -> {
                    // set the library version = 1.0
                    patchManifestAsMap(JUnit5Assertions, successKlib.resultingArtifact.klibFile) { properties ->
                        @Suppress("DEPRECATION")
                        properties[KLIB_PROPERTY_LIBRARY_VERSION] = "1.0"
                    }
                }
                "libb" -> {
                    // check that dependency version is set
                    patchManifestAsMap(JUnit5Assertions, successKlib.resultingArtifact.klibFile) { properties ->
                        val dependencyVersionPropertyNames: Set<String> =
                            properties.keys.filter { @Suppress("DEPRECATION") it.startsWith(KLIB_PROPERTY_DEPENDENCY_VERSION) }.toSet()

                        assertTrue(dependencyVersionPropertyNames.isEmpty()) {
                            "Unexpected properties in manifest: ${dependencyVersionPropertyNames.joinToString()}"
                        }
                    }
                }
            }
        }
    }
}
