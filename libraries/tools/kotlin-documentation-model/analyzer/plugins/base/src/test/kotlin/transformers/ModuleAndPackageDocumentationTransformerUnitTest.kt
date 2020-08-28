package transformers

import org.jetbrains.dokka.base.transformers.documentables.ModuleAndPackageDocumentationReader
import org.jetbrains.dokka.base.transformers.documentables.ModuleAndPackageDocumentationTransformer
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.DPackage
import org.jetbrains.dokka.model.SourceSetDependent
import org.jetbrains.dokka.model.doc.DocumentationNode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import testApi.testRunner.dPackage
import testApi.testRunner.documentationNode
import testApi.testRunner.sourceSet


class ModuleAndPackageDocumentationTransformerUnitTest {

    @Test
    fun `empty list of modules`() {
        val transformer = ModuleAndPackageDocumentationTransformer(
            object : ModuleAndPackageDocumentationReader {
                override fun get(module: DModule): SourceSetDependent<DocumentationNode> = throw NotImplementedError()
                override fun get(pkg: DPackage): SourceSetDependent<DocumentationNode> = throw NotImplementedError()
            }
        )

        assertEquals(
            emptyList<DModule>(), transformer(emptyList()),
        )
    }

    @Test
    fun `single module documentation`() {
        val transformer = ModuleAndPackageDocumentationTransformer(
            object : ModuleAndPackageDocumentationReader {
                override fun get(pkg: DPackage): SourceSetDependent<DocumentationNode> = throw NotImplementedError()
                override fun get(module: DModule): SourceSetDependent<DocumentationNode> {
                    return module.sourceSets.associateWith { sourceSet ->
                        documentationNode("doc" + sourceSet.displayName)
                    }
                }

            }
        )

        val result = transformer(
            listOf(
                DModule(
                    "ModuleName",
                    documentation = emptyMap(),
                    packages = emptyList(),
                    sourceSets = setOf(
                        sourceSet("A"),
                        sourceSet("B")
                    )
                )
            )
        )

        assertEquals(
            DModule(
                "ModuleName",
                documentation = mapOf(
                    sourceSet("A") to documentationNode("docA"),
                    sourceSet("B") to documentationNode("docB")
                ),
                sourceSets = setOf(sourceSet("A"), sourceSet("B")),
                packages = emptyList()
            ),
            result.single()
        )

    }

    @Test
    fun `merges with already existing module documentation`() {
        val transformer = ModuleAndPackageDocumentationTransformer(
            object : ModuleAndPackageDocumentationReader {
                override fun get(pkg: DPackage): SourceSetDependent<DocumentationNode> = throw NotImplementedError()
                override fun get(module: DModule): SourceSetDependent<DocumentationNode> {
                    /* Only add documentation for first source set */
                    return module.sourceSets.take(1).associateWith { sourceSet ->
                        documentationNode("doc" + sourceSet.displayName)
                    }
                }
            }
        )

        val result = transformer(
            listOf(
                DModule(
                    "MyModule",
                    documentation = mapOf(
                        sourceSet("A") to documentationNode("pre-existing:A"),
                        sourceSet("B") to documentationNode("pre-existing:B")
                    ),
                    sourceSets = setOf(sourceSet("A"), sourceSet("B")),
                    packages = emptyList()
                )
            )
        )

        assertEquals(
            DModule(
                "MyModule",
                documentation = mapOf(
                    /* Expect previous documentation and newly attached one */
                    sourceSet("A") to documentationNode("pre-existing:A", "docA"),
                    /* Only first source set will get documentation attached */
                    sourceSet("B") to documentationNode("pre-existing:B")
                ),
                sourceSets = setOf(sourceSet("A"), sourceSet("B")),
                packages = emptyList()
            ),
            result.single()
        )
    }

