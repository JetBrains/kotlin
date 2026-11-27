/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt.firapt

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import java.io.Writer
import javax.lang.model.element.*
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.ExecutableType
import javax.lang.model.util.ElementFilter
import javax.lang.model.util.Elements

internal class AptElementUtils(private val env: AptEnv, private val types: AptTypes) : Elements {
    private val byCanonicalName = HashMap<String, AptTypeElement?>()

    override fun getPackageElement(name: CharSequence): PackageElement? = env.packageElement(name.toString())

    override fun getTypeElement(name: CharSequence): TypeElement? = byCanonicalName.getOrPut(name.toString()) {
        // Nested classes: try each split of the canonical name into a package and a chain of class names.
        val parts = name.toString().split('.')
        for (packageParts in parts.size - 1 downTo 0) {
            val classId = ClassId(FqName.fromSegments(parts.take(packageParts)), FqName.fromSegments(parts.drop(packageParts)), false)
            env.typeElement(classId)?.let { return@getOrPut it }
        }
        null
    }

    override fun getElementValuesWithDefaults(a: AnnotationMirror): Map<out ExecutableElement, AnnotationValue> =
        (a as AptAnnotationMirror).valuesWithDefaults()

    override fun getDocComment(e: Element): String? = null

    override fun isDeprecated(e: Element): Boolean =
        e.annotationMirrors.any { (it.annotationType.asElement() as TypeElement).qualifiedName.contentEquals("java.lang.Deprecated") }

    override fun getBinaryName(type: TypeElement): Name = AptName((type as AptTypeElement).internalName.replace('/', '.'))

    override fun getPackageOf(e: Element): PackageElement {
        var current: Element? = e
        while (current != null && current !is PackageElement) current = current.enclosingElement
        return current as PackageElement
    }

    override fun getAllMembers(type: TypeElement): List<Element> {
        val result = mutableListOf<Element>()
        val seenMethods = mutableListOf<ExecutableElement>()
        val seenNames = HashSet<String>()
        val visited = HashSet<TypeElement>()
        fun visit(element: TypeElement, isSelf: Boolean) {
            if (!visited.add(element)) return
            for (member in element.enclosedElements) {
                val isPrivate = Modifier.PRIVATE in member.modifiers
                if (!isSelf && (isPrivate || member.kind == ElementKind.CONSTRUCTOR)) continue
                when (member) {
                    is ExecutableElement -> {
                        if (seenMethods.any { it.simpleName == member.simpleName && sameErasedParameters(it, member) }) continue
                        seenMethods += member
                        result += member
                    }
                    else -> if (seenNames.add(member.kind.name + member.simpleName)) result += member
                }
            }
            (element.superclass as? DeclaredType)?.let { visit(it.asElement() as TypeElement, false) }
            element.interfaces.forEach { visit((it as DeclaredType).asElement() as TypeElement, false) }
        }
        visit(type, true)
        return result
    }

    private fun sameErasedParameters(a: ExecutableElement, b: ExecutableElement): Boolean {
        val pa = a.parameters
        val pb = b.parameters
        return pa.size == pb.size && pa.zip(pb).all { types.isSameType(types.erasure(it.first.asType()), types.erasure(it.second.asType())) }
    }

    override fun getAllAnnotationMirrors(e: Element): List<AnnotationMirror> {
        val result = e.annotationMirrors.toMutableList()
        if (e is TypeElement) {
            var superclass = e.superclass as? DeclaredType
            while (superclass != null) {
                val element = superclass.asElement() as TypeElement
                for (mirror in element.annotationMirrors) {
                    val annotationType = mirror.annotationType.asElement() as TypeElement
                    val inherited = annotationType.annotationMirrors.any {
                        (it.annotationType.asElement() as TypeElement).qualifiedName.contentEquals("java.lang.annotation.Inherited")
                    }
                    if (inherited && result.none { it.annotationType.asElement() === annotationType }) result += mirror
                }
                superclass = element.superclass as? DeclaredType
            }
        }
        return result
    }

    override fun hides(hider: Element, hidden: Element): Boolean = false

    override fun overrides(overrider: ExecutableElement, overridden: ExecutableElement, type: TypeElement): Boolean {
        if (overrider === overridden) return false
        if (overrider.kind != ElementKind.METHOD || overridden.kind != ElementKind.METHOD) return false
        if (overrider.simpleName != overridden.simpleName) return false
        val hiddenModifiers = overridden.modifiers
        if (Modifier.PRIVATE in hiddenModifiers || Modifier.STATIC in hiddenModifiers || Modifier.STATIC in overrider.modifiers) return false
        val overriddenOwner = overridden.enclosingElement as TypeElement
        val overriderOwner = overrider.enclosingElement as TypeElement
        if (!types.isSubtype(types.erasure(overriderOwner.asType()), types.erasure(overriddenOwner.asType()))) return false
        if (hiddenModifiers.none { it == Modifier.PUBLIC || it == Modifier.PROTECTED } &&
            getPackageOf(overriddenOwner) !== getPackageOf(overriderOwner)
        ) return false
        val containing = type.asType() as DeclaredType
        val t1 = types.asMemberOf(containing, overrider) as? ExecutableType ?: return false
        val t2 = types.asMemberOf(containing, overridden) as? ExecutableType ?: return false
        return types.isSubsignature(t1, t2)
    }

    override fun getConstantExpression(value: Any): String = when (value) {
        is String -> "\"" + value.flatMap { escape(it) } .joinToString("") + "\""
        is Char -> "'" + escape(value).joinToString("") + "'"
        is Long -> "${value}L"
        is Float -> when {
            value.isNaN() -> "0.0f/0.0f"
            value.isInfinite() -> if (value > 0) "1.0f/0.0f" else "-1.0f/0.0f"
            else -> "${value}f"
        }
        is Double -> when {
            value.isNaN() -> "0.0/0.0"
            value.isInfinite() -> if (value > 0) "1.0/0.0" else "-1.0/0.0"
            else -> value.toString()
        }
        is Byte -> "(byte)$value"
        is Short -> "(short)$value"
        else -> value.toString()
    }

    private fun escape(c: Char): List<Char> = when (c) {
        '"' -> "\\\"".toList()
        '\'' -> "\\'".toList()
        '\\' -> "\\\\".toList()
        '\n' -> "\\n".toList()
        '\t' -> "\\t".toList()
        '\r' -> "\\r".toList()
        else -> if (c < ' ' || c > '~') "\\u%04x".format(c.code).toList() else listOf(c)
    }

    override fun printElements(w: Writer, vararg elements: Element) {
        elements.forEach { w.write(it.toString()); w.write("\n") }
        w.flush()
    }

    override fun getName(cs: CharSequence): Name = AptName(cs.toString())

    override fun isFunctionalInterface(type: TypeElement): Boolean {
        if (type.kind != ElementKind.INTERFACE) return false
        val abstract = ElementFilter.methodsIn(getAllMembers(type)).filter {
            Modifier.ABSTRACT in it.modifiers && Modifier.STATIC !in it.modifiers
        }
        return abstract.size == 1
    }

}
