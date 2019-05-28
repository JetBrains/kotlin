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

package androidx.compose.plugins.kotlin

import android.app.Activity
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.Component
import androidx.compose.Compose
import androidx.compose.composer
import androidx.compose.runWithCurrent
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.backend.common.output.OutputFile
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.net.URLClassLoader

@RunWith(ComposeRobolectricTestRunner::class)
@Config(
    manifest = Config.NONE,
    minSdk = 23,
    maxSdk = 23
)
class KtxCrossModuleTests : AbstractCodegenTest() {

    @Test
    fun testCrossModule_SimpleComposition(): Unit = ensureSetup {
        val tvId = 29

        compose(
            "TestF", mapOf(
                "library module" to mapOf(
                    "my/test/lib/InternalComp.kt" to """
                    package my.test.lib

                    import androidx.compose.*

                    class InternalComp(@Children var block: () -> Unit) : Component() {

                      override fun compose() {
                        <block />
                      }
                    }
                 """
                ),
                "Main" to mapOf(
                    "my/test/app/Main.kt" to """
                   package my.test.app

                   import android.widget.*
                   import androidx.compose.*
                   import my.test.lib.*

                   var bar = 0
                   var doRecompose: () -> Unit = {}

                   class TestF {
                       @Composable
                       fun compose() {
                         <Recompose> recompose ->
                           doRecompose = recompose
                           <Foo bar />
                         </Recompose>
                       }

                       fun advance() {
                         bar++
                         doRecompose()
                       }
                   }

                   @Composable
                   fun Foo(bar: Int) {
                     <InternalComp>
                       <TextView text="${'$'}bar" id=$tvId />
                     </InternalComp>
                   }
                """
                )
            )
        ).then { activity ->
            val tv = activity.findViewById(tvId) as TextView
            assertEquals("0", tv.text)
        }.then { activity ->
            val tv = activity.findViewById(tvId) as TextView
            assertEquals("1", tv.text)
        }
    }

    @Test
    fun testCrossModule_ComponentFunction(): Unit = ensureSetup {
        val tvName = 101
        val tvAge = 102

        compose(
            "TestF", mapOf(
                "library KTX module" to mapOf(
                    "my/test2/lib/ktx/ComponentFunction.kt" to """
                       package my.test2.ktx

                       import android.widget.*
                       import androidx.compose.*

                       @Composable
                       fun ComponentFunction(name: String, age: Int) {
                         <LinearLayout>
                           <TextView text=name id=$tvName />
                           <TextView text="${'$'}age" id=$tvAge />
                         </LinearLayout>
                       }
                 """
                ),
                "Main" to mapOf(
                    "my/test2/app/Test.kt" to """
                       package my.test2.app

                       import android.widget.*
                       import androidx.compose.*
                       import my.test2.ktx.*

                       var age = $PRESIDENT_AGE_1
                       var name = "$PRESIDENT_NAME_1"
                       var doRecompose: () -> Unit = {}

                       class TestF {
                           @Composable
                           fun compose() {
                             <Recompose> recompose ->
                               doRecompose = recompose
                               <Foo name age />
                             </Recompose>
                           }

                           fun advance() {
                             age = $PRESIDENT_AGE_16
                             name = "$PRESIDENT_NAME_16"
                             doRecompose()
                           }
                       }

                       @Composable
                       fun Foo(name: String, age: Int) {
                         <ComponentFunction name age />
                       }
                    """
                )
            )
        ).then { activity ->
            val name = activity.findViewById(tvName) as TextView
            assertEquals(PRESIDENT_NAME_1, name.text)
            val age = activity.findViewById(tvAge) as TextView
            assertEquals("$PRESIDENT_AGE_1", age.text)
        }.then { activity ->
            val name = activity.findViewById(tvName) as TextView
            assertEquals(PRESIDENT_NAME_16, name.text)
            val age = activity.findViewById(tvAge) as TextView
            assertEquals("$PRESIDENT_AGE_16", age.text)
        }
    }

