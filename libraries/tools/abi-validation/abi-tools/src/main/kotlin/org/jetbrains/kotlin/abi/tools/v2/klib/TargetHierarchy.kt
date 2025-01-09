/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abi.tools.v2.klib

/**
 * A hierarchy of KMP targets that should resemble the default hierarchy template.
 */
internal object TargetHierarchy {
    class Node(val name: String, vararg childrenNodes: Node) {
        var parent: Node? = null
        val children = childrenNodes.toList().toTypedArray()

        init {
            childrenNodes.forEach {
                it.parent = this
            }
        }
    }

    data class NodeClosure(val node: Node, val depth: Int, val allLeafs: Set<String>)

    internal val hierarchyIndex: Map<String, NodeClosure>

    private val hierarchy = Node(
        "all",
        Node("js"),
        Node("wasmJs"),
        Node("wasmWasi"),
        Node(
            "native",
            Node(
                "mingw",
                Node("mingwX64"),
                Node("mingwX86")
            ),
            Node(
                "linux",
                Node("linuxArm64"),
                Node("linuxArm32Hfp"),
                Node("linuxX64"),
            ),
            Node(
                "androidNative",
                Node("androidNativeArm64"),
                Node("androidNativeArm32"),
                Node("androidNativeX86"),
                Node("androidNativeX64")
            ),
            Node(
                "apple",
                Node(
                    "macos",
                    Node("macosArm64"),
                    Node("macosX64")
                ),
                Node(
                    "ios",
                    Node("iosArm64"),
                    Node("iosArm32"),
                    Node("iosX64"),
                    Node("iosSimulatorArm64")
                ),
                Node(
                    "tvos",
                    Node("tvosArm64"),
                    Node("tvosX64"),
                    Node("tvosSimulatorArm64")
                ),
                Node(
                    "watchos",
                    Node("watchosArm32"),
                    Node("watchosArm64"),
                    Node("watchosX64"),
                    Node("watchosSimulatorArm64"),
                    Node("watchosDeviceArm64"),
                    Node("watchosX86")
                )
            )
        )
    )

    private fun Node.collectLeafs(to: MutableMap<String, NodeClosure>, depth: Int): Set<String> {
        val leafs = mutableSetOf<String>()
        if (children.isEmpty()) {
            leafs.add(name)
        } else {
            children.forEach { leafs.addAll(it.collectLeafs(to, depth + 1)) }
        }
        to[name] = NodeClosure(this, depth, leafs)
        return leafs
    }

    init {
        val index = mutableMapOf<String, NodeClosure>()
        val rootDepth = 0
        val leafs = hierarchy.collectLeafs(index, rootDepth + 1)
        index[hierarchy.name] = NodeClosure(hierarchy, rootDepth, leafs)
        hierarchyIndex = index
    }

    fun parent(targetOrGroup: String): String? {
        return hierarchyIndex[targetOrGroup]?.node?.parent?.name
    }

    fun targets(targetOrGroup: String): Set<String> {
        return hierarchyIndex[targetOrGroup]?.allLeafs ?: emptySet()
    }
}
