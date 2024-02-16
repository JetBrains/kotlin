/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.metadata.test

import kotlin.metadata.*
import kotlin.metadata.jvm.*
import org.jetbrains.org.objectweb.asm.ClassWriter
import org.jetbrains.org.objectweb.asm.Opcodes
import org.junit.Test
import java.net.URLClassLoader
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.full.primaryConstructor
import kotlin.test.*

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
            .filter { it.isInline }
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
            visibility = Visibility.PUBLIC
            constructors += KmConstructor().apply {
                visibility = Visibility.PUBLIC
                signature = JvmMethodSignature("<init>", "()V")
            }
            functions += KmFunction("hello").apply {
                visibility = Visibility.PUBLIC
                kind = MemberKind.DECLARATION
                returnType = KmType().apply {
                    classifier = KmClassifier.Class("kotlin/String")
                }
                signature = JvmMethodSignature("hello", "()Ljava/lang/String;")
            }
        }

        val annotationData = KotlinClassMetadata.Class(klass, JvmMetadataVersion.LATEST_STABLE_SUPPORTED, 0).write()

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

        class L

        val l = (KotlinClassMetadata.readStrict(L::class.java.getMetadata()) as KotlinClassMetadata.Class).kmClass.name
        assertEquals(".kotlin/metadata/test/MetadataSmokeTest\$jvmInternalName\$L", l)
        assertEquals("kotlin/metadata/test/MetadataSmokeTest\$jvmInternalName\$L", l.toJvmInternalName())

        val coroutineContextKey =
            (KotlinClassMetadata.readStrict(CoroutineContext.Key::class.java.getMetadata()) as KotlinClassMetadata.Class).kmClass.name

        assertEquals("kotlin/coroutines/CoroutineContext.Key", coroutineContextKey)
        assertEquals("kotlin/coroutines/CoroutineContext\$Key", coroutineContextKey.toJvmInternalName())
    }

    @Test
    fun lambdaVersionRequirement() {
        val x: suspend Int.(String, String) -> Unit = { _, _ -> }
        val annotation = x::class.java.getMetadata()
        val metadata = KotlinClassMetadata.readStrict(annotation) as KotlinClassMetadata.SyntheticClass
        assertNotNull(metadata.kmLambda)
    }

    @Test
    @Suppress("DEPRECATION_ERROR") // flags will become internal eventually
    fun unstableParameterNames() {
        @Suppress("unused", "UNUSED_PARAMETER")
        class Test(a: String, b: Int, c: Boolean) {
            fun foo(a: String, b: Int, c: Boolean) = Unit
        }

        val classWithStableParameterNames = Test::class.java.readMetadataAsClass()

        classWithStableParameterNames.kmClass.constructors.forEach { assertFalse(Flag.Constructor.HAS_NON_STABLE_PARAMETER_NAMES(it.flags)) }
        classWithStableParameterNames.kmClass.functions.forEach { assertFalse(Flag.Function.HAS_NON_STABLE_PARAMETER_NAMES(it.flags)) }

        classWithStableParameterNames.kmClass.constructors.forEach { assertFalse(it.hasNonStableParameterNames) }
        classWithStableParameterNames.kmClass.functions.forEach { assertFalse(it.hasNonStableParameterNames) }

        classWithStableParameterNames.kmClass.constructors.forEach { it.hasNonStableParameterNames = true }
        classWithStableParameterNames.kmClass.functions.forEach { it.hasNonStableParameterNames = true }

        val newMetadata = classWithStableParameterNames.write()

        val classWithUnstableParameterNames = newMetadata.readAsKmClass()

        classWithUnstableParameterNames.constructors.forEach { assertTrue(Flag.Constructor.HAS_NON_STABLE_PARAMETER_NAMES(it.flags)) }
        classWithUnstableParameterNames.functions.forEach { assertTrue(Flag.Function.HAS_NON_STABLE_PARAMETER_NAMES(it.flags)) }

        classWithUnstableParameterNames.constructors.forEach { assertTrue(it.hasNonStableParameterNames) }
        classWithUnstableParameterNames.functions.forEach { assertTrue(it.hasNonStableParameterNames) }
    }

    @Test
    @OptIn(UnstableMetadataApi::class)
    fun metadataVersionEarlierThan1_4() {
        val dummy = MetadataSmokeTest::class.java.readMetadataAsKmClass()
        val mv = JvmMetadataVersion(1, 3)
        assertFailsWith<IllegalArgumentException> { KotlinClassMetadata.Class(dummy, mv, 0).write() } // We can't write empty KmClass()
        assertFailsWith<IllegalArgumentException> { KotlinClassMetadata.FileFacade(KmPackage(), mv, 0).write() }
        assertFailsWith<IllegalArgumentException> { KotlinClassMetadata.MultiFileClassFacade(listOf("A"), mv, 0).write() }
        assertFailsWith<IllegalArgumentException> { KotlinClassMetadata.MultiFileClassPart(KmPackage(), "A", mv, 0).write() }
        assertFailsWith<IllegalArgumentException> { KotlinClassMetadata.SyntheticClass(null, mv, 0).write() }

        KotlinModuleMetadata(KmModule(), mv).write()
    }

    @Test
    fun jvmClassFlags() {
        // Test that we can (de-)serialize the jvmClassFlags extension. All the flags that currently
        // exist are controlled by compiler options, so we have to manually create metadata with the
        // flags set. Since the current flags only apply to interfaces with default functions we modify
        // the metadata for the kotlin.coroutines.CoroutineContext interface.

        val metadata = CoroutineContext::class.java.readMetadataAsClass()
        val kmClass = metadata.kmClass
        assertFalse(kmClass.isCompiledInCompatibilityMode)
        assertFalse(kmClass.hasMethodBodiesInInterface)
        kmClass.isCompiledInCompatibilityMode = true
        kmClass.hasMethodBodiesInInterface = true

        val kmClassCopy = metadata.write().readAsKmClass()
        assertTrue(kmClassCopy.isCompiledInCompatibilityMode)
        assertTrue(kmClassCopy.hasMethodBodiesInInterface)
    }

    @Test
    fun testDisplayNameSample() {
        class A {}

        val b: (Int) -> Int = @JvmSerializableLambda fun(x: Int) = x

        assertEquals("Class .kotlin/metadata/test/MetadataSmokeTest\$testDisplayNameSample\$A", displayName(A::class.java.getMetadata()))
        assertEquals("Lambda <no name provided>", displayName(b::class.java.getMetadata()))
    }

    fun displayName(metadata: Metadata): String = when (val kcm = KotlinClassMetadata.readStrict(metadata)) {
        is KotlinClassMetadata.Class -> "Class ${kcm.kmClass.name}"
        is KotlinClassMetadata.FileFacade -> "File facade with functions: ${kcm.kmPackage.functions.joinToString { it.name }}"
        is KotlinClassMetadata.SyntheticClass -> kcm.kmLambda?.function?.name?.let { "Lambda $it" } ?: "Synthetic class"
        is KotlinClassMetadata.MultiFileClassFacade -> "Multifile class facade with parts: ${kcm.partClassNames.joinToString()}"
        is KotlinClassMetadata.MultiFileClassPart -> "Multifile class part ${kcm.facadeClassName}"
        is KotlinClassMetadata.Unknown -> "Unknown metadata"
    }
}
