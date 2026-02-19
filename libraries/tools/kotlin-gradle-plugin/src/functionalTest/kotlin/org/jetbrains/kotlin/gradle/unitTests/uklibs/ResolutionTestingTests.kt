/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.uklibs

import org.gradle.api.Project
import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.attributes.Attribute
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.project
import org.jetbrains.kotlin.gradle.testing.*
import org.jetbrains.kotlin.gradle.unitTests.uklibs.GradleMetadataComponent.Variant
import org.jetbrains.kotlin.gradle.util.buildProject
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class ResolutionTestingTests {

    @field:TempDir
    lateinit var temporaryFolder: File

    @Test
    fun `single artifact resolution`() {
        assertEquals<PrettyPrint<Map<String, ResolvedComponentWithArtifacts>>>(
            mutableMapOf<String, ResolvedComponentWithArtifacts>(
                "foo:direct:1.0" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            attribute.name to attributeValue,
                            "artifactType" to "jar",
                        ),
                    ),
                    configuration = "jvmApiElements-published",
                ),
            ).prettyPrinted,
            resolveComponent(
                singleArtifactVariant,
                mapOf(attribute to attributeValue),
            ).prettyPrinted
        )
    }

    @Test
    fun `multiple artifacts resolution`() {
        assertEquals<PrettyPrint<Map<String, ResolvedComponentWithArtifacts>>>(
            mutableMapOf<String, ResolvedComponentWithArtifacts>(
                "foo:direct:1.0" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            attribute.name to attributeValue,
                        ),
                        mutableMapOf(
                            attribute.name to attributeValue,
                        ),
                    ),
                    configuration = "jvmApiElements-published",
                ),
            ).prettyPrinted, resolveComponent(
                multipleArtifactsWithDifferentExtensionsVariant,
                mapOf(attribute to attributeValue),
            ).prettyPrinted
        )
    }

    @Test
    fun `project dependency resolution`() {
        val root = buildProject()
        buildProject(
            projectBuilder = {
                this.withName("producer")
                this.withParent(root)
            }
        ) {
            configurations.create("consumable") {
                it.isCanBeConsumed = true
                it.isCanBeResolved = false
                it.attributes.attribute(attribute, attributeValue)
            }
        }

        assertEquals<PrettyPrint<Map<String, ResolvedComponentWithArtifacts>>>(
            mutableMapOf<String, ResolvedComponentWithArtifacts>(
                ":producer" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                    ),
                    configuration = "consumable",
                ),
            ).prettyPrinted, with(
                buildProject(
                    projectBuilder = {
                        this.withName("consumer")
                        this.withParent(root)
                    }
                )
            ) {
                configurations.create("resolvable") {
                    it.isCanBeConsumed = false
                    it.isCanBeResolved = true
                    it.dependencies.add(dependencies.project(":producer"))
                    it.attributes.attribute(attribute, attributeValue)
                }
            }.resolveProjectDependencyComponentsWithArtifacts()
                .prettyPrinted
        )
    }

    @Test
    fun `resolution with transforms`() {
        val transformAttribute = Attribute.of("transformed", String::class.java)
        assertEquals<PrettyPrint<Map<String, ResolvedComponentWithArtifacts>>>(
            mutableMapOf<String, ResolvedComponentWithArtifacts>(
                "foo:direct:1.0" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            attribute.name to attributeValue,
                            transformAttribute.name to "true",
                            "artifactType" to "jar",
                        ),
                    ),
                    configuration = "jvmApiElements-published",
                ),
            ).prettyPrinted, resolveComponent(
                singleArtifactVariant,
                mapOf(
                    attribute to attributeValue,
                    transformAttribute to "true",
                ),
            ) {
                dependencies.artifactTypes.create("jar").attributes.attribute(transformAttribute, "false")
                dependencies.registerTransform(TransparentTransform::class.java) {
                    it.from.attribute(transformAttribute, "false")
                    it.to.attribute(transformAttribute, "true")
                }
            }
                .prettyPrinted)
    }

    private fun resolveComponent(
        variant: Variant,
        requestedAttributes: Map<Attribute<String>, String>,
        configuration: Project.() -> Unit = {},
    ): Map<String, ResolvedComponentWithArtifacts> {
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
                        variants = listOf(variant),
                    ),
                    directMavenComponent,
                ),
            )
        )

        return with(buildProject()) {
            configuration()
            repositories.maven { it.url = uri(generated) }
            configurations.create("resolvable") { conf ->
                requestedAttributes.forEach {
                    conf.attributes.attribute(it.key, it.value)
                }
                conf.dependencies.add(dependencies.create("foo:direct:1.0"))
            }.resolveProjectDependencyComponentsWithArtifacts()
        }
    }

    private val attribute = Attribute.of("key", String::class.java)
    private val attributeValue = "value"

    private val directGradleComponent = GradleMetadataComponent.Component(
        group = "foo",
        module = "direct",
        version = "1.0",
    )

    private val singleArtifactVariant = Variant(
        name = "jvmApiElements-published",
        attributes = mapOf(
            attribute.name to attributeValue,
        ),
        files = listOf(
            GradleMetadataComponent.MockVariantFile(
                artifactId = "bar",
                version = "1.0",
                extension = "jar",
            ),
        ),
        dependencies = listOf()
    )

    private val multipleArtifactsWithDifferentExtensionsVariant = Variant(
        name = "jvmApiElements-published",
        attributes = mapOf(
            attribute.name to attributeValue,
        ),
        files = listOf(
            GradleMetadataComponent.MockVariantFile(
                artifactId = "bar",
                version = "1.0",
                extension = "jar",
            ),
            GradleMetadataComponent.MockVariantFile(
                artifactId = "baz",
                version = "1.0",
                extension = "klib",
            ),
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

    abstract class TransparentTransform : TransformAction<TransformParameters.None> {
        @get:InputArtifact
        abstract val inputArtifact: Provider<FileSystemLocation>
        override fun transform(outputs: TransformOutputs) {
            outputs.file(inputArtifact.get().asFile)
        }
    }

}
