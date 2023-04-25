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

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.junit.Assert.assertEquals
import org.junit.Test

class CodegenMetadataTests : AbstractLoweringTests() {
    override fun CompilerConfiguration.updateConfiguration() {
        put(ComposeConfiguration.LIVE_LITERALS_ENABLED_KEY, true)
    }

    @Test
    fun testBasicFunctionality() {
        val className = "Test_${uniqueNumber++}"
        val fileName = "$className.kt"
        val loader = classLoader(
            """
            import kotlin.reflect.full.primaryConstructor
            import kotlin.reflect.jvm.isAccessible
            data class MyClass(val someBoolean: Boolean? = false)
            object Main { @JvmStatic fun main() { MyClass::class.java.kotlin.primaryConstructor!!.isAccessible = true } }
            """,
            fileName,
            false
        )
        val main = loader.loadClass("Main").methods.single { it.name == "main" }
        main.invoke(null)
    }

    @Test
    fun testDelegatedProperties() {
        val className = "Test_${uniqueNumber++}"
        val fileName = "$className.kt"
        val loader = classLoader(
            """
            import androidx.compose.runtime.Composable
            import kotlin.reflect.KProperty
            import kotlinx.metadata.jvm.KotlinClassMetadata
            import kotlinx.metadata.jvm.localDelegatedProperties

            inline operator fun String.getValue(thisRef: Any?, property: KProperty<*>) = 0

            @Composable
            inline operator fun Int.getValue(thisRef: Any?, property: KProperty<*>) = 0

            object Main {
                @JvmStatic
                fun main(): List<String> {
                    val metadataAnnotation = Main::class.java
                        .annotations
                        .filter { it.annotationClass.qualifiedName == "kotlin.Metadata" }
                        .single() as Metadata

                    val cls = (
                        KotlinClassMetadata.read(metadataAnnotation) as KotlinClassMetadata.Class
                    ).toKmClass()
                    return cls.localDelegatedProperties.map { it.name }
                }

                fun test() {
                    val foo by ""
                    println(foo)
                }

                @Composable fun ComposableTest(value: Int) {
                    val fooComposable by value
                    ComposableTest(fooComposable)
                }
            }
            """,
            fileName,
            false
        )
        val main = loader.loadClass("Main").methods.single { it.name == "main" }
        val delegates = main.invoke(null)

        assertEquals(delegates, listOf("foo", "fooComposable"))
    }
}
