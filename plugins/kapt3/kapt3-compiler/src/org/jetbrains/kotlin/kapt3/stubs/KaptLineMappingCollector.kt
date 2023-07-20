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

package org.jetbrains.kotlin.kapt3.stubs

import com.sun.tools.javac.tree.JCTree
import org.jetbrains.kotlin.kapt3.KaptContextForStubGeneration
import org.jetbrains.kotlin.kapt3.base.stubs.KotlinPosition
import org.jetbrains.kotlin.kapt3.base.stubs.getJavacSignature
import org.jetbrains.org.objectweb.asm.tree.ClassNode
import org.jetbrains.org.objectweb.asm.tree.FieldNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode

class KaptLineMappingCollector(private val kaptContext: KaptContextForStubGeneration) : KaptLineMappingCollectorBase() {
    fun registerClass(clazz: ClassNode) {
        register(clazz, clazz.name)
    }

    fun registerMethod(clazz: ClassNode, method: MethodNode) {
        register(method, clazz.name + "#" + method.name + method.desc)
    }

    fun registerField(clazz: ClassNode, field: FieldNode) {
        register(field, clazz.name + "#" + field.name)
    }

    fun registerSignature(declaration: JCTree.JCMethodDecl, method: MethodNode) {
        signatureInfo[declaration.getJavacSignature()] = method.name + method.desc
    }

    fun getPosition(clazz: ClassNode): KotlinPosition? = lineInfo[clazz.name]
    fun getPosition(clazz: ClassNode, method: MethodNode): KotlinPosition? = lineInfo[clazz.name + "#" + method.name + method.desc]
    fun getPosition(clazz: ClassNode, field: FieldNode): KotlinPosition? = lineInfo[clazz.name + "#" + field.name]

    private fun register(asmNode: Any, fqName: String) {
        val psiElement = kaptContext.origins[asmNode]?.element ?: return
        register(fqName, psiElement)
    }
}