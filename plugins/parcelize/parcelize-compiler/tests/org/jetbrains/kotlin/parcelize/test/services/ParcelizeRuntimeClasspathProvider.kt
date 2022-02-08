/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize.test.services

import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.RuntimeClasspathProvider
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.jvm.compiledClassesManager
import org.jetbrains.kotlin.test.services.temporaryDirectoryManager
import org.jetbrains.kotlin.utils.PathUtil
import org.jetbrains.org.objectweb.asm.ClassWriter
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.junit.runner.JUnitCore
import java.io.File

class ParcelizeRuntimeClasspathProvider(testServices: TestServices) : RuntimeClasspathProvider(testServices) {
    companion object {
        val layoutlibJar: File by lazy { getLayoutLibFile("layoutLib.path") }
        val layoutlibApiJar: File by lazy { getLayoutLibFile("layoutLibApi.path") }

        private const val ANDROID_TOOLS_PREFIX = "studio.android.sdktools."

        private fun getLayoutLibFile(property: String): File {
            val layoutLibFile = File(System.getProperty(property))
            if (!layoutLibFile.isFile) {
                error("Can't find jar file in $property system property")
            }
            return layoutLibFile
        }

        private val JUNIT_GENERATED_TEST_CLASS_BYTES by lazy { constructSyntheticTestClass() }
        private const val JUNIT_GENERATED_TEST_CLASS_PACKAGE = "test"
        private const val JUNIT_GENERATED_TEST_CLASS_NAME = "JunitTest.class"
        const val JUNIT_GENERATED_TEST_CLASS_FQNAME = "test.JunitTest"

        private fun constructSyntheticTestClass(): ByteArray {
            return with(ClassWriter(ClassWriter.COMPUTE_MAXS or ClassWriter.COMPUTE_FRAMES)) {
                visit(49, Opcodes.ACC_PUBLIC, JUNIT_GENERATED_TEST_CLASS_FQNAME.replace('.', '/'), null, "java/lang/Object", emptyArray())
                visitSource(null, null)

                with(visitAnnotation("Lorg/junit/runner/RunWith;", true)) {
                    visit("value", Type.getType("Lorg/robolectric/RobolectricTestRunner;"))
                    visitEnd()
                }

                with(visitAnnotation("Lorg/robolectric/annotation/Config;", true)) {
                    visit("sdk", intArrayOf(21))
                    visit("manifest", "--none")
                    visitEnd()
                }

                with(visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null)) {
                    visitVarInsn(Opcodes.ALOAD, 0)
                    visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)

                    visitInsn(Opcodes.RETURN)
                    visitMaxs(-1, -1)
                    visitEnd()
                }

                with(visitMethod(Opcodes.ACC_PUBLIC, "test", "()V", null, null)) {
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

    override fun runtimeClassPaths(module: TestModule): List<File> {
        val kotlinRuntimeJar = PathUtil.kotlinPathsForIdeaPlugin.stdlibPath

        val robolectricClasspath = System.getProperty("robolectric.classpath")
            ?: throw RuntimeException("Unable to get a valid classpath from 'robolectric.classpath' property, please set it accordingly")
        val robolectricJars = robolectricClasspath.split(File.pathSeparator)
            .map { File(it) }
            .sortedBy { it.nameWithoutExtension }

        val junitCoreResourceName = JUnitCore::class.java.name.replace('.', '/') + ".class"
        val junitJar = File(
            JUnitCore::class.java.classLoader.getResource(junitCoreResourceName)!!.file
                .substringAfter("file:")
                .substringBeforeLast('!')
        )

        val parcelizeRuntimeJars = System.getProperty("parcelizeRuntime.classpath")?.split(File.pathSeparator)?.map(::File)
            ?: error("Unable to get a valid classpath from 'parcelizeRuntime.classpath' property")

        val tempDir = testServices.temporaryDirectoryManager.getOrCreateTempDirectory("additionalClassFiles")
        tempDir
            .resolve(JUNIT_GENERATED_TEST_CLASS_PACKAGE).also { it.mkdir() }
            .resolve(JUNIT_GENERATED_TEST_CLASS_NAME)
            .apply { writeBytes(JUNIT_GENERATED_TEST_CLASS_BYTES) }

        return buildList {
            add(kotlinRuntimeJar)
            add(layoutlibJar)
            add(layoutlibApiJar)
            addAll(robolectricJars)
            add(junitJar)
            addAll(parcelizeRuntimeJars)
            add(tempDir)
        }
    }
}
