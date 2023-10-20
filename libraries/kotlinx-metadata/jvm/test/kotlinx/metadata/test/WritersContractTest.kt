/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.test

import kotlinx.metadata.jvm.KotlinClassMetadata
import org.junit.Ignore
import org.junit.Test
import kotlin.test.assertFailsWith

class WritersContractTest {
    val classMd = WritersContractTest::class.java.getMetadata()
    val l: () -> Unit = {}
    val lambdaMd = l::class.java.getMetadata()
//    val fileFacadeMd = TODO

    @Test
    @Ignore // TODO
    fun lenientDataCantBeWritten() {
        val lenientClass = KotlinClassMetadata.readLenient(classMd) as KotlinClassMetadata.Class
        assertFailsWith<IllegalArgumentException> { KotlinClassMetadata.writeClass(lenientClass.kmClass) }
    }

    @Test
    fun oldVersionCantBeWritten() {
        val writeableClass = KotlinClassMetadata.readStrict(classMd) as KotlinClassMetadata.Class
        assertFailsWith<IllegalArgumentException> { KotlinClassMetadata.writeClass(writeableClass.kmClass, intArrayOf(1, 2, 0)) }
    }
}
