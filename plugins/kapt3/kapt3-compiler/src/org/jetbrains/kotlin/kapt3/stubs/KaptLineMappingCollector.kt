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
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.extensions.JvmIrDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.ir.erasedUpperBound
import org.jetbrains.kotlin.backend.jvm.ir.fileParent
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.fields
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.kapt3.KaptContextForStubGeneration
import org.jetbrains.kotlin.kapt3.base.stubs.KotlinPosition
import org.jetbrains.kotlin.kapt3.base.stubs.getJavacSignature
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.SpecialNames
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
        val origin = kaptContext.origins[asmNode]
        val ir = (origin as? JvmIrDeclarationOrigin)?.declaration ?: return
        val offset = computeOffset(ir)

        lineInfo[fqName] = KotlinPosition(ir.fileParent.fileEntry.name, isRelativePath = false, offset)
    }

    private fun computeOffset(ir: IrDeclaration): Int {
        if (ir.origin == JvmLoweredDeclarationOrigin.SYNTHETIC_METHOD_FOR_PROPERTY_OR_TYPEALIAS_ANNOTATIONS) {
            // `$annotations` functions have -1 offsets in the IR, but we'd like to keep them closer to the accessors of the corresponding
            // property to maintain the order that was used when line information was computed by PSI.
            val getter = ir.findPropertyGetterOrFieldBySyntheticAnnotationsMethod()
            if (getter != null) return getter.startOffset
        }

        // All other functions with -1 offsets are supposed to be synthesized by the compiler and it makes sense for them to be at the end.
        if (ir.startOffset == UNDEFINED_OFFSET) return Int.MAX_VALUE

        // Some declarations are synthesized by the compiler, but their offsets are not -1: enum entries, delegated members.
        // Let's place them at the end as well, as other synthesized declarations.

        if (ir is IrFunction && ir.name == SpecialNames.ENUM_GET_ENTRIES && ir.origin == IrDeclarationOrigin.ENUM_CLASS_SPECIAL_MEMBER)
            return Int.MAX_VALUE

        if (ir.origin == IrDeclarationOrigin.DELEGATED_MEMBER)
            return Int.MAX_VALUE

        return ir.startOffset
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrDeclaration.findPropertyGetterOrFieldBySyntheticAnnotationsMethod(): IrDeclaration? {
        if (this !is IrSimpleFunction) return null
        for (declaration in parentAsClass.functions + parentAsClass.fields) {
            val property = when (declaration) {
                is IrSimpleFunction -> declaration.correspondingPropertySymbol?.owner
                is IrField -> declaration.correspondingPropertySymbol?.owner
                else -> null
            } ?: continue
            val expectedAnnotationsMethodName =
                JvmAbi.getSyntheticMethodNameForAnnotatedProperty(JvmAbi.getterName(property.name.asString()))
            val extensionReceiver = (declaration as? IrSimpleFunction)?.extensionReceiverParameter
            if (expectedAnnotationsMethodName == name.asString() &&
                extensionReceiver?.type?.erasedUpperBound == valueParameters.firstOrNull()?.type?.erasedUpperBound
            ) {
                return declaration
            }
        }
        return null
    }
}
