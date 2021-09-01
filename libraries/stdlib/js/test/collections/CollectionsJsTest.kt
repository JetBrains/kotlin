/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.collections.js

import kotlin.test.*

class CollectionsJsTest {
    @Test
    fun toJSON() {
        val list = arrayListOf("array", "List", "Of")
        assertContentEquals(arrayOf("array", "List", "Of"), list.asDynamic().toJSON() as Array<String>)

        val set = linkedSetOf("linked", "Set", "Of")
        assertContentEquals(arrayOf("linked", "Set", "Of"), (set as AbstractMutableCollection<String>).asDynamic().toJSON() as Array<String>)
    }
}