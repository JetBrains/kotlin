/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kaptlite.stubs.model

import org.jetbrains.kaptlite.signature.SigType
import org.jetbrains.kaptlite.signature.SignatureParser
import org.jetbrains.kaptlite.stubs.StubGeneratorContext
import org.jetbrains.kaptlite.stubs.util.*
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaPropertyDescriptor
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.FieldNode
import org.kotlin.kaptlite.kdoc.KDocParsingHelper
import javax.lang.model.element.Modifier

class JavaFieldStub(
    private val name: String,
    private val modifiers: Set<Modifier>,
    private val type: SigType,
    private val initializer: JavaValue?,
    private val annotations: List<JavaAnnotationStub>,
    private val javadoc: String?
) : Renderable {
    companion object {
        fun parse(context: StubGeneratorContext, node: FieldNode): JavaFieldStub? {
            if (node.isSynthetic) {
                return null
            }

            val origin = context.getFieldOrigin(node)
            val name = node.name
            val type = parseType(context, node)

            val initializer = getEnhancedFieldInitializer(context, node, origin) ?: parseInitializer(context, node, type)
            val annotations = JavaAnnotationStub.parse(context, node.access, node.visibleAnnotations, node.invisibleAnnotations)
            val javadoc = context.getKDocComment(KDocParsingHelper.DeclarationKind.FIELD, origin)

            if (!context.checker.checkField(name, type, annotations, origin)) {
                return null
            }

            return JavaFieldStub(name, parseModifiers(node), type, initializer, annotations, javadoc)
        }

        private fun getEnhancedFieldInitializer(context: StubGeneratorContext, node: FieldNode, origin: JvmDeclarationOrigin): JavaValue? {
            val value = node.value

            if (value != null && isSimpleValue(value)) {
                val propertyDescriptor = origin.descriptor as? PropertyDescriptor
                val ktProperty = origin.element as? KtProperty
                val initializer = ktProperty?.initializer
                if (propertyDescriptor != null && propertyDescriptor.compileTimeInitializer != null) {
                    if (ktProperty != null && !ktProperty.hasDelegate() && initializer != null) {
                        val expression = KtPsiUtil.deparenthesize(initializer)
                        val descriptor = expression.getResolvedCall(context.bindingContext)?.resultingDescriptor
                        if (descriptor is JavaPropertyDescriptor && descriptor.isConst && !isBuiltInConst(descriptor)) {
                            return JavaValue.VExpressionUnsafe(descriptor.fqNameSafe.asString())
                        }
                    }
                }
            }

            return null
        }

        private fun isBuiltInConst(descriptor: PropertyDescriptor): Boolean {
            val containingClassDescriptor = descriptor.containingDeclaration as? ClassDescriptor ?: return false
            if (!containingClassDescriptor.isCompanionObject) {
                return false
            }

            val classDescriptor = containingClassDescriptor.containingDeclaration as? ClassDescriptor ?: return false
            return KotlinBuiltIns.isPrimitiveType(classDescriptor.defaultType)
        }

        private fun isSimpleValue(value: Any?): Boolean {
            return when (value) {
                is Byte, is Short, is Int, is Long, is Char, is Boolean, is Float, is Double -> true
                is String -> true
                is List<*> -> value.isNotEmpty() && isSimpleValue(value.first())
                else -> false
            }
        }

        private fun parseInitializer(context: StubGeneratorContext, node: FieldNode, type: SigType): JavaValue? {
            val value = node.value ?: return if (node.isStatic) type.defaultValue else null

            if (node.desc == "Z") {
                return JavaValue.VBoolean(value != 0)
            }

            return JavaValue.parse(context, value)
        }

        private fun parseType(context: StubGeneratorContext, node: FieldNode): SigType {
            val signature = node.signature
            return if (signature == null) {
                parseType(context, Type.getType(node.desc)).patchType(context)
            } else {
                SignatureParser.parseFieldSignature(signature).patchType(context)
            }
        }
    }

    override fun CodeScope.render() {
        appendJavadoc(javadoc)
        appendList(annotations, "\n", postfix = "\n")
        appendModifiers(modifiers).append(' ')
        append(type).append(' ')
        append(name)
        if (initializer != null) {
            append(" = ")
            append(initializer)
        }
        append(';')
    }
}