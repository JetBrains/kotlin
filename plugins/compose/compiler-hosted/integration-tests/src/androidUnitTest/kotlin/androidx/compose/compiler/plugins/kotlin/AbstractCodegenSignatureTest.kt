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

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.widget.LinearLayout
import java.net.URLClassLoader
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.backend.common.output.OutputFile
import org.junit.Assert.assertEquals
import org.robolectric.Robolectric

abstract class AbstractCodegenSignatureTest(useFir: Boolean) : AbstractCodegenTest(useFir) {
    private fun OutputFile.printApi(): String {
        return printPublicApi(asText(), relativePath)
    }

    protected fun checkApi(
        @Language("kotlin") src: String,
        expected: String,
        dumpClasses: Boolean = false
    ) {
        val className = "Test_REPLACEME_${uniqueNumber++}"
        val fileName = "$className.kt"

        val loader = classLoader(
            """
           import androidx.compose.runtime.*

           $src
        """,
            fileName, dumpClasses
        )

        val apiString = loader
            .allGeneratedFiles
            .filter { it.relativePath.endsWith(".class") }
            .joinToString(separator = "\n") { it.printApi() }
            .replace(className, "Test")

        val expectedApiString = expected
            .trimIndent()
            .split("\n")
            .filter { it.isNotBlank() }
            .joinToString("\n")

        assertEquals(expectedApiString, apiString)
    }

