/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.core

import org.jetbrains.kotlin.commonizer.*
import org.jetbrains.kotlin.commonizer.cir.CirType
import org.jetbrains.kotlin.commonizer.mergedtree.CirClassifierIndex
import org.jetbrains.kotlin.commonizer.mergedtree.CirCommonizedClassifierNodes
import org.jetbrains.kotlin.commonizer.mergedtree.CirKnownClassifiers
import org.jetbrains.kotlin.commonizer.mergedtree.CirProvidedClassifiers
import org.jetbrains.kotlin.commonizer.tree.mergeCirTree
import org.jetbrains.kotlin.commonizer.utils.*
import org.jetbrains.kotlin.storage.LockBasedStorageManager

class TypeCommonizerTest : AbstractInlineSourcesCommonizationTest() {

    fun createCommonizer(
        commonTargetSources: InlineSourceBuilder.ModuleBuilder.() -> Unit = {
            source(
                """
                package org.sample
                class Foo
                class Bar
            """.trimIndent(), "commonTargetSource.kt"
            )
            source(
                """
                package org.fictitiousPackageName
                class Foo
                """.trimIndent()
            )
        },
        commonDependencySources: InlineSourceBuilder.ModuleBuilder.() -> Unit = {
            source(
                """
                package kotlin.collections
                class List
                class Set
            """.trimIndent(), "collections.kt"
            )

            source(
                """
                package kotlinx.cinterop
                class CPointer
            """.trimIndent(), "cinterop.kt"
            )

            source(
                """
                package kotlin.fictitiousPackageName
                class List
                class CPointer
            """.trimIndent(), "fictitiousPackageName.kt"
            )
        },
        targetASpecificSources: (InlineSourceBuilder.ModuleBuilder.() -> Unit)? = null,
        targetBSpecificSources: (InlineSourceBuilder.ModuleBuilder.() -> Unit)? = null,
        targetCSpecificSources: (InlineSourceBuilder.ModuleBuilder.() -> Unit)? = null,
        targetADependencySources: (InlineSourceBuilder.ModuleBuilder.() -> Unit)? = null,
        targetBDependencySources: (InlineSourceBuilder.ModuleBuilder.() -> Unit)? = null,
        targetCDependencySources: (InlineSourceBuilder.ModuleBuilder.() -> Unit)? = null
    ): TypeCommonizer {

        val targetARoot = createCirTreeRoot {
            commonTargetSources()
            if (targetASpecificSources != null) targetASpecificSources()
        }

        val targetBRoot = createCirTreeRoot {
            commonTargetSources()
            if (targetBSpecificSources != null) targetBSpecificSources()
        }

        val targetCRoot = createCirTreeRoot {
            commonTargetSources()
            if (targetCSpecificSources != null) targetCSpecificSources()
        }

        val commonDependencies = createCirProvidedClassifiers { commonDependencySources() }

        val targetADependencies = targetADependencySources?.let { createCirProvidedClassifiers { it() } } ?: CirProvidedClassifiers.EMPTY
        val targetBDependencies = targetBDependencySources?.let { createCirProvidedClassifiers { it() } } ?: CirProvidedClassifiers.EMPTY
        val targetCDependencies = targetCDependencySources?.let { createCirProvidedClassifiers { it() } } ?: CirProvidedClassifiers.EMPTY

        val targetDependencies = TargetDependent(
            LeafCommonizerTarget("a") to targetADependencies,
            LeafCommonizerTarget("b") to targetBDependencies,
            LeafCommonizerTarget("c") to targetCDependencies
        )

        val roots = TargetDependent(
            LeafCommonizerTarget("a") to targetARoot,
            LeafCommonizerTarget("b") to targetBRoot,
            LeafCommonizerTarget("c") to targetCRoot
        )

        val classifiers = CirKnownClassifiers(
            classifierIndices = roots.mapValue(::CirClassifierIndex),
            targetDependencies = targetDependencies,
            commonizedNodes = CirCommonizedClassifierNodes.default(),
            commonDependencies = commonDependencies
        ).also { classifiers ->
            mergeCirTree(LockBasedStorageManager.NO_LOCKS, classifiers, roots, settings = DefaultCommonizerSettings)
        }

        return TypeCommonizer(classifiers, DefaultCommonizerSettings)
    }


