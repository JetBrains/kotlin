/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package hair.ir

import hair.ir.nodes.Node

fun Session.verify() {
    val uses = allNodes().associateWith { mutableListOf<Node>() }
    for (node in allNodes()) {
        for (arg in node.args) {
            if (arg != null) uses[arg]!!.add(node)
        }
    }
    fun sameElements(a: List<Node>, b: List<Node>) =
        a.groupingBy { it }.eachCount() == b.groupingBy { it }.eachCount()
    for ([node, uses] in uses) {
        require(sameElements(node.uses.toList(), uses)) {
            "Node $node has invalid uses list: ${node.uses.toList()}, expected $uses"
        }
    }

    // TODO other verifications
}