    protected fun checkComposerParam(
        @Language("kotlin") src: String,
        dumpClasses: Boolean = false
    ) {
        val className = "Test_REPLACEME_${uniqueNumber++}"
        val compiledClasses = classLoader(
            """
                import androidx.compose.runtime.*
                import android.widget.LinearLayout
                import android.content.Context
                import androidx.compose.ui.node.UiApplier
                import androidx.compose.runtime.tooling.CompositionData
                import androidx.compose.runtime.tooling.CompositionGroup
                import kotlin.coroutines.CoroutineContext
                import kotlin.coroutines.EmptyCoroutineContext

                $src

                class FakeApplier: Applier<Any> {
                    override val current: Any get() = this
                    override fun down(node: Any) { }
                    override fun up() { }
                    override fun insertTopDown(index: Int, instance: Any) { }
                    override fun insertBottomUp(index: Int, instance: Any) { }
                    override fun remove(index: Int, count: Int) { }
                    override fun move(from: Int, to: Int, count: Int) { }
                    override fun clear() { }
                }

                @OptIn(InternalComposeApi::class)
                class FakeComposition: ControlledComposition {
                    override val isComposing: Boolean get() = false
                    override val isDisposed: Boolean get() = false
                    override val hasInvalidations: Boolean get() = false
                    override val hasPendingChanges: Boolean get() = false
                    override fun composeContent(content: () -> Unit) { }
                    override fun recordModificationsOf(values: Set<Any>) { }
                    override fun recordReadOf(value: Any) { }
                    override fun recordWriteOf(value: Any) { }
                    override fun recompose(): Boolean = false
                    override fun applyChanges() { }
                    override fun invalidateAll() { }
                    override fun verifyConsistent() { }
                    override fun dispose() { }
                    override fun setContent(content: () -> Unit) { }
                }

                @OptIn(InternalComposeApi::class)
                class FakeComposer : Composer {
                    override val applier: Applier<*> = FakeApplier()
                    override val inserting: Boolean get() = true
                    override val skipping: Boolean get() = true
                    override val defaultsInvalid: Boolean get() = false
                    override val recomposeScope: RecomposeScope? get() = null
                    override val compoundKeyHash: Int get() = 0
                    override fun startReplaceableGroup(key: Int) { }
                    override fun startReplaceableGroup(key: Int, sourceInformation: String?) { }
                    override fun endReplaceableGroup() { }
                    override fun startMovableGroup(key: Int, dataKey: Any?) { }
                    override fun startMovableGroup(key: Int, dataKey: Any?, sourceInformation: String?) { }
                    override fun endMovableGroup() { }
                    override fun startDefaults() { }
                    override fun endDefaults() { }
                    override fun startRestartGroup(key: Int): Composer = this
                    override fun startRestartGroup(key: Int, sourceInformation: String?): Composer = this
                    override fun endRestartGroup(): ScopeUpdateScope? = null
                    override fun skipToGroupEnd() { }
                    override fun skipCurrentGroup() { }
                    override fun startNode() { }
                    override fun <T> createNode(factory: () -> T) { }
                    override fun useNode() { }
                    override fun endNode() { }
                    override fun <V, T> apply(value: V, block: T.(V) -> Unit) { }
                    override fun joinKey(left: Any?, right: Any?): Any = Any()
                    override fun rememberedValue(): Any? = Composer.Empty
                    override fun updateRememberedValue(value: Any?) { }
                    override fun changed(value: Any?): Boolean = true
                    override fun recordUsed(scope: RecomposeScope) { }
                    override fun recordSideEffect(effect: () -> Unit) { }
                    @Suppress("UNCHECKED_CAST")
                    override fun <T> consume(key: CompositionLocal<T>): T = null as T
                    override fun startProviders(values: Array<out ProvidedValue<*>>) { }
                    override fun endProviders() { }
                    override fun recordReadOf(value: Any) { }
                    override fun recordWriteOf(value: Any) { }
                    override val compositionData: CompositionData = object : CompositionData {
                        override val compositionGroups: Iterable<CompositionGroup> get() = emptyList()
                        override val isEmpty: Boolean  get() = true
                    }
                    override fun collectParameterInformation() { }
                    override fun buildContext(): CompositionContext = error("Not mockable")
                    override val applyCoroutineContext: CoroutineContext get() = EmptyCoroutineContext
                    override val composition: ControlledComposition = FakeComposition()
               }

               @Composable fun assertComposer(expected: Composer?) {
                    val actual = currentComposer
                    assert(expected === actual)
                }

                fun makeComposer(): Composer = FakeComposer()

                fun invokeComposable(composer: Composer?, fn: @Composable () -> Unit) {
                    if (composer == null) error("Composer was null")
                    val realFn = fn as Function2<Composer, Int, Unit>
                    realFn(composer, 1)
                }

                class Test {
                  fun test(context: Context) {
                    run()
                  }
                }
            """,
            fileName = className,
            dumpClasses = dumpClasses
        )

        val allClassFiles = compiledClasses.allGeneratedFiles.filter {
            it.relativePath.endsWith(".class")
        }

        val loader = URLClassLoader(emptyArray(), this.javaClass.classLoader)

        val instanceClass = run {
            var instanceClass: Class<*>? = null
            var loadedOne = false
            for (outFile in allClassFiles) {
                val bytes = outFile.asByteArray()
                val loadedClass = loadClass(loader, null, bytes)
                if (loadedClass.name == "Test") instanceClass = loadedClass
                loadedOne = true
            }
            if (!loadedOne) error("No classes loaded")
            instanceClass ?: error("Could not find class $className in loaded classes")
        }

        val instanceOfClass = instanceClass.getDeclaredConstructor().newInstance()
        val testMethod = instanceClass.getMethod("test", Context::class.java)

        val controller = Robolectric.buildActivity(TestActivity::class.java)
        val activity = controller.create().get()
        testMethod.invoke(instanceOfClass, activity)
    }

    private class TestActivity : Activity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(LinearLayout(this))
        }
    }

    protected fun codegen(
        @Language("kotlin") text: String,
        dumpClasses: Boolean = false
    ) {
        codegenNoImports(
            """
           import android.content.Context
           import android.widget.*
           import androidx.compose.runtime.*

           $text

            fun used(x: Any?) {}
        """,
            dumpClasses
        )
    }

    private fun codegenNoImports(
        @Language("kotlin") text: String,
        dumpClasses: Boolean = false
    ) {
        val className = "Test_${uniqueNumber++}"
        val fileName = "$className.kt"

        classLoader(text, fileName, dumpClasses)
    }
}
