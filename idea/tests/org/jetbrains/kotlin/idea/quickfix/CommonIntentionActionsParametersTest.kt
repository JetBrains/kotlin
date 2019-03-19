/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.lang.jvm.actions.*
import com.intellij.lang.jvm.types.JvmType
import com.intellij.psi.PsiType
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.uast.UMethod

class CommonIntentionActionsParametersTest : LightPlatformCodeInsightFixtureTestCase() {

    override fun getProjectDescriptor() = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE_FULL_JDK

    fun testSetParameters() {
        myFixture.configureByText(
            "foo.kt",
            """
            class Foo {
                fun ba<caret>r() {}
            }
            """.trimIndent()
        )

        myFixture.launchAction(
            createChangeParametersActions(
                myFixture.atCaret<UMethod>().javaPsi,
                setMethodParametersRequest(
                    linkedMapOf<String, JvmType>(
                        "i" to PsiType.INT,
                        "file" to PsiType.getTypeByName("java.io.File", project, myFixture.file.resolveScope)
                    ).entries
                )
            ).findWithText("Change method parameters to '(i: Int, file: File)'")
        )
        myFixture.checkResult(
            """
            import java.io.File

            class Foo {
                fun bar(i: Int, file: File) {}
            }
            """.trimIndent(),
            true
        )
    }

}

