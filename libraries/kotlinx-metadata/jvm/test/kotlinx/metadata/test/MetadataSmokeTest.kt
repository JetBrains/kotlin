/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.test

import kotlinx.metadata.*
import kotlinx.metadata.jvm.*
import org.jetbrains.org.objectweb.asm.ClassWriter
import org.jetbrains.org.objectweb.asm.Opcodes
import org.junit.Test
import java.net.URLClassLoader
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.full.primaryConstructor
import kotlin.test.*

@Suppress("DEPRECATION")
class MetadataSmokeTest {

    @Test
    fun listInlineFunctions() {
        @Suppress("unused")
        class L {
            val x: Int inline get() = 42
            inline fun foo(f: () -> String) = f()
            fun bar() {}
        }

        val classMetadata = L::class.java.readMetadataAsKmClass()

        val inlineFunctions = classMetadata.functions
            .filter { Flag.Function.IS_INLINE(it.flags) }
            .mapNotNull { it.signature?.toString() }

        assertEquals(
            listOf("foo(Lkotlin/jvm/functions/Function0;)Ljava/lang/String;"),
            inlineFunctions
        )
    }

    @Test
    fun produceKotlinClassFile() {
        // First, produce a KotlinMetadata instance with the kotlinx-metadata API, for the following class:
        //     class Hello {
        //         fun hello(): String = "Hello, world!"
        //     }

        val klass = KmClass().apply {
            name = "Hello"
            flags = flagsOf(Flag.IS_PUBLIC)
            constructors += KmConstructor(flagsOf(Flag.IS_PUBLIC)).apply {
                signature = JvmMethodSignature("<init>", "()V")
            }
            functions += KmFunction(flagsOf(Flag.IS_PUBLIC, Flag.Function.IS_DECLARATION), "hello").apply {
                returnType = KmType(flagsOf()).apply {
                    classifier = KmClassifier.Class("kotlin/String")
                }
                signature = JvmMethodSignature("hello", "()Ljava/lang/String;")
            }
        }

        val annotationData = KotlinClassMetadata.writeClass(klass).annotationData

        // Then, produce the bytecode of a .class file with ASM

        val bytes = ClassWriter(0).run {
            visit(Opcodes.V1_6, Opcodes.ACC_PUBLIC + Opcodes.ACC_FINAL + Opcodes.ACC_SUPER, "Hello", null, "java/lang/Object", null)

            // Use the created KotlinMetadata instance to write @kotlin.Metadata annotation on the class file
            visitAnnotation("Lkotlin/Metadata;", true).run {
                visit("mv", annotationData.metadataVersion)
                visit("k", annotationData.kind)
                visitArray("d1").run {
                    annotationData.data1.forEach { visit(null, it) }
                    visitEnd()
                }
                visitArray("d2").run {
                    annotationData.data2.forEach { visit(null, it) }
                    visitEnd()
                }
                visitEnd()
            }

            visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_FINAL, "hello", "()Ljava/lang/String;", null, null).run {
                visitAnnotation("Lorg/jetbrains/annotations/NotNull;", false).visitEnd()
                visitCode()
                visitLdcInsn("Hello, world!")
                visitInsn(Opcodes.ARETURN)
                visitMaxs(1, 1)
                visitEnd()
            }
            visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null).run {
                visitCode()
                visitVarInsn(Opcodes.ALOAD, 0)
                visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
                visitInsn(Opcodes.RETURN)
                visitMaxs(1, 1)
                visitEnd()
            }
            visitEnd()

