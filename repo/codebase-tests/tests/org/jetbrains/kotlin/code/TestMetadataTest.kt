/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code

import org.jetbrains.kotlin.testFederation.*
import org.jetbrains.org.objectweb.asm.Type
import org.junit.jupiter.api.fail
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch
import kotlin.io.path.Path
import kotlin.io.path.absolute
import kotlin.test.Test
import kotlin.time.Clock

@OptIn(ExperimentalAtomicApi::class)
class TestMetadataTest {

    private val absoluteRoot = Path("").absolute()

    private val testMetadataAnnotationDesc = "Lorg/jetbrains/kotlin/test/TestMetadata;"
    private val smokeTestAnnotationDesc = Type.getDescriptor(SmokeTest::class.java)
    private val affectedByAnnotationDesc = Domain.entries.associateWith { domain ->
        Type.getDescriptor(affectedByAnnotationOf(domain).java)
    }

    /**
     * Our repository contains many tests generated from test-data.
     * Those tests can use test-data from generic locations.
     * These test-data locations might be living within a different 'Domain' as the actual test.
     * This test is checking if the test is executed when the given test-data is changed (according to the rules of federal ci)
     *
     * This test, therefore, walks through the repository, analyzing each class file for '@TestMetadata' annotations.
     * If the provided '@TestMetadata' annotation is found, it will check if the test-data is inside the same 'Domain' as the test.
     * If the test-data is not inside the same 'Domain', the test will check if the test is executed when the test-data is changed.
     *
     * A test marked by the `@TestMetadata` annotation must meet one of the following conditions:
     * - the metadata is living in the same domains as the test
     * - the metadata is living in any of the 'fullyAffectedBy' dependencies of the test
     * - the test is marked as '@AffectedBy' any of metadata domains
     * - the test is marked as '@SmokeTest' (so it always runs)
     */
    @Test
    fun `test-federation dependencies`() {
        val violations = mutableListOf<String>()
        val checkedAnnotations = AtomicInt(0)
        val checkedClasses = AtomicInt(0)

        var lastProgressPrinted = Clock.System.now()

        fun printProgress() {
            println("Checked: $checkedAnnotations '@TestMetadata' annotations on $checkedClasses classes")
        }

        forEachCompiledClass { file, classNode ->
            checkedClasses.incrementAndFetch()
            classNode.visibleAnnotations?.forEach { annotation ->
                if (annotation.desc == testMetadataAnnotationDesc) {
                    checkedAnnotations.incrementAndFetch()
                    val now = Clock.System.now()
                    if ((now - lastProgressPrinted).inWholeSeconds >= 5) {
                        lastProgressPrinted = now
                        printProgress()
                    }
                    val metadataPath = annotation.values.zipWithNext().toMap().getValue("value").toString()
                    val metadataDomains = DomainInfo.resolveDomainInfosOf(RepositoryPath(absoluteRoot, Path(metadataPath)))
                    val testDomains = DomainInfo.resolveDomainInfosOf(RepositoryPath(absoluteRoot, file))

                    /* Check if the metadata is living in the same domains as the test */
                    if (metadataDomains.intersect(testDomains.toSet()).isNotEmpty()) return@forEach

                    /* Check if the metadata is living in any of the 'fullyAffectedBy' dependencies of the test */
                    if (metadataDomains.intersect(testDomains.flatMap { it.fullyAffectedBy }.toSet()).isNotEmpty()) return@forEach

                    /* Check if the test is marked as SmokeTest and therefore always runs */
                    if (classNode.visibleAnnotations.any { it.desc == smokeTestAnnotationDesc }) return@forEach

                    /* Check if the test is marked as '@AffectedBy' any of metadata domains*/
                    if (classNode.visibleAnnotations.any { annotation ->
                            metadataDomains.any { metadataDomain ->
                                annotation.desc == affectedByAnnotationDesc[metadataDomain.domain]
                            }
                        }) return@forEach

                    violations.add(buildString {
                        appendLine("${classNode.name}: ${testDomains.joinToString(", ") { it.domain.name }}")
                        appendLine("@TestMetadata(\"$metadataPath\"): ${metadataDomains.joinToString(", ") { it.domain.name }}")
                        appendLine("""   The test class uses metadata from a different domain, without declaring a dependency on it.""")
                        appendLine("""   Solutions:""")
                        metadataDomains.forEach { metadataDomain ->
                            appendLine("""       - Add @${affectedByAnnotationOf(metadataDomain.domain).simpleName} (recommended)""")
                            appendLine("""       - Declare fullyAffectedBy: ${metadataDomain.domain.name} (if absolutely necessary)""")
                        }
                        appendLine("""       - Add @${SmokeTest::class.simpleName} (mark this test as SmokeTest)""")
                    })
                }
            }
        }

        printProgress()

        if (checkedAnnotations.load() == 0 || checkedClasses.load() == 0) {
            error("No @TestMetadata annotations or classes processed")
        }

        if (violations.isNotEmpty()) {
            fail {
                buildString {
                    appendLine("${violations.size} @TestMetadata dependency violations found:")
                    appendLine("")
                    append(violations.joinToString("\n\n"))
                }
            }
        }
    }
}
