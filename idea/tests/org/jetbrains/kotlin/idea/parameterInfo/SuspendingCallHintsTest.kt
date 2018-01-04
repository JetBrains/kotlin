/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.parameterInfo

import org.jetbrains.kotlin.idea.actions.internal.KotlinInternalMode
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor

class SuspendingCallHintsTest : KotlinLightCodeInsightFixtureTestCase() {
    override fun getProjectDescriptor(): KotlinLightProjectDescriptor = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    fun check(text: String) {
        KotlinInternalMode.enabled = true
        try {
            HintType.SUSPENDING_CALL.option.set(true)
            myFixture.configureByText("A.kt", text)
            myFixture.testInlays()
        } finally {
            KotlinInternalMode.enabled = false
        }
    }

    fun testSimple() {
        check(
            """import kotlin.coroutines.experimental.buildSequence

             val x = buildSequence {<hint text="this: SequenceBuilder<Int>" />
                 <hint text="#" />yield(<hint text="value:" />1)
             } """
        )
    }
}