    @Test
    fun testCrossModule_ObjectFunction(): Unit = ensureSetup {
        val tvName = 101
        val tvAge = 102

        compose(
            "TestF", mapOf(
                "library KTX module" to mapOf(
                    "my/test2/lib/ktx/ObjectFunction.kt" to """
                       package my.test2.ktx

                       import android.widget.*
                       import androidx.compose.*

                       object Container {
                           @Composable
                           fun ComponentFunction(name: String, age: Int) {
                             <LinearLayout>
                               <TextView text=name id=$tvName />
                               <TextView text="${'$'}age" id=$tvAge />
                             </LinearLayout>
                           }
                       }
                 """
                ),
                "Main" to mapOf(
                    "my/test2/app/Test.kt" to """
                       package my.test2.app

                       import android.widget.*
                       import androidx.compose.*
                       import my.test2.ktx.*

                       var age = $PRESIDENT_AGE_1
                       var name = "$PRESIDENT_NAME_1"
                       var doRecompose: () -> Unit = {}

                       class TestF {
                           @Composable
                           fun compose() {
                             <Recompose> recompose ->
                               doRecompose = recompose
                               <Foo name age />
                             </Recompose>
                           }

                           fun advance() {
                             age = $PRESIDENT_AGE_16
                             name = "$PRESIDENT_NAME_16"
                             doRecompose()
                           }
                       }

                       @Composable
                       fun Foo(name: String, age: Int) {
                         <Container.ComponentFunction name age />
                       }
                    """
                )
            )
        ).then { activity ->
            val name = activity.findViewById(tvName) as TextView
            assertEquals(PRESIDENT_NAME_1, name.text)
            val age = activity.findViewById(tvAge) as TextView
            assertEquals("$PRESIDENT_AGE_1", age.text)
        }.then { activity ->
            val name = activity.findViewById(tvName) as TextView
            assertEquals(PRESIDENT_NAME_16, name.text)
            val age = activity.findViewById(tvAge) as TextView
            assertEquals("$PRESIDENT_AGE_16", age.text)
        }
    }

    @Test
    fun testCrossModule_ConstructorProperties(): Unit = ensureSetup {
        val tvId = 29

        compose(
            "TestF", mapOf(
                "library module" to mapOf(
                    "my/test/lib/MyComponent.kt" to """
                    package my.test.lib

                    import androidx.compose.*

                    class MyComponent(
                        var a: Int,
                        var b: String,
                        @Children var children: (a: Int, b: String)->Unit
                    ) : Component() {

                      override fun compose() {
                        <children a b />
                      }
                    }
                 """
                ),
                "Main" to mapOf(
                    "my/test/app/Main.kt" to """
                   package my.test.app

                   import android.widget.*
                   import androidx.compose.*
                   import my.test.lib.*

                   var bar = 0
                   var doRecompose: () -> Unit = {}

                   class TestF {
                       @Composable
                       fun compose() {
                         <Recompose> recompose ->
                           doRecompose = recompose
                           <Foo bar />
                         </Recompose>
                       }

                       fun advance() {
                         bar++
                         doRecompose()
                       }
                   }

                   @Composable
                   fun Foo(bar: Int) {
                     <MyComponent b="SomeValue" a=bar> c, d ->
                       <TextView text="${'$'}d: ${'$'}c" id=$tvId />
                     </MyComponent>
                   }
                """
                )
            )
        ).then { activity ->
            val tv = activity.findViewById(tvId) as TextView
            assertEquals("SomeValue: 0", tv.text)
        }.then { activity ->
            val tv = activity.findViewById(tvId) as TextView
            assertEquals("SomeValue: 1", tv.text)
        }
    }

    @Test
    fun testCrossModule_ConstructorParameters(): Unit = ensureSetup {
        val tvId = 29

        compose(
            "TestF", mapOf(
                "library module" to mapOf(
                    "my/test/lib/MyComponent.kt" to """
                    package my.test.lib

                    import androidx.compose.*

                    class MyComponent(
                      a: Int,
                      b: String,
                      @Children var children: (a: Int, b: String)->Unit
                    ) : Component() {
                      val aValue = a
                      val bValue = b

                      override fun compose() {
                        <children a=aValue b=bValue />
                      }
                    }
                 """
                ),
                "Main" to mapOf(
                    "my/test/app/Main.kt" to """
                   package my.test.app

                   import android.widget.*
                   import androidx.compose.*
                   import my.test.lib.*

                   var bar = 0
                   var doRecompose: () -> Unit = {}

                    class MyComponent(
                      a: Int,
                      b: String,
                      @Children var children: (a: Int, b: String)->Unit
                    ) : Component() {
                      val aValue = a
                      val bValue = b

                      override fun compose() {
                        <children a=aValue b=bValue />
                      }
                    }

                   class TestF {
                       @Composable
                       fun compose() {
                         <Recompose> recompose ->
                           doRecompose = recompose
                           <Foo bar />
                         </Recompose>
                       }

                       fun advance() {
                         bar++
                         doRecompose()
                       }
                   }

                   @Composable
                   fun Foo(bar: Int) {
                     <MyComponent b="SomeValue" a=bar> c, d ->
                       <TextView text="${'$'}d: ${'$'}c" id=$tvId />
                     </MyComponent>
                   }
                """
                )
            )
        ).then { activity ->
            val tv = activity.findViewById(tvId) as TextView
            assertEquals("SomeValue: 0", tv.text)
        }.then { activity ->
            val tv = activity.findViewById(tvId) as TextView
            assertEquals("SomeValue: 1", tv.text)
        }
    }

