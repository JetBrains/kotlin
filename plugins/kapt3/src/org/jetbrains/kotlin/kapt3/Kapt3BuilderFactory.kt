/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.kapt3

import org.jetbrains.kotlin.codegen.AbstractClassBuilder
import org.jetbrains.kotlin.codegen.ClassBuilder
import org.jetbrains.kotlin.codegen.ClassBuilderFactory
import org.jetbrains.kotlin.codegen.ClassBuilderMode
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.org.objectweb.asm.ClassWriter
import org.jetbrains.org.objectweb.asm.FieldVisitor
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.tree.ClassNode
import org.jetbrains.org.objectweb.asm.tree.FieldNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode

internal class Kapt3BuilderFactory : ClassBuilderFactory {
    internal val compiledClasses = mutableListOf<ClassNode>()
    internal val origins = mutableMapOf<Any, JvmDeclarationOrigin>()

    override fun getClassBuilderMode(): ClassBuilderMode = ClassBuilderMode.KAPT3

    override fun newClassBuilder(origin: JvmDeclarationOrigin): AbstractClassBuilder.Concrete {
        val classNode = ClassNode()
        compiledClasses += classNode
        origins.put(classNode, origin)
        return Kapt3ClassBuilder(classNode)
    }

    private inner class Kapt3ClassBuilder(val classNode: ClassNode) : AbstractClassBuilder.Concrete(classNode) {
        override fun newField(
                origin: JvmDeclarationOrigin,
                access: Int,
                name: String,
                desc: String,
                signature: String?,
                value: Any?
        ): FieldVisitor {
            val fieldNode = super.newField(origin, access, name, desc, signature, value) as FieldNode
            origins.put(fieldNode, origin)
            return fieldNode
        }

        override fun newMethod(
                origin: JvmDeclarationOrigin,
                access: Int,
                name: String,
                desc: String,
                signature: String?,
                exceptions: Array<out String>?
        ): MethodVisitor {
            val methodNode = super.newMethod(origin, access, name, desc, signature, exceptions) as MethodNode
            origins.put(methodNode, origin)
            return methodNode
        }
    }

    override fun asBytes(builder: ClassBuilder): ByteArray {
        val classWriter = ClassWriter(0)
        (builder as Kapt3ClassBuilder).classNode.accept(classWriter)
        return classWriter.toByteArray()
    }

    override fun asText(builder: ClassBuilder) = throw UnsupportedOperationException()

    override fun close() {}
}