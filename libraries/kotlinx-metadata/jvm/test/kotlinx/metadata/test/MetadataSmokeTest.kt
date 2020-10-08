/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.test

import kotlinx.metadata.*
import kotlinx.metadata.jvm.*
import org.jetbrains.org.objectweb.asm.ClassWriter
import org.jetbrains.org.objectweb.asm.Opcodes
import org.junit.Assert.*
import org.junit.Test
import java.net.URLClassLoader
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.full.primaryConstructor

class MetadataSmokeTest {
    private fun Class<*>.readMetadata(): KotlinClassHeader {
        return getAnnotation(Metadata::class.java).run {
            KotlinClassHeader(kind, metadataVersion, bytecodeVersion, data1, data2, extraString, packageName, extraInt)
        }
    }

    @Test
    fun listInlineFunctions() {
        @Suppress("unused")
        class L {
            val x: Int inline get() = 42
            inline fun foo(f: () -> String) = f()
            fun bar() {}
        }

        val classMetadata = KotlinClassMetadata.read(L::class.java.readMetadata()) as KotlinClassMetadata.Class

        val inlineFunctions = classMetadata.toKmClass().functions
            .filter { Flag.Function.IS_INLINE(it.flags) }
            .mapNotNull { it.signature?.asString() }

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

        val header = KotlinClassMetadata.Class.Writer().apply(klass::accept).write().header

        // Then, produce the bytecode of a .class file with ASM

        val bytes = ClassWriter(0).run {
            visit(Opcodes.V1_6, Opcodes.ACC_PUBLIC + Opcodes.ACC_FINAL + Opcodes.ACC_SUPER, "Hello", null, "java/lang/Object", null)

            // Use the created KotlinMetadata instance to write @kotlin.Metadata annotation on the class file
            visitAnnotation("Lkotlin/Metadata;", true).run {
                visit("mv", header.metadataVersion)
                visit("bv", header.bytecodeVersion)
                visit("k", header.kind)
                visitArray("d1").run {
                    header.data1.forEach { visit(null, it) }
                    visitEnd()
                }
                visitArray("d2").run {
                    header.data2.forEach { visit(null, it) }
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
            (KotlinClassMetadata.read(L::class.java.readMetadata()) as KotlinClassMetadata.Class).accept(this)
            className
        }
        assertEquals(".kotlinx/metadata/test/MetadataSmokeTest\$jvmInternalName\$L", l)
        assertEquals("kotlinx/metadata/test/MetadataSmokeTest\$jvmInternalName\$L", l.jvmInternalName)

        val coroutineContextKey = ClassNameReader().run {
            (KotlinClassMetadata.read(CoroutineContext.Key::class.java.readMetadata()) as KotlinClassMetadata.Class).accept(this)
            className
        }
        assertEquals("kotlin/coroutines/CoroutineContext.Key", coroutineContextKey)
        assertEquals("kotlin/coroutines/CoroutineContext\$Key", coroutineContextKey.jvmInternalName)
    }

    @Test
    fun lambdaVersionRequirement() {
        val x: suspend Int.(String, String) -> Unit = { _, _ -> }
        val annotation = x::class.java.getAnnotation(Metadata::class.java)!!
        val metadata = KotlinClassMetadata.read(
            KotlinClassHeader(
                kind = annotation.kind,
                metadataVersion = annotation.metadataVersion,
                bytecodeVersion = annotation.bytecodeVersion,
                data1 = annotation.data1,
                data2 = annotation.data2,
                extraInt = annotation.extraInt,
                extraString = annotation.extraString,
                packageName = annotation.packageName
            )
        ) as KotlinClassMetadata.SyntheticClass
        metadata.accept(KmLambda())
    }

    @Test
    fun unstableParameterNames() {
        @Suppress("unused")
        class Test(a: String, b: Int, c: Boolean) {
            fun foo(a: String, b: Int, c: Boolean) = Unit
        }

        val classWithStableParameterNames =
            (KotlinClassMetadata.read(Test::class.java.readMetadata()) as KotlinClassMetadata.Class).toKmClass()

        classWithStableParameterNames.constructors.forEach { assertFalse(Flag.Constructor.HAS_NON_STABLE_PARAMETER_NAMES(it.flags)) }
        classWithStableParameterNames.functions.forEach { assertFalse(Flag.Function.HAS_NON_STABLE_PARAMETER_NAMES(it.flags)) }

        val newMetadata = KotlinClassMetadata.Class.Writer().let { writer ->
            KmClass().apply {
                classWithStableParameterNames.accept(
                    object : KmClassVisitor(this) {
                        override fun visitConstructor(flags: Flags) =
                            super.visitConstructor(flags + flagsOf(Flag.Constructor.HAS_NON_STABLE_PARAMETER_NAMES))

                        override fun visitFunction(flags: Flags, name: String) =
                            super.visitFunction(flags + flagsOf(Flag.Function.HAS_NON_STABLE_PARAMETER_NAMES), name)
                    }
                )
            }.accept(writer)
            writer.write()
        }

        val classWithUnstableParameterNames = newMetadata.toKmClass()

        classWithUnstableParameterNames.constructors.forEach { assertTrue(Flag.Constructor.HAS_NON_STABLE_PARAMETER_NAMES(it.flags)) }
        classWithUnstableParameterNames.functions.forEach { assertTrue(Flag.Function.HAS_NON_STABLE_PARAMETER_NAMES(it.flags)) }
    }
}