    fun `test class types in Kotlin package with same name`() {
        assertEquals(
            mockClassType("kotlin/collections/List"), createCommonizer().invoke(
                listOf(
                    mockClassType("kotlin/collections/List"),
                    mockClassType("kotlin/collections/List"),
                    mockClassType("kotlin/collections/List")
                )
            )
        )
    }

    fun `test class types in Kotlin package with different names - 1`() {
        assertEquals(
            null, createCommonizer().invoke(
                listOf(
                    mockClassType("kotlin/collections/List"),
                    mockClassType("kotlin/collections/List"),
                    mockClassType("kotlin/fictitiousPackageName/List")
                )
            )
        )
    }

    fun `test class types in Kotlin package with different names - 2`() {
        assertEquals(
            null, createCommonizer().invoke(
                listOf(
                    mockClassType("kotlin/collections/List"),
                    mockClassType("kotlin/collections/List"),
                    mockClassType("kotlin/collections/Set")
                )
            )
        )
    }

    fun `test class types in Kotlin package with different names - 3`() {
        assertEquals(
            null, createCommonizer().invoke(
                listOf(
                    mockClassType("kotlin/collections/List"),
                    mockClassType("kotlin/collections/List"),
                    mockClassType("org/sample/Foo")
                )
            )
        )
    }

    fun `test class types in Kotlinx package with same name`() {
        assertEquals(
            mockClassType("kotlinx/cinterop/CPointer"), createCommonizer().invoke(
                listOf(
                    mockClassType("kotlinx/cinterop/CPointer"),
                    mockClassType("kotlinx/cinterop/CPointer"),
                    mockClassType("kotlinx/cinterop/CPointer")
                )
            )
        )
    }

    fun `test class types in Kotlinx package with different names - 1`() {
        assertEquals(
            null, createCommonizer().invoke(
                listOf(
                    mockClassType("kotlinx/cinterop/CPointer"),
                    mockClassType("kotlinx/cinterop/CPointer"),
                    mockClassType("kotlin/fictitiousPackageName/CPointer")
                )
            )
        )
    }

    fun `test class types in user package with same name`() {
        assertEquals(
            mockClassType("org/sample/Foo"), createCommonizer().invoke(
                listOf(
                    mockClassType("org/sample/Foo"),
                    mockClassType("org/sample/Foo"),
                    mockClassType("org/sample/Foo")
                )
            )
        )
    }

    fun `test class types in user package with different names - 1`() {
        assertEquals(
            null, createCommonizer().invoke(
                listOf(
                    mockClassType("org/sample/Foo"),
                    mockClassType("org/fictitiousPackageName/Foo")
                )
            )
        )
    }

    fun `test class types in user package with different names - 2`() {
        assertEquals(
            null, createCommonizer().invoke(
                listOf(
                    mockClassType("org/sample/Foo"),
                    mockClassType("org/sample/Bar")
                )
            )
        )
    }

    fun `test class types in user package with different names - 3`() {
        assertEquals(
            null, createCommonizer().invoke(
                listOf(
                    mockClassType("org/sample/Foo"),
                    mockClassType("kotlin/String")
                )
            )
        )
    }

    fun `test class types in Kotlin package with same nullability - 1`() {
        assertEquals(
            mockClassType("kotlin/collections/List", nullable = false), createCommonizer().invoke(
                listOf(
                    mockClassType("kotlin/collections/List", nullable = false),
                    mockClassType("kotlin/collections/List", nullable = false),
                    mockClassType("kotlin/collections/List", nullable = false)
                )
            )
        )
    }

