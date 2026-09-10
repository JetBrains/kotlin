/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
    private val mustRunAlwaysAnnotationDesc = Type.getDescriptor(MustRunAlways::class.java)
    private val mustRunOnChangesInAnnotationDesc = Domain.entries.associateWith { domain ->
        Type.getDescriptor(mustRunOnChangesInAnnotationOf(domain).java)
    }

    /**
     * Checks that tests using test data are selected to run when the domain containing that test data is changed.
     *
     * Scans compiled classes for `@TestMetadata` annotations and compares the domains of the test and its test data.
     *
     * A test marked by the `@TestMetadata` annotation must meet one of the following conditions:
     * - the test data belongs to the same domains as the test
     * - the test's domains list the test data's domains in `mustRunAllTestsOnChangesIn`
     * - the test is annotated with `@MustRunOnChangesInXYZ` for a domain containing its test data
     * - the test is annotated with `@MustRunAlways`
     */
    @Test
    fun `test-federation dependencies`() {
        val violations = Channel<String>(Channel.UNLIMITED)
        val checkedAnnotations = AtomicInt(0)
        val checkedClasses = AtomicInt(0)

        var lastProgressPrinted = Clock.System.now()

        fun printProgress() {
            println("Checked: $checkedAnnotations '@TestMetadata' annotations on $checkedClasses classes")
        }

        runBlocking(Dispatchers.IO) {
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

                        /* Check if the metadata is living in any of the 'mustRunAllTestsOnChangesIn' dependencies of the test */
                        if (metadataDomains.intersect(testDomains.flatMap { it.mustRunAllTestsOnChangesIn }.toSet()).isNotEmpty())
                            return@forEach

                        /* Check if the test is marked as MustRunAlways and therefore always runs */
                        if (classNode.visibleAnnotations.any { it.desc == mustRunAlwaysAnnotationDesc }) return@forEach

                        /* Check if the test is marked as '@MustRunOnChangesIn' any of metadata domains*/
                        if (classNode.visibleAnnotations.any { annotation ->
                                metadataDomains.any { metadataDomain ->
                                    annotation.desc == mustRunOnChangesInAnnotationDesc[metadataDomain.domain]
                                }
                            }) return@forEach

                        violations.send(buildString {
                            appendLine("${classNode.name}: ${testDomains.joinToString(", ") { it.domain.name }}")
                            appendLine("@TestMetadata(\"$metadataPath\"): ${metadataDomains.joinToString(", ") { it.domain.name }}")
                            appendLine("""   The test class uses metadata from a different domain, without declaring a dependency on it.""")
                            appendLine("""   Solutions:""")
                            metadataDomains.forEach { metadataDomain ->
                                appendLine("""       - Add @${mustRunOnChangesInAnnotationOf(metadataDomain.domain).simpleName} (recommended)""")
                                appendLine("""       - Declare mustRunAllTestsOnChangesIn: ${metadataDomain.domain.name} (if absolutely necessary)""")
                            }
                            appendLine("""       - Add @${MustRunAlways::class.simpleName} (run this test regardless of changed domains)""")
                        })
                    }
                }

            }

            violations.close()
            printProgress()

            if (checkedAnnotations.load() == 0 || checkedClasses.load() == 0) {
                error("No @TestMetadata annotations or classes processed")
            }

            val violations = violations.consumeAsFlow().toList().sorted()
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
}
