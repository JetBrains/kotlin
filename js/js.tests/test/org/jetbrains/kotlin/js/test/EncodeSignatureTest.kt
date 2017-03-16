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

package org.jetbrains.kotlin.js.test

import com.intellij.mock.MockVirtualFileSystem
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.js.analyze.TopDownAnalyzerFacadeForJS
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.config.JsConfig
import org.jetbrains.kotlin.js.naming.encodeSignature
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.junit.Assert
import org.junit.Test

class EncodeSignatureTest {
    @Test
    fun function() {
        assertSignature("", "fun test() {}")
        assertSignature("kotlin.Int", "fun test(x: Int) {}")
        assertSignature("kotlin.Int,kotlin.Long", "fun test(x: Int, y: Long) {}")
    }

    @Test
    fun extensionFunction() {
        assertSignature("kotlin.Int/", "fun Int.test() {}")
        assertSignature("kotlin.Int/kotlin.Long", "fun Int.test(x: Long) {}")
    }

    @Test
    fun property() {
        assertSignature("", "val test: Int = 23")
        assertSignature("", "class A(val test: Int)")
    }

    @Test
    fun extensionProperty() {
        assertSignature("kotlin.String/", "val String.test: Int = 23")
        assertSignature("kotlin.String/", "val String.test: Int = 23", "<get-test>")
        assertSignature("kotlin.String/kotlin.Int", "var String.test: Int = 23", "<set-test>")

        assertSignature("kotlin.String/", "class A { val String.test: Int get() = 23 }")
        assertSignature("kotlin.String/", "class A { val String.test: Int get() = 23 }", "<get-test>")
        assertSignature("kotlin.String/kotlin.Int", "class A { var String.test: Int get() = 23; set(_) {} }", "<set-test>")
    }

    @Test
    fun genericType() {
        assertSignature("kotlin.Array<kotlin.String>", "fun test(x: Array<String>) {}")
    }

    @Test
    fun varargParam() {
        assertSignature("*kotlin.Array<+kotlin.String>", "fun test(vararg x: String) {}")
    }

    @Test
    fun nullableType() {
        assertSignature("kotlin.String?", "fun test(x: String?) {}")
    }

    @Test
    fun ownTypeParameters() {
        assertSignature("0:0,0:1|0:0,0:1", "fun <S, T> test(x: S, y: T) {}")
        assertSignature("0:1,0:0|0:0,0:1", "fun <S, T> test(x: T, y: S) {}")
    }

    @Test
    fun enclosingTypeParameters() {
        assertSignature("2:0,1:0,0:0|0:0", """
            class A<S> {
                inner class B<T> {
                    fun <U> test(x: S, y: T, z: U) {}
                }
            }
        """)
    }

    @Test
    fun variance() {
        assertSignature("A<kotlin.Int>,A<-kotlin.Long>,A<+kotlin.String>", """
            class A<T>
            fun test(x: A<Int>, y: A<in Long>, z: A<out String>)
        """)

        assertSignature("A<-kotlin.Int>,A<-kotlin.Long>,A<+kotlin.String>", """
            class A<in T>
            fun test(x: A<Int>, y: A<in Long>, z: A<out String>)
        """)

        assertSignature("A<+kotlin.Int>,A<+kotlin.Long>,A<+kotlin.String>", """
            class A<out T>
            fun test(x: A<Int>, y: A<in Long>, z: A<out String>)
        """)
    }

    @Test
    fun starProjection() {
        assertSignature("A<*>", """
            class A<T>
            fun test(x: A<*>)
        """)
    }

    @Test
    fun typeConstraints() {
        assertSignature("0:0|0:0<:A<kotlin.Int>", """
            class A<T>
            fun <T : A<Int>> test(x: T)
        """)

        assertSignature("0:0|0:0<:A<kotlin.Int>&A<kotlin.String>", """
            class A<T>
            fun <T> test(x: T) where T : A<Int>, T : A<String>
        """)

        assertSignature("1:0,0:0|0:0<:1:0", """
            class A<T> {
                fun <S> test(x: T, x: S) where S : T
            }
        """)
    }