    fun `test class types in Kotlin package with same nullability - 2`() {
        assertEquals(
            mockClassType("kotlin/collections/List", nullable = true), createCommonizer().invoke(
                listOf(
                    mockClassType("kotlin/collections/List", nullable = true),
                    mockClassType("kotlin/collections/List", nullable = true),
                    mockClassType("kotlin/collections/List", nullable = true)
                )
            )
        )
    }

    fun `test class types in Kotlin package with different nullability - 1`() {
        assertEquals(
            null, createCommonizer().invoke(
                listOf(
                    mockClassType("kotlin/collections/List", nullable = false),
                    mockClassType("kotlin/collections/List", nullable = false),
                    mockClassType("kotlin/collections/List", nullable = true)
                )
            )
        )
    }

    fun `test class types in Kotlin package with different nullability - 2`() {
        assertEquals(
            null, createCommonizer().invoke(
                listOf(
                    mockClassType("kotlin/collections/List", nullable = true),
                    mockClassType("kotlin/collections/List", nullable = true),
                    mockClassType("kotlin/collections/List", nullable = false)
                )
            )
        )
    }

    fun `test class types in user package with same nullability - 1`() {
        assertEquals(
            mockClassType("org/sample/Foo", nullable = false), createCommonizer().invoke(
                listOf(
                    mockClassType("org/sample/Foo", nullable = false),
                    mockClassType("org/sample/Foo", nullable = false),
                    mockClassType("org/sample/Foo", nullable = false)
                )
            )
        )
    }

    fun `test class types in user package with same nullability - 2`() {
        assertEquals(
            mockClassType("org/sample/Foo", nullable = true), createCommonizer().invoke(
                listOf(
                    mockClassType("org/sample/Foo", nullable = true),
                    mockClassType("org/sample/Foo", nullable = true),
                    mockClassType("org/sample/Foo", nullable = true)
                )
            )
        )
    }

    fun `test class types in user package with different nullability - 1`() {
        assertEquals(
            null, createCommonizer().invoke(
                listOf(
                    mockClassType("org/sample/Foo", nullable = false),
                    mockClassType("org/sample/Foo", nullable = false),
                    mockClassType("org/sample/Foo", nullable = true)
                )
            )
        )
    }

    fun `test class types in user package with different nullability - 2`() {
        assertEquals(
            null, createCommonizer().invoke(
                listOf(
                    mockClassType("org/sample/Foo", nullable = true),
                    mockClassType("org/sample/Foo", nullable = true),
                    mockClassType("org/sample/Foo", nullable = false)
                )
            )
        )
    }

    fun `test ta types in Kotlin package with same name and class`() {
        val commonizer = createCommonizer(
            commonDependencySources = {
                source(
                    """
                    package kotlin.sequences
                    class SequenceScope
                    typealias SequenceBuilder = SequenceScope
                """.trimIndent(), "sequences.kt"
                )
            }
        )

        assertEquals(mockTAType("kotlin/sequences/SequenceBuilder") { mockClassType("kotlin/sequences/SequenceScope") }, commonizer(
            listOf(
                mockTAType("kotlin/sequences/SequenceBuilder") { mockClassType("kotlin/sequences/SequenceScope") },
                mockTAType("kotlin/sequences/SequenceBuilder") { mockClassType("kotlin/sequences/SequenceScope") },
                mockTAType("kotlin/sequences/SequenceBuilder") { mockClassType("kotlin/sequences/SequenceScope") }
            )))
    }

    fun `test ta types in Kotlin package with different names`() {
        val commonizer = createCommonizer(
            commonDependencySources = {
                source(
                    """
                    package kotlin.sequences
                    class SequenceScope
                    typealias SequenceBuilder = SequenceScope
                """.trimIndent()
                )
            },

            targetCDependencySources = {
                source(
                    """
                    package kotlin.sequences    
                    class SequenceScope
                    typealias FictitiousTypeAlias = SequenceScope
                    """.trimIndent(), "fictitious.kt"
                )
            }
        )
        assertEquals(
            mockClassType("kotlin/sequences/SequenceScope"), commonizer(
                listOf(
                    mockTAType("kotlin/sequences/SequenceBuilder") { mockClassType("kotlin/sequences/SequenceScope") },
                    mockTAType("kotlin/sequences/SequenceBuilder") { mockClassType("kotlin/sequences/SequenceScope") },
                    mockTAType("kotlin/sequences/FictitiousTypeAlias") { mockClassType("kotlin/sequences/SequenceScope") }
                )))
    }