    fun compose(
        mainClassName: String,
        modules: Map<String, Map<String, String>>,
        dumpClasses: Boolean = false
    ): MultiCompositionTest {
        val libraryClasses = (modules.filter { it.key != "Main" }.map {
            // Setup for compile
            this.classFileFactory = null
            this.myEnvironment = null
            setUp(it.key.contains("--ktx=false"))

            classLoader(it.value, dumpClasses).allGeneratedFiles.also {
                // Write the files to the class directory so they can be used by the next module
                // and the application
                it.writeToDir(classesDirectory)
            }
        } + emptyList()).reduce { acc, mutableList -> acc + mutableList }

        // Setup for compile
        this.classFileFactory = null
        this.myEnvironment = null
        setUp()

        // compile the next one
        val appClasses = classLoader(modules["Main"]
            ?: error("No Main module specified"), dumpClasses).allGeneratedFiles

        // Load the files looking for mainClassName
        val allClasses = (libraryClasses + appClasses).filter {
            it.relativePath.endsWith(".class")
        }
        val loader = URLClassLoader(emptyArray(), this.javaClass.classLoader)
        val instanceClass = run {
            var instanceClass: Class<*>? = null
            var loadedOne = false
            for (outFile in allClasses) {
                val bytes = outFile.asByteArray()
                val loadedClass = loadClass(loader, null, bytes)
                if (loadedClass.name.endsWith(mainClassName)) instanceClass = loadedClass
                loadedOne = true
            }
            if (!loadedOne) error("No classes loaded")
            instanceClass ?: error("Could not find class $mainClassName in loaded classes")
        }

        val instanceOfClass = instanceClass.newInstance()
        val advanceMethod = instanceClass.getMethod("advance")
        val composeMethod = instanceClass.getMethod("compose")

        return composeMulti({ composeMethod.invoke(instanceOfClass) }) {
            advanceMethod.invoke(instanceOfClass)
        }
    }

    fun setUp(disable: Boolean = false) {
        if (disable) {
            this.disableIrAndKtx = true
            try {
                setUp()
            } finally {
                this.disableIrAndKtx = false
            }
        } else {
            setUp()
        }
    }

    override fun setUp() {
        isSetup = true
        if (disableIrAndKtx) {
            super.setUp()
        } else {
            super.setUp()
        }
    }

    override fun setupEnvironment(environment: KotlinCoreEnvironment) {
        if (!disableIrAndKtx) {
            super.setupEnvironment(environment)
        }
    }

    private var disableIrAndKtx = false

    override fun updateConfiguration(configuration: CompilerConfiguration) {
        super.updateConfiguration(configuration)
        if (disableIrAndKtx) {
            configuration.put(JVMConfigurationKeys.IR, false)
        }
    }

    private var testLocalUnique = 0
    private var classesDirectory = tmpDir(
        "kotlin-${testLocalUnique++}-classes"
    )

    override val additionalPaths: List<File> = listOf(classesDirectory)

    private var isSetup = false
    private inline fun <T> ensureSetup(block: () -> T): T {
        if (!isSetup) setUp()
        return block()
    }
}

fun OutputFile.writeToDir(directory: File) =
    FileUtil.writeToFile(File(directory, relativePath), asByteArray())

fun Collection<OutputFile>.writeToDir(directory: File) = forEach { it.writeToDir(directory) }

private fun composeMulti(composable: () -> Unit, advance: () -> Unit) =
    MultiCompositionTest(composable, advance)

private class MultiTestActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(LinearLayout(this).apply { id = ROOT_ID })
    }
}

private val Activity.root get() = findViewById(ROOT_ID) as ViewGroup

private class MultiRoot : Component() {
    override fun compose() {}
}

class MultiCompositionTest(val composable: () -> Unit, val advance: () -> Unit) {

    inner class ActiveTest(val activity: Activity) {

        fun then(block: (activity: Activity) -> Unit): ActiveTest {
            advance()
            val scheduler = RuntimeEnvironment.getMasterScheduler()
            scheduler.advanceToLastPostedRunnable()
            block(activity)
            return this
        }
    }

    fun then(block: (activity: Activity) -> Unit): ActiveTest {
        val controller = Robolectric.buildActivity(MultiTestActivity::class.java)

        // Compose the root scope
        val activity = controller.create().get()
        val root = activity.root
        val component = MultiRoot()
        val cc = Compose.createCompositionContext(root.context, root, component, null)
        cc.composer.runWithCurrent {
            val composer = cc.composer
            composer.startRoot()
            composable()
            composer.endRoot()
            composer.applyChanges()
        }
        block(activity)
        return ActiveTest(activity)
    }
}

private fun tmpDir(name: String): File {
    return FileUtil.createTempDirectory(name, "", false).canonicalFile
}