    @Test
    fun genericExtensionFunctionToInner() {
        assertSignature("A.C<kotlin.String,2:0>/1:0", """
            class A<T> {
                inner class B<S> {
                    fun C<String>.test(x: S) {}
                }
                inner class C<U>
            }
            fun <T : A<Int>> test(x: T)
        """)
    }

    @Test
    fun typeAliases() {
        assertSignature("C<kotlin.String>", """
            class C<T>
            typealias A = C<String>
            fun test(x: A)
        """)
    }

    @Test
    fun typeParameterUsageInConstraints() {
        assertSignature("0:0|0:0<:1:0,1:0<:I", """
            interface I
            class A<T : I> {
                fun <S : T> test(x: S)
            }
        """)
    }

    @Test
    fun recursiveType() {
        assertSignature("1:0|1:0<:I<1:0>", """
            interface I<T : I<T>> {
                fun test(x: T)
            }
        """)
    }

    @Test
    fun missingType() {
        assertSignature(",kotlin.Int", "fun test(a: WrongClass, b: Int) {}")
        assertSignature(",kotlin.Int", "fun test(a: WrongClass<String>, b: Int) {}")
    }

    @Suppress("ObjectLiteralToLambda")
    private fun assertSignature(expectedEncoding: String, codeSnippet: String, name: String = "test") {
        val disposable = object : Disposable {
            override fun dispose() {
            }
        }

        val environment = createEnvironment(disposable)
        try {
            val project = environment.project

            val fs = MockVirtualFileSystem()
            val file = fs.file("sample.kt", codeSnippet).findFileByPath("/sample.kt")
            val psiManager = PsiManager.getInstance(project)
            val psiFile = psiManager.findFile(file) as KtFile

            val configuration = environment.configuration.copy()
            configuration.put(JSConfigurationKeys.LIBRARIES, JsConfig.JS_STDLIB)
            configuration.put(CommonConfigurationKeys.MODULE_NAME, "sample")

            val analysisResult = TopDownAnalyzerFacadeForJS.analyzeFiles(listOf(psiFile), JsConfig(project, configuration))
            val module = analysisResult.moduleDescriptor
            val rootPackage = module.getPackage(FqName.ROOT)

            val testDescriptor = findTestCallable(rootPackage, name) ?:
                                 error("Descriptor named `$name` was not found in provided snippet: $codeSnippet")
            val actualEncoding = encodeSignature(testDescriptor)
            Assert.assertEquals(expectedEncoding, actualEncoding)
        }
        finally {
            Disposer.dispose(disposable)
        }
    }

    private fun findTestCallable(scope: MemberScope, name: String): CallableMemberDescriptor? {
        return scope.getContributedDescriptors().asSequence().mapNotNull { findTestCallable(it, name) }.firstOrNull()
    }

    private fun findTestCallable(descriptor: DeclarationDescriptor, name: String): CallableMemberDescriptor? {
        return when (descriptor) {
            is PackageViewDescriptor -> findTestCallable(descriptor.memberScope, name)
            is ClassDescriptor -> findTestCallable(descriptor.unsubstitutedMemberScope, name)
            is PropertyDescriptor -> {
                if (descriptor.name.asString() == name) {
                    descriptor
                }
                else {
                    descriptor.accessors.asSequence().mapNotNull { findTestCallable(it, name) }.firstOrNull()
                }
            }
            is CallableMemberDescriptor -> if (descriptor.name.asString() == name) descriptor else null
            else -> null
        }
    }

    fun createEnvironment(disposable: Disposable): KotlinCoreEnvironment {
        return KotlinCoreEnvironment.createForTests(disposable, CompilerConfiguration(), EnvironmentConfigFiles.JS_CONFIG_FILES)
    }
}