    fun `test ta types in Kotlin package with different classes`() {
        val commonizer = createCommonizer(
            targetADependencySources = {
                source(
                    """
                    package kotlin.sequences
                    class SequenceScope
                    typealias SequenceBuilder = SequenceScope
                """.trimIndent()
                )
            },
            targetBDependencySources = {
                source(
                    """
                    package kotlin.sequences
                    class SequenceScope
                    typealias SequenceBuilder = SequenceScope
                """.trimIndent()
                )
            },
            targetCDependencySources = {
                source(
                    """
                    package kotlin.sequences
                    class FictitiousClass
                    typealias SequenceBuilder = FictitiousClass
                """.trimIndent()
                )
            }
        )
        assertEquals(null, commonizer(
            listOf(
                mockTAType("kotlin/sequences/SequenceBuilder") { mockClassType("kotlin/sequences/SequenceScope") },
                mockTAType("kotlin/sequences/SequenceBuilder") { mockClassType("kotlin/sequences/SequenceScope") },
                mockTAType("kotlin/sequences/SequenceBuilder") { mockClassType("kotlin/sequences/FictitiousClass") }
            )))
    }

    fun `test multilevel ta types in Kotlin package with same name and right hand side class`() {
        val commonizer = createCommonizer(
            targetASpecificSources = {
                source(
                    """
                    package kotlin
                    class FictitiousClass
                    typealias FictitiousTypeAlias = FictitiousClass
                """.trimIndent()
                )
            },
            targetBSpecificSources = {
                source(
                    """
                    package kotlin
                    class FictitiousClass
                    typealias FictitiousTypeAliasL2 = FictitiousClass
                    typealias FictitiousTypeAlias = FictitiousTypeAliasL2
                """.trimIndent()
                )
            },
            targetCSpecificSources = {
                source(
                    """
                    package kotlin
                    class FictitiousClass
                    typealias FictitiousTypeAliasL2 = FictitiousClass
                    typealias FictitiousTypeAliasL3 = FictitiousTypeAliasL2
                    typealias FictitiousTypeAlias = FictitiousTypeAliasL3
                """.trimIndent()
                )
            }
        )
        assertEquals(mockTAType("kotlin/FictitiousTypeAlias") {
            mockClassType("kotlin/FictitiousClass")
        }, commonizer(listOf(

            mockTAType("kotlin/FictitiousTypeAlias") {
                mockClassType("kotlin/FictitiousClass")
            },

            mockTAType("kotlin/FictitiousTypeAlias") {
                mockTAType("kotlin/FictitiousTypeAliasL2") {
                    mockClassType("kotlin/FictitiousClass")
                }
            },

            mockTAType("kotlin/FictitiousTypeAlias") {
                mockTAType("kotlin/FictitiousTypeAliasL2") {
                    mockTAType("kotlin/FictitiousTypeAliasL3") {
                        mockClassType("kotlin/FictitiousClass")
                    }
                }
            }
        )))
    }

