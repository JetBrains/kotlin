/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.tree.deserializer

import org.jetbrains.kotlin.commonizer.cir.CirClassType
import org.jetbrains.kotlin.commonizer.utils.createCirTree
import org.jetbrains.kotlin.descriptors.ClassKind
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CirTreeDependenciesDeserializerTest : AbstractCirTreeDeserializerTest() {

    fun `test deserialize with dependencies`() {
        val module = createCirTree {
            dependency {
                source("interface Dependency", "dependencies.kt")
            }
            source("object X: Dependency", "source.kt")
        }

        val xObject = module.assertSingleClass()
        assertEquals(ClassKind.OBJECT, xObject.clazz.kind, "Expected object 'X' to be object")
        assertEquals(1, xObject.clazz.supertypes.size, "Expected single supertype. Found ${xObject.clazz.supertypes}")

        val dependencyType = assertIs<CirClassType>(xObject.clazz.supertypes.single(), "Expected 'Dependency' to be class")
        assertEquals("/Dependency", dependencyType.classifierId.toString())
    }
}
