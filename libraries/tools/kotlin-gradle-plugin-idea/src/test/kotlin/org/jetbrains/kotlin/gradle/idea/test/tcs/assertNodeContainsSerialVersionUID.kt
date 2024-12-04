/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.test.tcs

import org.jetbrains.kotlin.gradle.idea.test.tcs.ReflectionTestUtils.displayName
import org.jetbrains.kotlin.gradle.idea.test.tcs.ReflectionTestUtils.getDeclaredFieldOrNull
import java.lang.reflect.Modifier
import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

fun assertNodeContainsSerialVersionUID(node: KClass<*>) {
    if (!node.java.isInterface && !Modifier.isAbstract(node.java.modifiers)) {
        val serialVersionUID = assertNotNull(
            node.java.getDeclaredFieldOrNull("serialVersionUID"),
            "Expected ${node.displayName()} to declare 'serialVersionUID' field"
        )

        assertTrue(
            Modifier.isStatic(serialVersionUID.modifiers),
            "Expected ${node.displayName()} to declare 'serialVersionUID' statically"
        )

        assertTrue(
            serialVersionUID.type.isPrimitive,
            "Expected ${node.displayName()} to declare primitive 'serialVersionUID'"
        )

        assertEquals(
            serialVersionUID.type, Long::class.javaPrimitiveType,
            "Expected ${node.displayName()} to declare 'serialVersionUID' of type Long"
        )
    }
}
