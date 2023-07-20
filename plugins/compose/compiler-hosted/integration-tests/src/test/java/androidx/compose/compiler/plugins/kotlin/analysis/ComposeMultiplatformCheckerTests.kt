/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.compiler.plugins.kotlin.analysis

import androidx.compose.compiler.plugins.kotlin.AbstractComposeDiagnosticsTest
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.config.languageVersionSettings
import org.junit.Test

class ComposeMultiplatformCheckerTests(useFir: Boolean) : AbstractComposeDiagnosticsTest(useFir) {
    override fun CompilerConfiguration.updateConfiguration() {
        languageVersionSettings = LanguageVersionSettingsImpl(
            languageVersionSettings.languageVersion,
            languageVersionSettings.apiVersion,
            specificFeatures = hashMapOf(
                LanguageFeature.MultiPlatformProjects to LanguageFeature.State.ENABLED
            )
        )
    }

    @Test
    fun testExpectActualMatching() {
        check(
            """
                import androidx.compose.runtime.Composable
                actual fun <!MISMATCHED_COMPOSABLE_IN_EXPECT_ACTUAL!>A<!>() {}
                @Composable actual fun <!MISMATCHED_COMPOSABLE_IN_EXPECT_ACTUAL!>B<!>() {}
            """,
            """
                import androidx.compose.runtime.Composable
                @Composable expect fun A()
                expect fun B()
            """,
        )
    }
}
