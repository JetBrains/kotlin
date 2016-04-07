/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
@file:JvmName("UastUtils")
package org.jetbrains.uast

import org.jetbrains.uast.visitor.UastVisitor

internal val ERROR_NAME = "<error>"

/**
 * Returns the containing class of an element.
 *
 * @return the containing [UClass] element,
 *         or null if the receiver is null, or it is a top-level declaration.
 */
tailrec fun UElement?.getContainingClass(): UClass? {
    val parent = this?.parent ?: return null
    if (parent is UClass) return parent
    return parent.getContainingClass()
}

/**
 * Returns the containing file of an element.
 *
 * @return the containing [UFile] element,
 *         or null if the receiver is null, or the element is not inside a [UFile] (it is abmormal).
 */
tailrec fun UElement?.getContainingFile(): UFile? {
    val parent = this?.parent ?: return null
    if (parent is UFile) return parent
    return parent.getContainingFile()
}

fun UElement?.getContainingClassOrEmpty() = getContainingClass() ?: UClassNotResolved

/**
 * Returns the containing function of an element.
 *
 * @return the containing [UFunction] element,
 *         or null if the receiver is null, or the element is not inside a [UFunction].
 */
tailrec fun UElement?.getContainingFunction(): UFunction? {
    val parent = this?.parent ?: return null
    if (parent is UFunction) return parent
    return parent.getContainingFunction()
}

/**
 * Returns the containing declaration of an element.
 *
 * @return the containing [UDeclaration] element,
 *         or null if the receiver is null, or the element is a top-level declaration.
 */
tailrec fun UElement?.getContainingDeclaration(): UDeclaration? {
    val parent = this?.parent ?: return null
    if (parent is UDeclaration) return parent
    return parent.getContainingDeclaration()
}

/**
 * Checks if the element is a top-level declaration.
 *
 * @return true if the element is a top-level declaration, false otherwise.
 */
fun UDeclaration.isTopLevel() = parent is UFile

/**
 * Builds the log message for the [UElement.logString] function.
 *
 * @param firstLine the message line (the interface name, some optional information).
 * @param nested nested UElements. Could be `List<UElement>`, [UElement] or `null`.
 * @throws IllegalStateException if the [nested] argument is invalid.
 * @return the rendered log string.
 */
fun UElement.log(firstLine: String, vararg nested: Any?): String {
    return (if (firstLine.isBlank()) "" else firstLine + "\n") + nested.joinToString("\n") {
        when (it) {
            null -> "<no element>".withMargin
            is List<*> ->
                @Suppress("UNCHECKED_CAST")
                (it as List<UElement>).logString()
            is UElement -> it.logString().withMargin
            else -> error("Invalid element type: $it")
        }
    }
}

val String.withMargin: String
    get() = lines().joinToString("\n") { "    " + it }

fun List<UElement>.acceptList(visitor: UastVisitor) {
    for (element in this) {
        element.accept(visitor)
    }
}

@Suppress("UNCHECKED_CAST")
fun UClass.findFunctions(name: String) = declarations.filter { it is UFunction && it.matchesName(name) } as List<UFunction>

fun UCallExpression.getReceiver(): UExpression? = (this.parent as? UQualifiedExpression)?.receiver

/**
 * Resolves the receiver element if it implements [UResolvable].
 *
 * @return the resolved element, or null if the element was not resolved, or if the receiver element is not an [UResolvable].
 */
fun UElement.resolveIfCan(context: UastContext): UDeclaration? = (this as? UResolvable)?.resolve(context)

/**
 * Find an annotation with the required qualified name.
 *
 * @param fqName the qualified name to search
 * @return [UAnnotation] element if the annotation with the specified [fqName] was found, null otherwise.
 */
fun UAnnotated.findAnnotation(fqName: String) = annotations.firstOrNull { it.fqName == fqName }

/**
 * Get all class declarations (including supertypes).
 *
 * @param context the Uast context
 * @return the list of declarations for the receiver class
 */
fun UClass.getAllDeclarations(context: UastContext): List<UDeclaration> = mutableListOf<UDeclaration>().apply {
    this += declarations
    for (superType in superTypes) {
        superType.resolveClass(context)?.declarations?.let { this += it }
    }
}

fun UClass.getAllFunctions(context: UastContext) = getAllDeclarations(context).filterIsInstance<UFunction>()

tailrec fun UQualifiedExpression.getCallElementFromQualified(): UCallExpression? {
    val selector = this.selector
    return when (selector) {
        is UQualifiedExpression -> selector.getCallElementFromQualified()
        is UCallExpression -> selector
        else -> null
    }
}

fun UCallExpression.getQualifiedCallElement(): UExpression {
    fun findParent(element: UExpression?): UExpression? = when (element) {
        is UQualifiedExpression -> findParent(element.parent as? UExpression) ?: element
        else -> null
    }

    return findParent(parent as? UExpression) ?: this
}

inline fun <reified T: UElement> UElement.getParentOfType(): T? = getParentOfType(T::class.java)

@JvmOverloads
fun <T: UElement> UElement.getParentOfType(clazz: Class<T>, strict: Boolean = true): T? {
    tailrec fun findParent(element: UElement?): UElement? {
        return when {
            element == null -> null
            clazz.isInstance(element) -> element
            else -> findParent(element.parent)
        }
    }

    @Suppress("UNCHECKED_CAST")
    return findParent(if (!strict) this else parent) as? T
}

fun <T> UClass.findStaticMemberOfType(name: String, type: Class<out T>): T? {
    for (companion in companions) {
        val member = companion.declarations.firstOrNull {
            it.name == name && type.isInstance(it) && it is UModifierOwner && it.hasModifier(UastModifier.STATIC)
        }
        @Suppress("UNCHECKED_CAST")
        if (member != null) return member as T
    }

    @Suppress("UNCHECKED_CAST")
    return declarations.firstOrNull {
        it.name == name && it is UModifierOwner
                && it.hasModifier(UastModifier.STATIC) && type.isInstance(it)
    } as T
}

fun UExpression.asQualifiedIdentifiers(): List<String>? {
    var error = false
    val list = mutableListOf<String>()
    fun addIdentifiers(expr: UQualifiedExpression) {
        val receiver = expr.receiver
        val selector = expr.selector as? USimpleReferenceExpression ?: run { error = true; return }
        when (receiver) {
            is UQualifiedExpression -> addIdentifiers(receiver)
            is USimpleReferenceExpression -> list += receiver.identifier
            else -> {
                error = true
                return
            }
        }
        list += selector.identifier
    }
    when (this) {
        is UQualifiedExpression -> addIdentifiers(this)
        is USimpleReferenceExpression -> listOf(identifier)
        else -> return null
    }
    return if (error) null else list
}

fun UExpression.matchesQualified(fqName: String): Boolean {
    val identifiers = this.asQualifiedIdentifiers() ?: return false
    val passedIdentifiers = fqName.split('.')
    return identifiers == passedIdentifiers
}

fun UExpression.startsWithQualified(fqName: String): Boolean {
    val identifiers = this.asQualifiedIdentifiers() ?: return false
    val passedIdentifiers = fqName.split('.')
    identifiers.forEachIndexed { i, identifier ->
        if (identifier != passedIdentifiers[i]) return false
    }
    return true
}

fun UExpression.endsWithQualified(fqName: String): Boolean {
    val identifiers = this.asQualifiedIdentifiers() ?: return false
    val passedIdentifiers = fqName.split('.')
    identifiers.forEachIndexed { i, identifier ->
        if (identifier != passedIdentifiers[i]) return false
    }
    return true
}