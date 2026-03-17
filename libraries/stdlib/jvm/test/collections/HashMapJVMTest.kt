/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.collections

import kotlin.random.Random
import kotlin.test.Test

// Most likely not to be merged, but shows the performance improvement well.

// Sample result:
//Collisions at 8 entries: limited map - 6, regular map - 0
//Collisions at 16 entries: limited map - 6, regular map - 0
//Collisions at 32 entries: limited map - 26, regular map - 0
//Collisions at 64 entries: limited map - 101, regular map - 0
//Collisions at 128 entries: limited map - 494, regular map - 0
//Collisions at 256 entries: limited map - 956, regular map - 0
//Collisions at 512 entries: limited map - 1859, regular map - 0
//Collisions at 1024 entries: limited map - 3841, regular map - 0
//Collisions at 2048 entries: limited map - 7762, regular map - 0
//Collisions at 4096 entries: limited map - 15300, regular map - 0
//Collisions at 8192 entries: limited map - 15216, regular map - 0
//Collisions at 16384 entries: limited map - 15095, regular map - 0
//Collisions at 32768 entries: limited map - 14308, regular map - 0
//Collisions at 65536 entries: limited map - 13582, regular map - 0
//Collisions at 131072 entries: limited map - 26965, regular map - 0
//Collisions at 262144 entries: limited map - 53569, regular map - 0
//Collisions at 524288 entries: limited map - 107407, regular map - 0
//Collisions at 1048576 entries: limited map - 461493, regular map - 0

class HashMapJVMTest {
    @Test
    fun compareCollisionsAtDifferentNumberOfEntries() {
        (3..20).forEach { compareCollisionsPerNumberOfEntries(1 shl it) }
    }

    private fun compareCollisionsPerNumberOfEntries(entriesCount: Int) {
        val limitedMap = hashMapOf<Long, String>()
        val regularMap = hashMapOf<Long, String>()

        repeat(entriesCount) {
            val signatureId = Random.nextInt(0, entriesCount)
            // Mocked current behavior
            limitedMap[encode(signatureId)] = "$it"
            // Mocked fixed behavior
            regularMap[signatureId.toLong()] = "$it"
        }

        val tableField = HashMap::class.java.getDeclaredField("table").apply { isAccessible = true }
        val limitedMapBuckets = (tableField.get(limitedMap) as Array<*>).map(::getNodeBucketList)
        val regularMapBuckets = (tableField.get(regularMap) as Array<*>).map(::getNodeBucketList)

        val limitedMapCollisions = limitedMapBuckets.sumOf { it.countCollisions() }
        val regularMapCollisions = regularMapBuckets.sumOf { it.countCollisions() }

//        limitedMapBuckets.forEach { println(it) }

        println("Collisions at $entriesCount entries: limited map - $limitedMapCollisions, regular map - $regularMapCollisions")
    }

    private fun getNodeBucketList(node: Any?): List<Node> {
        val nodeClass = node?.javaClass ?: return emptyList()

        return if (nodeClass.simpleName == "TreeNode") {
            getTreeNodeList(node)
        } else {
            getNodeList(node)
        }
    }

    private fun getNodeList(node: Any): List<Node> {
        val nodeClass = node.javaClass

        val keyField = nodeClass.getDeclaredField("key").apply { isAccessible = true }
        val valueField = nodeClass.getDeclaredField("value").apply { isAccessible = true }
        val nextField = nodeClass.getDeclaredField("next").apply { isAccessible = true }

        val result = mutableListOf<Node>()
        var current: Any? = node

        while (current != null) {
            result.add(Node(keyField.get(current) as Long, valueField.get(current) as String))
            current = nextField.get(current)
        }
        return result
    }

    private fun getTreeNodeList(treeNode: Any): List<Node> {
        val nodeClass = treeNode.javaClass

        val keyField = findField(nodeClass, "key")
        val valueField = findField(nodeClass, "value")
        val leftField = findField(nodeClass, "left")
        val rightField = findField(nodeClass, "right")

        val result = mutableListOf<Node>()

        fun traverse(current: Any?) {
            if (current == null) return

            traverse(leftField.get(current))
            result.add(Node(keyField.get(current) as Long, valueField.get(current) as String))
            traverse(rightField.get(current))
        }

        traverse(treeNode)

        return result
    }

    private fun findField(clazz: Class<*>, name: String): java.lang.reflect.Field {
        var currentClass: Class<*>? = clazz
        while (currentClass != null) {
            try {
                return currentClass.getDeclaredField(name).apply { isAccessible = true }
            } catch (e: NoSuchFieldException) {
                currentClass = currentClass.superclass
            }
        }
        throw NoSuchFieldException("Field $name not found in $clazz or its superclasses")
    }

    data class Node(val key: Long, val value: String)

    private fun List<Node>.countCollisions() =
        size * (size - 1) / 2

    // Mock BinarySymbolData.encode()
    private fun encode(signatureId: Int): Long {
        val kindId = Random.nextInt(0, 16)
        return (signatureId.toLong() shl 8) or kindId.toLong()
    }
}
