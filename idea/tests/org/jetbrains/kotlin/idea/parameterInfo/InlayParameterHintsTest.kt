/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.parameterInfo

import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor

class InlayParameterHintsTest : KotlinLightCodeInsightFixtureTestCase() {
    override fun getProjectDescriptor() = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    fun check(text: String) {
        myFixture.configureByText("A.kt", text)
        myFixture.testInlays()
    }

    fun `test insert literal arguments`() {
        check("""
 fun test(file: File) {
  val testNow = System.currentTimeMillis() > 34000
  val times = 1
  val pi = 4.0f
  val title = "Testing..."
  val ch = 'q';

  configure(<hint text="testNow:" />true, <hint text="times:" />555, <hint text="pii:" />3.141f, <hint text="title:" />"Huge Title", <hint text="terminate:" />'c', <hint text="file:" />null)
  configure(testNow, shouldIgnoreRoots(), fourteen, pi, title, c, file)
 }

 fun configure(testNow: Boolean, times: Int, pii: Float, title: String, terminate: Char, file: File) {
  System.out.println()
  System.out.println()
 }""")
    }

    fun `test do not show for Exceptions`() {
        check("""
  fun test() {
    val t = IllegalStateException("crime");
  }""")
    }
}
