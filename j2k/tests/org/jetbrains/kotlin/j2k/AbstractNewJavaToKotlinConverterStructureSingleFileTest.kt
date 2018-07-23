/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k

import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File


abstract class AbstractNewJavaToKotlinConverterStructureSingleFileTest : AbstractNewJavaToKotlinConverterSingleFileTest() {

    override fun compareResults(expectedFile: File, actual: String) {
        KotlinTestUtils.assertEqualsToFile(expectedFile, actual) {
            val file = createKotlinFile(it)
            file.dumpStructureText()
        }
    }
}