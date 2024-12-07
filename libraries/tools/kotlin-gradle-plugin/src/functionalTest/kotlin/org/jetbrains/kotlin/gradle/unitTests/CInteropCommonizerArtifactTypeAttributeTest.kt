/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.attributes.Usage
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.targets.native.internal.CInteropCommonizerArtifactTypeAttribute
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.utils.createConsumable
import org.jetbrains.kotlin.gradle.utils.createResolvable
import org.jetbrains.kotlin.gradle.utils.named
import kotlin.test.Test
import kotlin.test.assertEquals

class CInteropCommonizerArtifactTypeAttributeTest {

    @Test
    fun `test - transformation from klib-collection-dir to klibs`() {
        val project = buildProjectWithMPP()

        /* Create stub klibs collection directory */
        val klibCollectionDir = project.layout.buildDirectory.dir("testOutputDir").get().asFile
        klibCollectionDir.mkdirs()

        val klibs = listOf(
            klibCollectionDir.resolve("foo.klib").also { file -> file.writeText("stub") },
            klibCollectionDir.resolve("bar").also { file -> file.mkdirs() }
        )

        /* Create consumable elements configuration */
        project.configurations.createConsumable("testElements").also { configuration ->
            configuration.attributes.attributeProvider(
                Usage.USAGE_ATTRIBUTE,
                project.provider { project.objects.named("test") }
            )

            /* Add klibCollectionDir as artifact */
            configuration.outgoing.artifact(klibCollectionDir) { artifact ->
                artifact.type = CInteropCommonizerArtifactTypeAttribute.KLIB_COLLECTION_DIR
                artifact.extension = CInteropCommonizerArtifactTypeAttribute.KLIB_COLLECTION_DIR
            }
        }

        /* Create resolvable configuration */
        val resolvable = project.configurations.createResolvable("testDependencies").also { configuration ->
            configuration.attributes.attributeProvider(
                Usage.USAGE_ATTRIBUTE,
                project.provider { project.objects.named("test") }
            )
            configuration.attributes.attributeProvider(
                CInteropCommonizerArtifactTypeAttribute.attribute,
                project.provider { CInteropCommonizerArtifactTypeAttribute.KLIB }
            )
        }

        project.dependencies { resolvable(project) }

        assertEquals(klibs.toSet(), resolvable.incoming.artifactView { }.files.toSet())
    }
}