    @Test
    fun `package documentation`() {
        val transformer = ModuleAndPackageDocumentationTransformer(
            object : ModuleAndPackageDocumentationReader {
                override fun get(module: DModule): SourceSetDependent<DocumentationNode> = emptyMap()
                override fun get(pkg: DPackage): SourceSetDependent<DocumentationNode> {
                    /* Only attach documentation to packages with 'attach' */
                    if ("attach" !in pkg.dri.packageName.orEmpty()) return emptyMap()
                    /* Only attach documentation to two source sets */
                    return pkg.sourceSets.take(2).associateWith { sourceSet ->
                        documentationNode("doc:${sourceSet.displayName}:${pkg.dri.packageName}")
                    }
                }
            }
        )

        val result = transformer(
            listOf(
                DModule(
                    "MyModule",
                    documentation = emptyMap(),
                    sourceSets = emptySet(),
                    packages = listOf(
                        dPackage(
                            dri = DRI("com.sample"),
                            documentation = mapOf(
                                sourceSet("A") to documentationNode("pre-existing:A:com.sample")
                            ),
                            sourceSets = setOf(sourceSet("A"), sourceSet("B"), sourceSet("C")),
                        ),
                        dPackage(
                            dri = DRI("com.attach"),
                            documentation = mapOf(
                                sourceSet("A") to documentationNode("pre-existing:A:com.attach")
                            ),
                            sourceSets = setOf(sourceSet("A"), sourceSet("B"), sourceSet("C"))
                        ),
                        dPackage(
                            dri = DRI("com.attach.sub"),
                            documentation = mapOf(
                                sourceSet("A") to documentationNode("pre-existing:A:com.attach.sub"),
                                sourceSet("B") to documentationNode("pre-existing:B:com.attach.sub"),
                                sourceSet("C") to documentationNode("pre-existing:C:com.attach.sub")
                            ),
                            sourceSets = setOf(sourceSet("A"), sourceSet("B"), sourceSet("C")),
                        )
                    )
                )
            )
        )

        result.single().packages.forEach { pkg ->
            assertEquals(
                setOf(sourceSet("A"), sourceSet("B"), sourceSet("C")), pkg.sourceSets,
                "Expected source sets A, B, C for package ${pkg.dri.packageName}"
            )
        }

        val comSample = result.single().packages.single { it.dri.packageName == "com.sample" }
        assertEquals(
            mapOf(sourceSet("A") to documentationNode("pre-existing:A:com.sample")),
            comSample.documentation,
            "Expected no documentation added to package 'com.sample' because of wrong package"
        )

        val comAttach = result.single().packages.single { it.dri.packageName == "com.attach" }
        assertEquals(
            mapOf(
                sourceSet("A") to documentationNode("pre-existing:A:com.attach", "doc:A:com.attach"),
                sourceSet("B") to documentationNode("doc:B:com.attach")
            ),
            comAttach.documentation,
            "Expected documentation added to source sets A and B"
        )

        assertEquals(
            DModule(
                "MyModule",
                documentation = emptyMap(),
                sourceSets = emptySet(),
                packages = listOf(
                    dPackage(
                        dri = DRI("com.sample"),
                        documentation = mapOf(
                            /* No documentation added, since in wrong package */
                            sourceSet("A") to documentationNode("pre-existing:A:com.sample")
                        ),
                        sourceSets = setOf(sourceSet("A"), sourceSet("B"), sourceSet("C")),

                        ),
                    dPackage(
                        dri = DRI("com.attach"),
                        documentation = mapOf(
                            /* Documentation added */
                            sourceSet("A") to documentationNode("pre-existing:A:com.attach", "doc:A:com.attach"),
                            sourceSet("B") to documentationNode("doc:B:com.attach")
                        ),
                        sourceSets = setOf(sourceSet("A"), sourceSet("B"), sourceSet("C")),
                    ),
                    dPackage(
                        dri = DRI("com.attach.sub"),
                        documentation = mapOf(
                            /* Documentation added */
                            sourceSet("A") to documentationNode(
                                "pre-existing:A:com.attach.sub",
                                "doc:A:com.attach.sub"
                            ),
                            /* Documentation added */
                            sourceSet("B") to documentationNode(
                                "pre-existing:B:com.attach.sub",
                                "doc:B:com.attach.sub"
                            ),
                            /* No documentation added, since in wrong source set */
                            sourceSet("C") to documentationNode("pre-existing:C:com.attach.sub")
                        ),
                        sourceSets = setOf(sourceSet("A"), sourceSet("B"), sourceSet("C")),
                    )
                )
            ), result.single()
        )
    }

}
