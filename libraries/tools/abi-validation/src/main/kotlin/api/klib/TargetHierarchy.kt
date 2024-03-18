/*
 * Copyright 2016-2024 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.validation.api.klib

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

internal val konanTargetNameMapping = mapOf(
    "android_x64" to "androidNativeX64",
    "android_x86" to "androidNativeX86",
    "android_arm32" to "androidNativeArm32",
    "android_arm64" to "androidNativeArm64",
    "ios_arm64" to "iosArm64",
    "ios_x64" to "iosX64",
    "ios_simulator_arm64" to "iosSimulatorArm64",
    "watchos_arm32" to "watchosArm32",
    "watchos_arm64" to "watchosArm64",
    "watchos_x64" to "watchosX64",
    "watchos_simulator_arm64" to "watchosSimulatorArm64",
    "watchos_device_arm64" to "watchosDeviceArm64",
    "tvos_arm64" to "tvosArm64",
    "tvos_x64" to "tvosX64",
    "tvos_simulator_arm64" to "tvosSimulatorArm64",
    "linux_x64" to "linuxX64",
    "mingw_x64" to "mingwX64",
    "macos_x64" to "macosX64",
    "macos_arm64" to "macosArm64",
    "linux_arm64" to "linuxArm64",
    "ios_arm32" to "iosArm32",
    "watchos_x86" to "watchosX86",
    "linux_arm32_hfp" to "linuxArm32Hfp",
    "mingw_x86" to "mingwX86"
)
