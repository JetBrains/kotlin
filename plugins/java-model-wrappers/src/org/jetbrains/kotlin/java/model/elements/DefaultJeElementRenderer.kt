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

package org.jetbrains.kotlin.java.model.elements

import org.jetbrains.kotlin.java.model.JeElement
import org.jetbrains.kotlin.java.model.types.*
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.type.TypeMirror

class DefaultJeElementRenderer : JeElementRenderer {
    override fun render(element: JeElement): String {
        return when (element) {
            is JePackageElement -> buildString {
                appendln("package ${element.qualifiedName}").appendln()
                element.enclosedElements.forEach { appendln(render(it).withMargin()).appendln() }
            }
            is JeTypeElement -> buildString {
                val classType = when (element.kind) {
                    ElementKind.ANNOTATION_TYPE -> "@interface"
                    ElementKind.INTERFACE -> "interface"
                    ElementKind.ENUM -> "enum"
                    ElementKind.CLASS -> "class"
                    else -> throw IllegalStateException("Invalid class type: ${element.kind}")
                }
                appendln(renderModifiers(element) + classType + ' ' + element.simpleName + " {")
                appendln(element.enclosedElements.joinToString(LINE_SEPARATOR.repeat(2)) { render(it).withMargin() })
                append("}")
            }
            is JeVariableElement -> renderModifiers(element) + renderType(element.asType()) + " " + element.simpleName
            is JeMethodExecutableElement -> buildString {
                append(renderModifiers(element) + renderType(element.returnType, noneAsVoid = true) + " " + element.simpleName)
                append(element.parameters.joinToString(prefix = "(", postfix = ")") { renderType(it.asType()) + " " + it.simpleName })
            }
            is JeClassInitializerExecutableElement -> renderModifiers(element) + "{}"
            else -> throw IllegalArgumentException("Unsupported element: $element")
        }
    }
    
    private fun renderType(type: TypeMirror, noneAsVoid: Boolean = false): String {
        return when (type) {
            is JeNullType -> "null"
            is JeVoidType -> "void"
            is JeErrorType -> "<ERROR>"
            is JeNoneType -> if (noneAsVoid) "void" else throw IllegalArgumentException("Unexpected 'none' type")
            is JePsiType -> type.psiType.getCanonicalText(false)
            else -> throw IllegalArgumentException("Unsupported type: $type")
        }
    }
    
    private fun renderModifiers(element: Element): String {
        val renderedAnnotations = renderAnnotations(element)
        val modifiers = element.modifiers
        
        return if (modifiers.isEmpty())
            renderedAnnotations
        else
            renderedAnnotations + modifiers.map { it.name.toLowerCase() }.joinToString(" ", postfix = " ")
    }
    
    private fun renderAnnotations(element: Element): String {
        val annotations = element.annotationMirrors
        if (annotations.isEmpty()) return ""
        return annotations.joinToString(LINE_SEPARATOR, postfix = LINE_SEPARATOR) { renderAnnotation(it) }
    }
    
    private fun renderAnnotation(anno: AnnotationMirror): String {
        val name = "@" + renderType(anno.annotationType)
        val args = anno.elementValues
        if (args.isEmpty()) return name
        
        return name + args
                .map { it.key.simpleName.toString() + " = " + renderAnnotationValue(it.value) }
                .joinToString(prefix = "(", postfix = ")")
    }
    
    private fun renderAnnotationValue(av: AnnotationValue): String {
        return when (av) {
            is JeAnnotationAnnotationValue -> renderAnnotation(av.value)
            is JeArrayAnnotationValue -> av.value.joinToString(prefix = "{ ", postfix = " }") { renderAnnotationValue(it) }
            is JeSingletonArrayAnnotationValue -> av.value.joinToString(prefix = "{ ", postfix = " }") { renderAnnotationValue(it) }
            is JeEnumValueAnnotationValue -> av.value.simpleName.toString()
            is JeErrorAnnotationValue -> "<ERROR>"
            is JeExpressionAnnotationValue, is JeLiteralAnnotationValue -> av.value?.let { renderConstantValue(it) } ?: "null"
            is JeTypeAnnotationValue -> renderType(av.value) + ".class"
            else -> throw IllegalArgumentException("Unsupported annotation value: $av")
        }
    }
    
    private fun renderConstantValue(value: Any?): String {
        return when (value) {
            null -> "null"
            is String -> "\"" + value.replace("\"", "\\\"") + "\""
            else -> value.toString()
        }
    }
    
    private fun String.withMargin() = lines().map { MARGIN + it }.joinToString(LINE_SEPARATOR)
    
    private companion object {
        val MARGIN = "    "
        val LINE_SEPARATOR: String = System.getProperty("line.separator")
    }
}