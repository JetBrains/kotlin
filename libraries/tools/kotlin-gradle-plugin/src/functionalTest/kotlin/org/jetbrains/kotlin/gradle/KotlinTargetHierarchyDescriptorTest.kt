/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetHierarchyDescriptor
import org.jetbrains.kotlin.gradle.plugin.mpp.targetHierarchy.CyclicKotlinTargetHierarchyException
import org.jetbrains.kotlin.gradle.plugin.mpp.targetHierarchy.KotlinTargetHierarchy
import org.jetbrains.kotlin.gradle.plugin.mpp.targetHierarchy.KotlinTargetHierarchy.Node
import org.jetbrains.kotlin.gradle.plugin.mpp.targetHierarchy.buildKotlinTargetHierarchy
import org.junit.Test
import kotlin.test.*

class KotlinTargetHierarchyDescriptorTest {

    private val project = buildProjectWithMPP()
    private val kotlin = project.multiplatformExtension

    @Test
    fun `test - simple descriptor`() {
        val descriptor = KotlinTargetHierarchyDescriptor {
            common {
                group("groupA") { addCompilations { it.target.name == "a" } }
                group("groupB") { addCompilations { it.target.name == "b" } }
            }
        }

        val targetA = kotlin.linuxX64("a")
        val targetB = kotlin.linuxArm64("b")
        val targetC = kotlin.jvm()

        assertEquals(
            hierarchy {
                group("common") {
                    group("groupA")
                }
            },
            descriptor.buildKotlinTargetHierarchy(targetA.compilations.main)
        )

        assertEquals(
            hierarchy {
                group("common") {
                    group("groupB")
                }
            },
            descriptor.buildKotlinTargetHierarchy(targetB.compilations.main)
        )

        /* targetC is not mentioned in hierarchy description */
        assertNull(
            descriptor.buildKotlinTargetHierarchy(targetC.compilations.main)
        )
    }

    @Test
    fun `test - extend`() {
        val descriptor = KotlinTargetHierarchyDescriptor { group("base") }.extend {
            group("base") {
                group("extension") {
                    addCompilations { true }
                }
            }
        }

        assertEquals(
            hierarchy {
                group("base") {
                    group("extension")
                }
            },
            descriptor.buildKotlinTargetHierarchy(kotlin.linuxX64().compilations.getByName("main"))
        )
    }


    @Test
    fun `test - extend - with new root`() {
        val descriptor = KotlinTargetHierarchyDescriptor { group("base") }.extend {
            group("newRoot") {
                group("base") {
                    group("extension") {
                        addCompilations { true }
                    }
                }
            }
        }

        assertEquals(
            hierarchy {
                group("newRoot") {
                    group("base") {
                        group("extension")
                    }
                }
            },
            descriptor.buildKotlinTargetHierarchy(kotlin.linuxX64().compilations.main)
        )
    }


    @Test
    fun `test - extend - with two new roots and two extensions`() {
        val descriptor = KotlinTargetHierarchyDescriptor { group("base") }
            .extend {
                group("newRoot1") {
                    group("base") {
                        group("extension1") {
                            addCompilations { true }
                        }
                    }
                }
            }
            .extend {
                group("newRoot2") {
                    group("base") {
                        group("extension2") {
                            addCompilations { true }
                        }
                    }
                }
            }

        val hierarchy = assertNotNull(descriptor.buildKotlinTargetHierarchy(kotlin.linuxX64().compilations.main))

        assertEquals(
            hierarchy {
                group("newRoot1") {
                    group("base") {
                        group("extension1")
                        group("extension2")
                    }
                }

                group("newRoot2") {
                    group("base") {
                        group("extension1")
                        group("extension2")
                    }
                }
            }, hierarchy
        )

        fun KotlinTargetHierarchy.collectChildren(): List<KotlinTargetHierarchy> {
            return children.toList() + children.flatMap { it.collectChildren() }
        }

        /* Check that all equal hierarchies are even the same instance */
        val allNodes = hierarchy.collectChildren()
        allNodes.forEach { node ->
            val equalNodes = allNodes.filter { otherNode -> otherNode == node }
            equalNodes.forEach { equalNode ->
                assertSame(node, equalNode, "Expected equal nodes to be the same instance")
            }
        }
    }

    @Test
    fun `test - cycle`() {
        val descriptor = KotlinTargetHierarchyDescriptor {
            group("x") { // decoy 1
                group("a") {
                    group("xx") // decoy 2

                    group("b") {
                        group("xxx") // decoy 3

                        group("c") {
                            group("a") {
                                group("xxxx") // decoy 4
                            }
                        }
                    }
                }
            }
        }

        val cycleStack = assertFailsWith<CyclicKotlinTargetHierarchyException> {
            descriptor.buildKotlinTargetHierarchy(kotlin.linuxX64().compilations.main)
        }.cycle

        assertEquals(
            listOf(Node.Group("a"), Node.Group("b"), Node.Group("c"), Node.Group("a")),
            cycleStack
        )
    }

    @Test
    fun `test - filterCompilations`() {
        val descriptor = KotlinTargetHierarchyDescriptor {
            filterCompilations { it.name in setOf("a", "b") }
            common {
                group("x") {
                    addCompilations { true }
                }
            }
        }

        assertEquals(
            hierarchy {
                group("common") {
                    group("x")
                }
            },
            descriptor.buildKotlinTargetHierarchy(kotlin.linuxX64().compilations.maybeCreate("a"))
        )

        assertEquals(
            descriptor.buildKotlinTargetHierarchy(kotlin.linuxX64().compilations.maybeCreate("a")),
            descriptor.buildKotlinTargetHierarchy(kotlin.linuxX64().compilations.maybeCreate("b"))
        )

        assertNull(
            descriptor.buildKotlinTargetHierarchy(kotlin.linuxX64().compilations.maybeCreate("c"))
        )
    }

    @Test
    fun `test - filterCompilations - include them again`() {
        val descriptor = KotlinTargetHierarchyDescriptor {
            addCompilations { true }
            filterCompilations { it.name == "a" }
        }

        assertNotNull(descriptor.buildKotlinTargetHierarchy(kotlin.linuxX64().compilations.maybeCreate("a")))
        assertNull(descriptor.buildKotlinTargetHierarchy(kotlin.linuxX64().compilations.maybeCreate("b")))

        val extended = descriptor.extend {
            addCompilations { true } // <- adds all compilations back again!
        }

        assertNull(descriptor.buildKotlinTargetHierarchy(kotlin.linuxX64().compilations.maybeCreate("b")))
        assertNotNull(extended.buildKotlinTargetHierarchy(kotlin.linuxX64().compilations.maybeCreate("b")))
    }
}

private class TestHierarchyBuilder(private val node: Node) {
    private val children = mutableSetOf<TestHierarchyBuilder>()

    fun group(name: String, builder: TestHierarchyBuilder.() -> Unit = {}) {
        children.add(TestHierarchyBuilder(Node.Group(name)).also(builder))
    }

    fun build(): KotlinTargetHierarchy = KotlinTargetHierarchy(node, children.map { it.build() }.toSet())
}

private fun hierarchy(build: TestHierarchyBuilder.() -> Unit): KotlinTargetHierarchy {
    return TestHierarchyBuilder(Node.Root).also(build).build()
}
