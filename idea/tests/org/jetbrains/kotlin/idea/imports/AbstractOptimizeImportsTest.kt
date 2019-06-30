/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.imports

import com.intellij.testFramework.LightProjectDescriptor
import junit.framework.TestCase
import org.jetbrains.kotlin.AbstractImportsTest
import org.jetbrains.kotlin.idea.test.KotlinStdJSProjectDescriptor
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.InTextDirectivesUtils

abstract class AbstractOptimizeImportsTest : AbstractImportsTest() {
    override fun doTest(file: KtFile): String {
        OptimizedImportsBuilder.testLog = StringBuilder()
        try {
            val optimizer = KotlinImportOptimizer().processFile(file)
            optimizer.run()
            val message = InTextDirectivesUtils.findStringWithPrefixes(file.text, "// WITH_MESSAGE: ")
            if (message != null) {
                TestCase.assertEquals(message, optimizer.userNotificationInfo)
            }
            return OptimizedImportsBuilder.testLog.toString()
        } finally {
            OptimizedImportsBuilder.testLog = null
        }
    }

    override val nameCountToUseStarImportDefault: Int
        get() = Integer.MAX_VALUE
}

abstract class AbstractJvmOptimizeImportsTest : AbstractOptimizeImportsTest() {
    override fun getProjectDescriptor(): LightProjectDescriptor =
        if (fileName().endsWith(".kts")) KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE_WITH_SCRIPT_RUNTIME
        else KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE_FULL_JDK
}

abstract class AbstractJsOptimizeImportsTest : AbstractOptimizeImportsTest() {
    override fun getProjectDescriptor() = KotlinStdJSProjectDescriptor
}
