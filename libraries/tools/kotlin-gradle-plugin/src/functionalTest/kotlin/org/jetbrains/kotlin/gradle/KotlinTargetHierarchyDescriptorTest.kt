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
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class KotlinTargetHierarchyDescriptorTest {

    private val project = buildProjectWithMPP()
    private val kotlin = project.multiplatformExtension

    @Test
    fun `test - simple descriptor`() {
        val descriptor = KotlinTargetHierarchyDescriptor {
            common {
                if (target.name == "a") group("groupA")
                if (target.name == "b") group("groupB")
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

        assertEquals(
            hierarchy { group("common") },
            descriptor.buildKotlinTargetHierarchy(targetC.compilations.main)
        )
    }

    @Test
    fun `test - extend`() {
        val descriptor = KotlinTargetHierarchyDescriptor { group("base") }.extend {
            group("base") {
                group("extension")
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
                    group("extension")
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
                        group("extension1")
                    }
                }
            }
            .extend {
                group("newRoot2") {
                    group("base") {
                        group("extension2")
                    }
                }
            }

        val hierarchy = descriptor.buildKotlinTargetHierarchy(kotlin.linuxX64().compilations.main)

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