    fun `test multilevel ta types in user package with same name and right hand side class - 1`() {
        val commonizer = createCommonizer(
            targetASpecificSources = {
                source(
                    """
                    package org.sample
                    class F
                    typealias FAlias = F
                """.trimIndent()
                )
            },
            targetBSpecificSources = {
                source(
                    """
                    package org.sample
                    class F
                    typealias FAlias = F
                """.trimIndent()
                )
            },
            targetCSpecificSources = {
                source(
                    """
                    package org.sample
                    class F
                    typealias FAliasL2 = F
                    typealias FAlias = FAliasL2
                """.trimIndent()
                )
            }
        )
        assertEquals(
            mockTAType("org/sample/FAlias") {
                mockClassType("org/sample/F")
            }, commonizer(
                listOf(
                    mockTAType("org/sample/FAlias") {
                        mockClassType("org/sample/F")
                    },
                    mockTAType("org/sample/FAlias") {
                        mockClassType("org/sample/F")
                    },
                    mockTAType("org/sample/FAlias") {
                        mockTAType("org/sample/FAliasL2") {
                            mockClassType("org/sample/F")
                        }
                    })
            )
        )
    }

    fun `test multilevel ta types in user package with same name and right hand side class - 3`() {
        val commonizer = createCommonizer(
            commonTargetSources = {
                source(
                    """
                    package org.sample
                    class Foo
                    typealias FooAliasL2 = Foo
                    typealias FooAlias = FooAliasL2
                """.trimIndent()
                )
            }
        )
        assertEquals(
            mockTAType("org/sample/FooAlias") {
                mockTAType("org/sample/FooAliasL2") {
                    mockClassType("org/sample/Foo")
                }
            }, commonizer(
                listOf(
                    mockTAType("org/sample/FooAlias") {
                        mockTAType("org/sample/FooAliasL2") {
                            mockClassType("org/sample/Foo")
                        }
                    },
                    mockTAType("org/sample/FooAlias") {
                        mockTAType("org/sample/FooAliasL2") {
                            mockClassType("org/sample/Foo")
                        }
                    },

                    mockTAType("org/sample/FooAlias") {
                        mockTAType("org/sample/FooAliasL2") {
                            mockClassType("org/sample/Foo")
                        }
                    })
            )
        )
    }

    fun `test ta types in Kotlin package with different nullability`() {
        val commonizer = createCommonizer(
            targetADependencySources = {
                source(
                    """
                    package kotlin.sequences
                    class SequenceScope
                    typealias SequenceBuilder = SequenceScope
                """.trimIndent()
                )
            },
            targetBDependencySources = {
                source(
                    """
                    package kotlin.sequences
                    class SequenceScope
                    typealias SequenceBuilder = SequenceScope
                """.trimIndent()
                )
            },
            targetCDependencySources = {
                source(
                    """
                    package kotlin.sequences
                    class SequenceScope
                    typealias SequenceBuilder = SequenceScope?
                """.trimIndent()
                )
            }
        )
        assertEquals(null, commonizer(listOf(
            mockTAType("kotlin/sequences/SequenceBuilder") { mockClassType("kotlin/sequences/SequenceScope", nullable = false) },
            mockTAType("kotlin/sequences/SequenceBuilder") { mockClassType("kotlin/sequences/SequenceScope", nullable = false) },
            mockTAType("kotlin/sequences/SequenceBuilder") { mockClassType("kotlin/sequences/SequenceScope", nullable = true) }
        )))
    }

    fun `test ta types in user package with same nullability`() {
        val commonizer = createCommonizer(
            commonTargetSources = {
                source(
                    """
                    package org.sample
                    class Foo
                    typealias FooAlias = Foo?
                    """.trimIndent()
                )
            }
        )
        assertEquals(mockTAType("org/sample/FooAlias", nullable = true) { mockClassType("org/sample/Foo") }, commonizer(listOf(
            mockTAType("org/sample/FooAlias", nullable = true) { mockClassType("org/sample/Foo") },
            mockTAType("org/sample/FooAlias", nullable = true) { mockClassType("org/sample/Foo") },
            mockTAType("org/sample/FooAlias", nullable = true) { mockClassType("org/sample/Foo") }
        )))
    }


    companion object {
        fun areEqual(classifiers: CirKnownClassifiers, a: CirType, b: CirType): Boolean =
            TypeCommonizer(classifiers, DefaultCommonizerSettings).invoke(listOf(a, b)) != null
    }
}