            toByteArray()
        }

        // Finally, load the generated class, create its instance and invoke the method via Kotlin reflection.
        // Kotlin reflection loads the metadata and builds a mapping from Kotlin symbols to JVM, so if the call succeeds,
        // we can be sure that the metadata is consistent

        val classLoader = object : URLClassLoader(emptyArray()) {
            override fun findClass(name: String): Class<*> =
                if (name == "Hello") defineClass(name, bytes, 0, bytes.size) else super.findClass(name)
        }

        val kClass = classLoader.loadClass("Hello").kotlin
        val hello = kClass.primaryConstructor!!.call()
        val result = kClass.members.single { it.name == "hello" }.call(hello) as String

        assertEquals("Hello, world!", result)
    }

    @Test
    fun jvmInternalName() {
        class ClassNameReader : KmClassVisitor() {
            lateinit var className: ClassName

            override fun visit(flags: Flags, name: ClassName) {
                className = name
            }
        }

        class L

        val l = ClassNameReader().run {
            (KotlinClassMetadata.read(L::class.java.getMetadata()) as KotlinClassMetadata.Class).accept(this)
            className
        }
        assertEquals(".kotlinx/metadata/test/MetadataSmokeTest\$jvmInternalName\$L", l)
        assertEquals("kotlinx/metadata/test/MetadataSmokeTest\$jvmInternalName\$L", l.toJvmInternalName())

        val coroutineContextKey = ClassNameReader().run {
            (KotlinClassMetadata.read(CoroutineContext.Key::class.java.getMetadata()) as KotlinClassMetadata.Class).accept(this)
            className
        }
        assertEquals("kotlin/coroutines/CoroutineContext.Key", coroutineContextKey)
        assertEquals("kotlin/coroutines/CoroutineContext\$Key", coroutineContextKey.toJvmInternalName())
    }

    @Test
    fun lambdaVersionRequirement() {
        val x: suspend Int.(String, String) -> Unit = { _, _ -> }
        val annotation = x::class.java.getMetadata()
        val metadata = KotlinClassMetadata.read(annotation) as KotlinClassMetadata.SyntheticClass
        metadata.accept(KmLambda())
    }

    @Test
    fun unstableParameterNames() {
        @Suppress("unused", "UNUSED_PARAMETER")
        class Test(a: String, b: Int, c: Boolean) {
            fun foo(a: String, b: Int, c: Boolean) = Unit
        }

        val classWithStableParameterNames = Test::class.java.readMetadataAsKmClass()

        classWithStableParameterNames.constructors.forEach { assertFalse(Flag.Constructor.HAS_NON_STABLE_PARAMETER_NAMES(it.flags)) }
        classWithStableParameterNames.functions.forEach { assertFalse(Flag.Function.HAS_NON_STABLE_PARAMETER_NAMES(it.flags)) }

        val newMetadata = KotlinClassMetadata.writeClass(
            KmClass().apply {
                classWithStableParameterNames.accept(
                    object : KmClassVisitor(this) {
                        override fun visitConstructor(flags: Flags) =
                            super.visitConstructor(flags + flagsOf(Flag.Constructor.HAS_NON_STABLE_PARAMETER_NAMES))

                        override fun visitFunction(flags: Flags, name: String) =
                            super.visitFunction(flags + flagsOf(Flag.Function.HAS_NON_STABLE_PARAMETER_NAMES), name)
                    }
                )
            }
        )

        val classWithUnstableParameterNames = newMetadata.toKmClass()

        classWithUnstableParameterNames.constructors.forEach { assertTrue(Flag.Constructor.HAS_NON_STABLE_PARAMETER_NAMES(it.flags)) }
        classWithUnstableParameterNames.functions.forEach { assertTrue(Flag.Function.HAS_NON_STABLE_PARAMETER_NAMES(it.flags)) }
    }

    @Test
    @OptIn(UnstableMetadataApi::class)
    fun metadataVersionEarlierThan1_4() {
        val dummy = MetadataSmokeTest::class.java.readMetadataAsKmClass()
        val mv = intArrayOf(1, 3)
        assertFailsWith<IllegalArgumentException> { KotlinClassMetadata.writeClass(dummy, mv) } // We can't write empty KmClass()
        assertFailsWith<IllegalArgumentException> { KotlinClassMetadata.writeFileFacade(KmPackage(), mv) }
        assertFailsWith<IllegalArgumentException> { KotlinClassMetadata.writeMultiFileClassFacade(listOf("A"), mv) }
        assertFailsWith<IllegalArgumentException> { KotlinClassMetadata.writeMultiFileClassPart(KmPackage(), "A", mv) }
        assertFailsWith<IllegalArgumentException> { KotlinClassMetadata.writeSyntheticClass(mv) }

        KotlinModuleMetadata.write(KmModule(), mv)
    }

    @Test
    fun jvmClassFlags() {
        // Test that we can (de-)serialize the jvmClassFlags extension. All the flags that currently
        // exist are controlled by compiler options, so we have to manually create metadata with the
        // flags set. Since the current flags only apply to interfaces with default functions we modify
        // the metadata for the kotlin.coroutines.CoroutineContext interface.
        val jvmClassFlags: Flags = flagsOf(
            JvmFlag.Class.IS_COMPILED_IN_COMPATIBILITY_MODE,
            JvmFlag.Class.HAS_METHOD_BODIES_IN_INTERFACE
        )

        val metadata = CoroutineContext::class.java.getMetadata()
        val kmClass = metadata.readAsKmClass()
        kmClass.jvmFlags = jvmClassFlags

        val kmClassCopy = KotlinClassMetadata
            .writeClass(kmClass, metadata.metadataVersion, metadata.extraInt)
            .toKmClass()
        assertEquals(kmClassCopy.jvmFlags, jvmClassFlags)
    }
}
