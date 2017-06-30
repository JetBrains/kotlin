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

package org.jetbrains.kotlin.android.parcel

import org.jetbrains.kotlin.android.synthetic.AndroidComponentRegistrar
import org.jetbrains.kotlin.android.synthetic.test.addAndroidExtensionsRuntimeLibrary
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.codegen.CodegenTestCase
import org.jetbrains.kotlin.codegen.getClassFiles
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.tree.ClassNode
import java.io.File
import java.net.URLClassLoader
import java.nio.ByteBuffer
import java.security.ProtectionDomain


abstract class AbstractParcelBoxTest : CodegenTestCase() {
    protected companion object {
        val BASE_DIR = "plugins/android-extensions/android-extensions-compiler/testData/parcel/box"
        val LIBRARY_KT = File(File(BASE_DIR).parentFile, "boxLib.kt")
    }

    override fun doTest(filePath: String) {
        super.doTest(File(BASE_DIR, filePath + ".kt").absolutePath)
    }

    open protected fun getClassLoaderWithGeneratedFiles(): ClassLoader {
        return object : URLClassLoader(arrayOf(), this::class.java.classLoader) {
            init {
                for (classFile in classFileFactory.getClassFiles().sortedBy { it.relativePath }) {
                    val bytes = classFile.asByteArray()
                    val className = ClassNode().also { ClassReader(bytes).accept(it, ClassReader.EXPAND_FRAMES) }.name
                    try {
                        defineClass(className.replace('/', '.'), ByteBuffer.wrap(bytes), null as ProtectionDomain?)
                    } catch (e: Throwable) {
                        throw RuntimeException("Can't load class $className", e)
                    }
                }
            }
        }
    }

    override fun doMultiFileTest(wholeFile: File, files: List<TestFile>, javaFilesDir: File?) {
        compile(files + TestFile(LIBRARY_KT.name, LIBRARY_KT.readText()), javaFilesDir)

        val testClass = Class.forName("test.TestKt", false, getClassLoaderWithGeneratedFiles())
        try {
            testClass.getDeclaredMethod("box").invoke(testClass)
        } catch (e: Throwable) {
            throw AssertionError(classFileFactory.createText(), e)
        }
    }

    override fun setupEnvironment(environment: KotlinCoreEnvironment) {
        AndroidComponentRegistrar.registerParcelExtensions(environment.project)
        addAndroidExtensionsRuntimeLibrary(environment)
        environment.updateClasspath(listOf(JvmClasspathRoot(File("ideaSDK/plugins/android/lib/layoutlib.jar"))))
    }
}