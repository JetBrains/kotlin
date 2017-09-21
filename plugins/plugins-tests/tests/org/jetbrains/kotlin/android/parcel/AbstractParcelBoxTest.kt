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
import org.jetbrains.kotlin.utils.PathUtil
import org.jetbrains.org.objectweb.asm.ClassWriter
import org.jetbrains.org.objectweb.asm.ClassWriter.COMPUTE_FRAMES
import org.jetbrains.org.objectweb.asm.ClassWriter.COMPUTE_MAXS
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Opcodes.*
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.junit.runner.JUnitCore
import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit

abstract class AbstractParcelBoxTest : CodegenTestCase() {
    protected companion object {
        val BASE_DIR = "plugins/android-extensions/android-extensions-compiler/testData/parcel/box"
        val LIBRARY_KT = File(File(BASE_DIR).parentFile, "boxLib.kt")

        private val JUNIT_GENERATED_TEST_CLASS_BYTES by lazy { constructSyntheticTestClass() }
        private val JUNIT_GENERATED_TEST_CLASS_FQNAME = "test.JunitTest"

        private fun constructSyntheticTestClass(): ByteArray {
            return with(ClassWriter(COMPUTE_MAXS or COMPUTE_FRAMES)) {
                visit(49, ACC_PUBLIC, JUNIT_GENERATED_TEST_CLASS_FQNAME.replace('.', '/'), null, "java/lang/Object", emptyArray())
                visitSource(null, null)

                with(visitAnnotation("Lorg/junit/runner/RunWith;", true)) {
                    visit("value", Type.getType("Lorg/robolectric/RobolectricTestRunner;"))
                    visitEnd()
                }

                with(visitAnnotation("Lorg/robolectric/annotation/Config;", true)) {
                    visit("manifest", "--none")
                    visitEnd()
                }

                with(visitMethod(ACC_PUBLIC, "<init>", "()V", null, null)) {
                    visitVarInsn(ALOAD, 0)
                    visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)

                    visitInsn(RETURN)
                    visitMaxs(-1, -1)
                    visitEnd()
                }

                with(visitMethod(ACC_PUBLIC, "test", "()V", null, null)) {
                    visitAnnotation("Lorg/junit/Test;", true).visitEnd()

                    val v = InstructionAdapter(this)

                    val assertionOk = Label()

                    v.invokestatic("test/TestKt", "box", "()Ljava/lang/String;", false) // -> ret
                    v.dup() // -> ret, ret
                    v.aconst("OK") // -> ret, ret, ok
                    v.invokevirtual("java/lang/String", "equals", "(Ljava/lang/Object;)Z", false) // -> ret, eq
                    v.ifne(assertionOk) // -> ret

                    val assertionErrorType = Type.getObjectType("java/lang/AssertionError")

                    v.anew(assertionErrorType) // -> ret, ae
                    v.dupX1() // -> ae, ret, ae
                    v.swap() // -> ae, ae, ret
                    v.invokespecial(assertionErrorType.internalName, "<init>", "(Ljava/lang/Object;)V", false) // -> ae
                    v.athrow()

                    v.visitLabel(assertionOk)
                    v.pop() // -> [empty]
                    v.areturn(Type.VOID_TYPE)

                    visitMaxs(-1, -1)
                    visitEnd()
                }

                visitEnd()
                toByteArray()
            }
        }
    }

    override fun doTest(filePath: String) {
        super.doTest(File(BASE_DIR, filePath + ".kt").absolutePath)
    }

    private fun getClasspathForTest(): List<File> {
        val kotlinRuntimeJar = PathUtil.kotlinPathsForIdeaPlugin.stdlibPath
        val layoutLibJars = listOf(File("ideaSDK/plugins/android/lib/layoutlib.jar"), File("ideaSDK/plugins/android/lib/layoutlib-api.jar"))

        val robolectricJars = File("dependencies/robolectric")
                .listFiles { f: File -> f.extension == "jar" }
                .sortedBy { it.nameWithoutExtension }

        val junitCoreResourceName = JUnitCore::class.java.name.replace('.', '/') + ".class"
        val junitJar = File(JUnitCore::class.java.classLoader.getResource(junitCoreResourceName).file.substringBeforeLast('!'))

        val androidExtensionsRuntime = File("out/production/android-extensions-runtime")

        return listOf(kotlinRuntimeJar) + layoutLibJars + robolectricJars + junitJar + androidExtensionsRuntime
    }

    override fun doMultiFileTest(wholeFile: File, files: List<TestFile>, javaFilesDir: File?) {
        compile(files + TestFile(LIBRARY_KT.name, LIBRARY_KT.readText()), javaFilesDir)

        val javaBin = File(System.getProperty("java.home").takeIf { it.isNotEmpty() } ?: error("JAVA_HOME is not set"), "bin")
        val javaExe = File(javaBin, "java.exe").takeIf { it.exists() } ?: File(javaBin, "java")
        assert(javaExe.exists()) { "Can't find 'java' executable in $javaBin" }

        val libraryClasspath = getClasspathForTest()
        val dirForTestClasses = Files.createTempDirectory("parcel").toFile()

        fun writeClass(fqNameOrPath: String, bytes: ByteArray) {
            val path = if (fqNameOrPath.endsWith(".class")) fqNameOrPath else (fqNameOrPath.replace('.', '/') + ".class")
            File(dirForTestClasses, path).also { it.parentFile.mkdirs() }.writeBytes(bytes)
        }

        try {
            writeClass(JUNIT_GENERATED_TEST_CLASS_FQNAME, JUNIT_GENERATED_TEST_CLASS_BYTES)
            classFileFactory.getClassFiles().forEach { writeClass(it.relativePath, it.asByteArray()) }

            val process = ProcessBuilder(
                    javaExe.absolutePath,
                    "-ea",
                    "-classpath",
                    (libraryClasspath + dirForTestClasses).joinToString(File.pathSeparator),
                    JUnitCore::class.java.name,
                    JUNIT_GENERATED_TEST_CLASS_FQNAME
            ).inheritIO().start()

            process.waitFor(3, TimeUnit.MINUTES)
            if (process.exitValue() != 0) {
                throw AssertionError(classFileFactory.createText())
            }
        } finally {
            if (!dirForTestClasses.deleteRecursively()) {
                throw AssertionError("Unable to delete $dirForTestClasses")
            }
        }
    }

    override fun setupEnvironment(environment: KotlinCoreEnvironment) {
        AndroidComponentRegistrar.registerParcelExtensions(environment.project)
        addAndroidExtensionsRuntimeLibrary(environment)
        environment.updateClasspath(listOf(JvmClasspathRoot(File("ideaSDK/plugins/android/lib/layoutlib.jar"))))
    }
}