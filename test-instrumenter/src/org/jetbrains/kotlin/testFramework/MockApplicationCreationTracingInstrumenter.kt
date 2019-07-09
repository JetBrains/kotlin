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

package org.jetbrains.kotlin.testFramework

import org.jetbrains.org.objectweb.asm.*
import org.jetbrains.org.objectweb.asm.util.TraceClassVisitor
import java.io.PrintWriter
import java.lang.instrument.ClassFileTransformer
import java.security.ProtectionDomain


class MockApplicationCreationTracingInstrumenter(private val debugInfo: Boolean) : ClassFileTransformer {

    private fun loadTransformAndSerialize(classfileBuffer: ByteArray, lambda: (out: ClassVisitor) -> ClassVisitor): ByteArray {
        val reader = ClassReader(classfileBuffer)

        val writer = ClassWriter(ClassWriter.COMPUTE_FRAMES or ClassWriter.COMPUTE_MAXS)

        val pv = if (debugInfo) {
            TraceClassVisitor(writer, PrintWriter(System.out.writer()))
        }
        else {
            writer
        }

        reader.accept(lambda(pv), 0)

        return writer.toByteArray()
    }

    private fun isMockComponentManagerCreationTracerCanBeLoaded(loader: ClassLoader): Boolean =
            loader.getResource("org/jetbrains/kotlin/testFramework/MockComponentManagerCreationTracer.class") != null

    override fun transform(
            loader: ClassLoader,
            className: String,
            classBeingRedefined: Class<*>?,
            protectionDomain: ProtectionDomain,
            classfileBuffer: ByteArray
    ): ByteArray? {
        if (loader::class.java.name == "org.jetbrains.kotlin.preloading.MemoryBasedClassLoader") return null

        if (className == "com/intellij/mock/MockComponentManager" && isMockComponentManagerCreationTracerCanBeLoaded(loader)) {
            return loadTransformAndSerialize(classfileBuffer, this::transformMockComponentManager)
        }
        else if (className == "com/intellij/mock/MockComponentManager$1" && isMockComponentManagerCreationTracerCanBeLoaded(loader)) {
            return loadTransformAndSerialize(classfileBuffer, this::transformMockComponentManagerPicoContainer)
        }

        return null
    }


    private fun createMethodTransformClassVisitor(
            out: ClassVisitor,
            predicate: (name: String, desc: String) -> Boolean,
            transform: (original: MethodVisitor) -> MethodVisitor
    ): ClassVisitor {
        return object : ClassVisitor(Opcodes.API_VERSION, out) {

            var visited = false

            override fun visitMethod(access: Int, name: String, desc: String, signature: String?, exceptions: Array<out String>?): MethodVisitor {
                val original = super.visitMethod(access, name, desc, signature, exceptions)
                return if (predicate(name, desc)) {
                    assert(!visited)
                    visited = true
                    transform(original)
                }
                else {
                    original
                }
            }

            override fun visitEnd() {
                super.visitEnd()
                assert(visited)
            }
        }
    }

    private fun transformMockComponentManagerPicoContainer(out: ClassVisitor): ClassVisitor {
        return createMethodTransformClassVisitor(out, { name, _ -> name == "getComponentInstance" }) { original ->
            object : MethodVisitor(Opcodes.API_VERSION, original) {
                override fun visitCode() {
                    super.visitCode()
                    visitLabel(Label())
                    visitVarInsn(Opcodes.ALOAD, 0)
                    visitFieldInsn(Opcodes.GETFIELD, "com/intellij/mock/MockComponentManager$1", "this$0", "Lcom/intellij/mock/MockComponentManager;")
                    visitMethodInsn(
                            Opcodes.INVOKESTATIC,
                            "org/jetbrains/kotlin/testFramework/MockComponentManagerCreationTracer",
                            "onGetComponentInstance",
                            "(Lcom/intellij/mock/MockComponentManager;)V",
                            false
                    )
                }
            }
        }
    }

    private fun transformMockComponentManager(out: ClassVisitor): ClassVisitor {
        return createMethodTransformClassVisitor(out, { name, _ -> name == "<init>" }) { original ->
            object : MethodVisitor(Opcodes.API_VERSION, original) {
                override fun visitInsn(opcode: Int) {
                    if (opcode == Opcodes.RETURN) {
                        visitVarInsn(Opcodes.ALOAD, 0)
                        visitMethodInsn(
                                Opcodes.INVOKESTATIC,
                                "org/jetbrains/kotlin/testFramework/MockComponentManagerCreationTracer",
                                "onCreate",
                                "(Lcom/intellij/mock/MockComponentManager;)V",
                                false
                        )
                    }
                    super.visitInsn(opcode)
                }
            }
        }
    }
}
