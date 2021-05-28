/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.transformer

import org.jetbrains.kotlin.commonizer.cir.CirModule
import org.jetbrains.kotlin.commonizer.cir.CirName
import org.jetbrains.kotlin.commonizer.cir.markedArtificial
import org.jetbrains.kotlin.commonizer.mergedtree.buildModuleNode
import org.jetbrains.kotlin.commonizer.mergedtree.buildRootNode
import org.jetbrains.kotlin.commonizer.transformer.Checked.Companion.invoke
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class RunTransformationTest {

    private val storageManager = LockBasedStorageManager("test")

    @Test
    fun `fails when attaching non-artificial declarations`() {
        val badTransformer = CirNodeTransformer { root ->
            root.modules[CirName.create("bad-module")] = buildModuleNode(storageManager, 1).also { moduleNode ->
                moduleNode.targetDeclarations[0] = CirModule.create(CirName.create("bad-module"))
            }
        }

        val node = buildRootNode(storageManager, 0)
        assertFailsWith<AssertionError> { badTransformer(node) }
    }

    @Test
    fun `when attaching non-artificial declarations`() {
        val goodTransformer = CirNodeTransformer { root ->
            assertIs<Checked>(this, "Expected 'Checked' context")

            root.modules[CirName.create("artificial-module")] = buildModuleNode(storageManager, 1).also { moduleNode ->
                moduleNode.targetDeclarations[0] = CirModule.create(CirName.create("artificial-module")).markedArtificial()
            }
        }

        val node = buildRootNode(storageManager, 0)
        goodTransformer(node)

        assertEquals(CirName.create("artificial-module"), node.modules.values.single().targetDeclarations.single()?.name)
    }
}