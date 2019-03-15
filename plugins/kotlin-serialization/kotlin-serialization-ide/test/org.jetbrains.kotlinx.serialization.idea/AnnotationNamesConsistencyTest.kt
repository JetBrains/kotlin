/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlinx.serialization.idea

import junit.framework.TestCase
import org.jetbrains.kotlin.idea.caches.lightClasses.annotations.KOTLINX_SERIALIZABLE_FQ_NAME
import org.jetbrains.kotlin.idea.caches.lightClasses.annotations.KOTLINX_SERIALIZER_FQ_NAME
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializationAnnotations


class AnnotationNamesConsistencyTest : TestCase() {
    fun testConsistency() {
        assertEquals(KOTLINX_SERIALIZABLE_FQ_NAME, SerializationAnnotations.serializableAnnotationFqName)
        assertEquals(KOTLINX_SERIALIZER_FQ_NAME, SerializationAnnotations.serializerAnnotationFqName)
    }
}
