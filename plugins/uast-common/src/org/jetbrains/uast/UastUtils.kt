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

import org.jetbrains.uast.kinds.UastClassKind

tailrec fun UElement?.getContainingClass(): UClass? {
    val parent = this?.parent ?: return null
    if (parent is UClass) return parent
    return parent.getContainingClass()
}

tailrec fun UElement?.getContainingFile(): UFile? {
    val parent = this?.parent ?: return null
    if (parent is UFile) return parent
    return parent.getContainingFile()
}

fun UElement?.getContainingClassOrEmpty() = getContainingClass() ?: UClassNotResolved

tailrec fun UElement?.getContainingFunction(): UFunction? {
    val parent = this?.parent ?: return null
    if (parent is UFunction) return parent
    return parent.getContainingFunction()
}

tailrec fun UElement?.getContainingDeclaration(): UDeclaration? {
    val parent = this?.parent ?: return null
    if (parent is UDeclaration) return parent
    return parent.getContainingDeclaration()
}

fun UClass.findProperty(name: String) = declarations.firstOrNull { it is UVariable && it.name == name } as? UVariable

fun UElement.handleTraverse(handler: UastHandler) {
    handler(this)
    this.traverse(handler)
}

fun UDeclaration.isTopLevel() = parent is UFile

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

fun List<UElement>.handleTraverseList(handler: UastHandler) {
    for (element in this) {
        handler(element)
        element.traverse(handler)
    }
}

@Suppress("UNCHECKED_CAST")
fun UClass.findFunctions(name: String) = declarations.filter { it is UFunction && it.matchesName(name) } as List<UFunction>

fun UCallExpression.getReceiver(): UExpression? = (this.parent as? UQualifiedExpression)?.receiver
fun UCallExpression.getReceiverOrEmpty(): UExpression = (this.parent as? UQualifiedExpression)?.receiver ?: EmptyExpression(this)

fun UElement.resolveIfCan(context: UastContext): UDeclaration? = (this as? UResolvable)?.resolve(context)

fun UElement.isThrow() = this is USpecialExpressionList && this.kind == UastSpecialExpressionKind.THROW

fun UAnnotated.findAnnotation(fqName: String) = annotations.firstOrNull { it.fqName == fqName }

fun UClass.getAllDeclarations(context: UastContext): List<UDeclaration> = mutableListOf<UDeclaration>().apply {
    this += declarations
    for (superType in superTypes) {
        superType.resolveClass(context)?.declarations?.let { this += it }
    }
}

fun UClass.getAllFunctions(context: UastContext) = getAllDeclarations(context).filterIsInstance<UFunction>()

fun UCallExpression.getQualifiedCallElement(): UExpression {
    fun findParent(element: UExpression?): UExpression? = when (element) {
        is UQualifiedExpression -> findParent(element.parent as? UExpression) ?: element
        else -> null
    }

    return findParent(parent as? UExpression) ?: this
}

val UDeclaration.fqName: String
    get() {
        val containingFqName = this.getContainingDeclaration()?.fqName
                ?: this.getContainingFile()?.packageFqName
        val containingFqNameWithDot = containingFqName?.let { it + "." } ?: ""
        return containingFqNameWithDot + this.name
    }

inline fun <reified T: UElement> UElement.getParentOfType(): T? = getParentOfType(T::class.java)

fun <T: UElement> UElement.getParentOfType(clazz: Class<T>): T? {
    tailrec fun findParent(element: UElement): UElement? {
        val parent = element.parent
        return when {
            parent == null -> null
            clazz.isInstance(parent) -> parent
            else -> findParent(parent)
        }
    }

    @Suppress("UNCHECKED_CAST")
    return findParent(this) as T
}

fun <T> UClass.findStaticMemberOfType(name: String, type: Class<out T>): T? {
    for (companion in companions) {
        val classKind = companion.kind as? UastClassKind.UastCompanionObject ?: continue
        if (!classKind.default) continue

        val member = companion.declarations.firstOrNull { it.name == name && type.isInstance(it) }
        @Suppress("UNCHECKED_CAST")
        if (member != null) return member as T
    }

    @Suppress("UNCHECKED_CAST")
    return declarations.firstOrNull {
        it.name == name && it is UModifierOwner
                && it.hasModifier(UastModifier.STATIC) && type.isInstance(it)
    } as T
}