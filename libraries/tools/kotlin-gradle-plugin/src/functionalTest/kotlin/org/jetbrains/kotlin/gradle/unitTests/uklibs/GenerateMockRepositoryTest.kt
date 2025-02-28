/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.uklibs

import org.jetbrains.kotlin.gradle.testing.generateFixtureIfMissing
import org.jetbrains.kotlin.gradle.unitTests.uklibs.GradleMetadataComponent.Variant
import org.jetbrains.kotlin.incremental.testingUtils.assertEqualDirectoriesIgnoringDotFiles
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import kotlin.test.Test

class GenerateMockRepositoryTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun test() {
        val expected = java.io.File(System.getProperty("resourcesPath")).resolve("GenerateMockRepositoryTest")
        val generated = generateMockRepository(
            temporaryFolder,
            gradleComponents = listOf(
                GradleComponent(
                    GradleMetadataComponent(
                        component = GradleMetadataComponent.Component(
                            group = "foo",
                            module = "direct",
                            version = "1.0",
                        ),
                        variants = listOf(
                            kmpMetadataJarVariant,
                            kmpJvmApiVariant,
                        ),
                    ),
                    directMavenComponent,
                ),
            )
        )

        assertEqualDirectoriesIgnoringDotFiles(
            generateFixtureIfMissing(
                expected,
                generated,
            ),
            generated,
            false,
        )
    }

    private val directGradleComponent = GradleMetadataComponent.Component(
        group = "foo",
        module = "direct",
        version = "1.0",
    )

    private val transitiveGradleComponent = GradleMetadataComponent.Component(
        group = "foo",
        module = "transitive",
        version = "1.0",
    )

    private val kmpMetadataJarVariant = Variant(
        name = "metadataApiElements",
        attributes = mapOf(
            "foo" to "bar",
            "bar" to "baz",
        ),
        files = listOf(
            GradleMetadataComponent.MockVariantFile(
                artifactId = "bar",
                version = "1.0",
                extension = "jar",
                classifier = "psm",
                type = GradleMetadataComponent.MockVariantType.EmptyJar,
            )
        ),
        dependencies = listOf(
            transitiveGradleComponent.requiresDependency,
        )
    )

    private val kmpJvmApiVariant = Variant(
        name = "jvmApiElements-published",
        attributes = mapOf(
            "jvm" to "true",
        ),
        files = listOf(
            GradleMetadataComponent.MockVariantFile(
                artifactId = "bar",
                version = "1.0",
                extension = "jar",
            )
        ),
        dependencies = listOf()
    )

    private val directMavenComponent = MavenComponent(
        directGradleComponent.group, directGradleComponent.module, directGradleComponent.version,
        packaging = "uklib",
        dependencies = listOf(
            MavenComponent.Dependency(
                groupId = "foo",
                artifactId = "bar",
                version = "1.0",
                scope = "scope",
            )
        ),
        true,
    )
}