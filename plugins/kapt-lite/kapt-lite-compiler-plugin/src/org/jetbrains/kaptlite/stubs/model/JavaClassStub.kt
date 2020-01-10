/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kaptlite.stubs.model

import org.jetbrains.kaptlite.stubs.JAVA_LANG_OBJECT
import org.jetbrains.kaptlite.signature.ClassSignature
import org.jetbrains.kaptlite.signature.SigType
import org.jetbrains.kaptlite.signature.SignatureParser
import org.jetbrains.kaptlite.signature.isJavaLangObject
import org.jetbrains.kaptlite.stubs.StubGeneratorContext
import org.jetbrains.kaptlite.stubs.util.*
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.ClassNode
import org.kotlin.kaptlite.kdoc.KDocParsingHelper
import javax.lang.model.element.Modifier
import kotlin.collections.ArrayList

class JavaClassStub(
    val name: JavaClassName,
    private val modifiers: Set<Modifier>,
    private val kind: TypeKind,
    private val signature: ClassSignature,
    private val enumValues: List<JavaEnumValue>,
    private val methods: List<JavaMethodStub>,
    private val fields: List<JavaFieldStub>,
    private val nestedClasses: List<JavaClassStub>,
    private val annotations: List<JavaAnnotationStub>,
    private val javadoc: String?
) : Renderable {
    enum class NestingKind {
        TOP_LEVEL, NESTED, INNER
    }

    companion object {
        fun parse(context: StubGeneratorContext, node: ClassNode, nestingKind: NestingKind): JavaClassStub? {
            if (node.isSynthetic) {
                return null
            }

            val origin = context.getClassOrigin(node)

            val descriptor = origin.descriptor
            val isDefaultImpls = descriptor is ClassDescriptor
                    && descriptor.kind == ClassKind.INTERFACE
                    && node.isPublic && node.isFinal
                    && node.name.endsWith("${descriptor.name.asString()}\$DefaultImpls")

            // DefaultImpls without any contents don't have INNERCLASS'es inside it (and inside the parent interface)
            if (isDefaultImpls && (nestingKind == NestingKind.TOP_LEVEL || (node.fields.isEmpty() && node.methods.isEmpty()))) {
                return null
            }

            val name = context.getClassName(node)

            val typeKind = getTypeKind(node.access)
            val signature = parseClassSignature(context, node)
            val constructorData = JavaMethodStub.ConstructorData(emptyList(), emptyList()) // getConstructorData(context, node)

            val methods = (node.methods ?: emptyList())
                .mapNotNullTo(mutableListOf()) {
                    JavaMethodStub.parse(context, it, node, name, nestingKind == NestingKind.INNER, constructorData)
                }

            if (methods.none { it.isConstructor } && typeKind == TypeKind.CLASS) {
                methods += JavaMethodStub(
                    name = name.className,
                    modifiers = setOf(Modifier.PRIVATE),
                    isConstructor = true,
                    data = JavaMethodStub.MethodData(emptyList(), emptyList(), SigType.Primitive.VOID, emptyList()),
                    annotations = emptyList(),
                    javadoc = null,
                    constructorData = constructorData
                )
            }

            val enumValues = (node.fields ?: emptyList())
                .filter { it.isEnumValue }
                .mapNotNull { field ->
                    val valueOrigin = context.getFieldOrigin(field)
                    JavaEnumValue(field.name, context.getKDocComment(KDocParsingHelper.DeclarationKind.FIELD, valueOrigin))
                }

            val fields = (node.fields ?: emptyList())
                .filter { !it.isEnumValue }
                .mapNotNull { JavaFieldStub.parse(context, it) }

            val nestedClasses = ArrayList<JavaClassStub>(0)
            for (innerClass in node.innerClasses) {
                if (innerClass.outerName == node.name) {
                    if (enumValues.any { it.name == innerClass.innerName }) {
                        continue
                    }

                    val childNode = context.loader.load(innerClass.name) ?: continue
                    val childNestingKind = when {
                        nestingKind == NestingKind.INNER || isInner(childNode, node) -> NestingKind.INNER
                        else -> NestingKind.NESTED
                    }
                    val childClass = parse(context, childNode, childNestingKind) ?: continue
                    nestedClasses += childClass
                }
            }

            // kotlin.jvm.JvmName is applicable to file facades but not applicable to classes
            val annotations = JavaAnnotationStub.parse(context, node.access, node.visibleAnnotations, node.invisibleAnnotations)
                .filter { it.name.toString() != "kotlin.jvm.JvmName" }

            val javadoc = context.getKDocComment(KDocParsingHelper.DeclarationKind.CLASS, origin)

            var modifiers = parseModifiers(node)
            if (nestingKind == NestingKind.NESTED) {
                modifiers = modifiers + Modifier.STATIC
            }

            if (!context.checker.checkClass(name, signature, enumValues, annotations, origin)) {
                return null
            }

            return JavaClassStub(
                name,
                modifiers, typeKind,
                signature,
                enumValues, methods, fields, nestedClasses,
                annotations,
                javadoc
            )
        }

        private fun isInner(node: ClassNode, owner: ClassNode): Boolean {
            val thisField = node.fields.find { it.name == "this$0" } ?: return false
            return Type.getType(thisField.desc).internalName == owner.name
        }

        private fun parseClassSignature(context: StubGeneratorContext, node: ClassNode): ClassSignature {
            return if (node.signature == null) {
                val superClass = parseType(context, (node.superName ?: JAVA_LANG_OBJECT))
                val interfaces = (node.interfaces ?: emptyList()).map { parseType(context, it) }
                ClassSignature(emptyList(), superClass, interfaces)
            } else {
                val signature = SignatureParser.parseClassSignature(node.signature)
                return ClassSignature(
                    typeParameters = signature.typeParameters.map { it.copy(bounds = it.bounds.patchTypes(context)) },
                    superClass = signature.superClass.patchType(context),
                    interfaces = signature.interfaces.patchTypes(context)
                )
            }
        }

        private fun getTypeKind(access: Int): TypeKind {
            if (access.test(Opcodes.ACC_ANNOTATION)) return TypeKind.ANNOTATION
            if (access.test(Opcodes.ACC_INTERFACE)) return TypeKind.INTERFACE
            if (access.test(Opcodes.ACC_ENUM)) return TypeKind.ENUM
            return TypeKind.CLASS
        }
    }

    class JavaEnumValue(val name: String, val javadoc: String?)

    enum class TypeKind(val keyword: String) {
        INTERFACE("interface"), ANNOTATION("@interface"), ENUM("enum"), CLASS("class")
    }

    override fun CodeScope.render() {
        appendJavadoc(javadoc)
        appendList(annotations, "\n", postfix = "\n")
        appendModifiers(modifiers).append(' ')
        append(kind.keyword).append(' ').append(name.className)
        appendList(signature.typeParameters, prefix = "<", postfix = ">") { append(it) }
        if (kind == TypeKind.CLASS && !signature.superClass.isJavaLangObject) {
            append(" extends ").append(signature.superClass)
        }
        if (kind != TypeKind.ANNOTATION) {
            val keyword = if (kind == TypeKind.INTERFACE) "extends" else "implements"
            appendList(signature.interfaces, prefix = " $keyword ") { append(it) }
        }

        append(' ').block {
            var isFirst = true

            if (kind == TypeKind.ENUM) {
                val implMethods = methods
                    .filter { it.modifiers.contains(Modifier.ABSTRACT) }
                    .map { it.copy(modifiers = it.modifiers - Modifier.ABSTRACT) }

                val constructorParameters = methods.asSequence()
                    .filter { it.isConstructor }
                    .minBy { it.data.parameters.size }?.data?.parameters
                    ?: emptyList()

                for (value in enumValues) {
                    appendJavadoc(value.javadoc)
                    append(value.name)
                    appendList(constructorParameters, prefix = "(", postfix = ")") { append(it.type.defaultValue) }

                    if (implMethods.isNotEmpty()) {
                        append(' ').block {
                            appendList(implMethods, "\n\n")
                        }
                    }

                    append(',').newLine()
                }
                append(";")
                isFirst = false
            }

            appendList(fields, "\n", prefix = if (!isFirst) "\n\n" else "")
            isFirst = isFirst && fields.isEmpty()

            appendList(methods, "\n\n", prefix = if (!isFirst) "\n\n" else "")
            isFirst = isFirst && methods.isEmpty()

            appendList(nestedClasses, "\n\n", prefix = if (!isFirst) "\n\n" else "")
        }
    }
}