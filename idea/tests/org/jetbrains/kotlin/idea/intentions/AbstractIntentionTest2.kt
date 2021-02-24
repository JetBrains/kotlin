/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import java.io.File

abstract class AbstractIntentionTest2 : AbstractIntentionTest() {
    override fun intentionFileName() = ".intention2"
    override fun afterFileNameSuffix(ktFilePath: File) = ".after2"
    override fun intentionTextDirectiveName() = "INTENTION_TEXT_2"
    override fun isApplicableDirectiveName() = "IS_APPLICABLE_2"
}
