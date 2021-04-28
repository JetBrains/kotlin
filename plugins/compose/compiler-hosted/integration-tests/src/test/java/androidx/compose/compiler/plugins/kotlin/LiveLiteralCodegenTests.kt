/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.compiler.plugins.kotlin

import android.widget.TextView
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.internal.updateLiveLiteralValue
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(
    manifest = Config.NONE,
    minSdk = 23,
    maxSdk = 23
)
class LiveLiteralCodegenTests : AbstractLoweringTests() {

    override fun updateConfiguration(configuration: CompilerConfiguration) {
        super.updateConfiguration(configuration)
        configuration.put(ComposeConfiguration.LIVE_LITERALS_ENABLED_KEY, true)
    }

    @Ignore("Live literals are currently disabled by default")
    @Test
    fun testBasicFunctionality(): Unit = ensureSetup {
        compose(
            """
            @Composable
            fun App() {
                TextView(id=1, text="abc")
            }
        """,
            "App()"
        ).then { activity ->
            val tv = activity.findViewById<TextView>(1)
            assertEquals("abc", tv.text)
            @OptIn(InternalComposeApi::class)
            updateLiveLiteralValue("String\$arg-2\$call-TextView\$fun-App", "def")
        }.then { activity ->
            val tv = activity.findViewById<TextView>(1)
            assertEquals("def", tv.text)
        }
    }

    @Ignore("Live literals are currently disabled by default")
    @Test
    fun testObjectFieldsLoweredToStaticFields(): Unit = ensureSetup {
        validateBytecode(
            """
            fun Test(): Int {
                return 123
            }
        """
        ) {
            // If we end up with PUTFIELD/GETFIELD then it means that the object-property ->
            // static field lowering didn't succeed for some reason
            assert(it.contains("PUTSTATIC"))
            assert(!it.contains("PUTFIELD"))
            assert(it.contains("GETSTATIC"))
            assert(!it.contains("GETFIELD"))
        }
    }
}