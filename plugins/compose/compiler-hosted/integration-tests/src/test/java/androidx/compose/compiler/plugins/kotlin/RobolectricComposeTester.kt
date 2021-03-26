/*
 * Copyright 2020 The Android Open Source Project
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

import android.app.Activity
import android.os.Bundle
import android.os.Looper.getMainLooper
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composer
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import org.robolectric.Robolectric
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

const val ROOT_ID = 18284847

private class TestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(LinearLayout(this).apply { id = ROOT_ID })
    }
}

private val Activity.root get() = findViewById(ROOT_ID) as ViewGroup

fun compose(composable: (Composer, Int) -> Unit) =
    RobolectricComposeTester(composable)
fun composeMulti(composable: (Composer, Int) -> Unit, advance: () -> Unit) =
    RobolectricComposeTester(composable, advance)

class RobolectricComposeTester internal constructor(
    val composable: (Composer, Int) -> Unit,
    val advance: (() -> Unit)? = null
) {
    inner class ActiveTest(
        val activity: Activity,
        val advance: () -> Unit
    ) {
        fun then(block: (activity: Activity) -> Unit): ActiveTest {
            try {
                val scheduler = RuntimeEnvironment.getMasterScheduler()
                scheduler.advanceToLastPostedRunnable()
                advance()
                scheduler.advanceToLastPostedRunnable()
                block(activity)
            } catch (e: Throwable) {
                shadowOf(getMainLooper()).idle()
                throw e
            }
            return this
        }
    }

    fun then(block: (activity: Activity) -> Unit): ActiveTest {
        val scheduler = RuntimeEnvironment.getMasterScheduler()
        scheduler.pause()
        val controller = Robolectric.buildActivity(TestActivity::class.java)
        val activity = controller.create().get()
//        val root = activity.root
        scheduler.advanceToLastPostedRunnable()

        val startProviders = Composer::class.java.methods.first {
            it.name.startsWith("startProviders")
        }
        val endProviders = Composer::class.java.methods.first {
            it.name.startsWith("endProviders")
        }
        val setContentMethod = Composition::class.java.methods.first { it.name == "setContent" }
        startProviders.isAccessible = true
        endProviders.isAccessible = true
        setContentMethod.isAccessible = true

//        val realComposable: (Composer, Int) -> Unit = { composer, _ ->
//            startProviders.invoke(
//                composer,
//                listOf(LocalContext provides root.context).toTypedArray()
//            )
//            composable(composer, 0)
//            endProviders.invoke(composer)
//        }

//        val composition = Composition(UiApplier(root), recomposer)
//        fun setContent() {
//            setContentMethod.invoke(composition, realComposable)
//        }
        scheduler.advanceToLastPostedRunnable()
//        setContent()
        scheduler.advanceToLastPostedRunnable()
        block(activity)
        val advanceFn = advance ?: { /* setContent() */ }
        return ActiveTest(activity, advanceFn)
    }

    companion object {
        private val recomposer = run {
            val mainScope = CoroutineScope(
                NonCancellable + Dispatchers.Main
            )

            Recomposer(mainScope.coroutineContext).also {
                // NOTE: Launching undispatched so that compositions created with the
                // singleton instance can assume the recomposer is running
                // when they perform initial composition. The relevant Recomposer code is
                // appropriately thread-safe for this.
                mainScope.launch(start = CoroutineStart.UNDISPATCHED) {
                    it.runRecomposeAndApplyChanges()
                }
            }
        }
    }
}
