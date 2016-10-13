package org.jetbrains.uast

import com.intellij.lang.Language
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.project.Project
import com.intellij.psi.*

interface UastLanguagePlugin {
    companion object {
        val extensionPointName = ExtensionPointName.create<UastLanguagePlugin>("org.jetbrains.uast.uastLanguagePlugin")

        fun getInstances(project: Project): Collection<UastLanguagePlugin> {
            val projectArea = Extensions.getArea(project)
            if (!projectArea.hasExtensionPoint(extensionPointName.name)) return listOf()
            return projectArea.getExtensionPoint(extensionPointName).extensions.toList()
        }
    }

    data class ResolvedMethod(val call: UCallExpression, val method: PsiMethod)
    data class ResolvedConstructor(val call: UCallExpression, val constructor: PsiMethod, val clazz: PsiClass)

    val language: Language

    val project: Project

    /**
     * Checks if the file with the given [fileName] is supported.
     *
     * @param fileName the source file name.
     * @return true, if the file is supported by this converter, false otherwise.
     */
    fun isFileSupported(fileName: String): Boolean
    
    /**
     * Returns the converter priority. Might be positive, negative or 0 (Java's is 0).
     * UastConverter with the higher priority will be queried earlier.
     *
     * Priority is useful when a language N wraps its own elements (NElement) to, for example, Java's PsiElements,
     *  and Java resolves the reference to such wrapped PsiElements, not the original NElement.
     * In this case N implementation can handle such wrappers in UastConverter earlier than Java's converter,
     *  so N language converter will have a higher priority.
     */
    val priority: Int

    fun convertElement(element: PsiElement, parent: UElement?, requiredType: Class<out UElement>? = null): UElement?

    /**
     * Convert [element] to the [UElement] with the given parent.
     */
    fun convertElementWithParent(element: PsiElement, requiredType: Class<out UElement>?): UElement?

    fun getMethodCallExpression(
            element: PsiElement,
            containingClassFqName: String?,
            methodName: String
    ): ResolvedMethod?

    fun getConstructorCallExpression(
            element: PsiElement,
            fqName: String
    ) : ResolvedConstructor?

    fun getMethodBody(element: PsiMethod): UExpression? {
        if (element is UMethod) return element.uastBody
        return (convertElementWithParent(element, null) as? UMethod)?.uastBody
    }

    fun getInitializerBody(element: PsiClassInitializer): UExpression {
        if (element is UClassInitializer) return element.uastBody
        return (convertElementWithParent(element, null) as? UClassInitializer)?.uastBody ?: UastEmptyExpression
    }

    fun getInitializerBody(element: PsiVariable): UExpression? {
        if (element is UVariable) return element.uastInitializer
        return (convertElementWithParent(element, null) as? UVariable)?.uastInitializer
    }

    /**
     * Returns true if the expression value is used.
     * Do not rely on this property too much, its value can be approximate in some cases.
     */
    fun isExpressionValueUsed(element: UExpression): Boolean
}

inline fun <reified T : UElement> UastLanguagePlugin.convertOpt(element: PsiElement?, parent: UElement?): T? {
    if (element == null) return null
    return convertElement(element, parent) as? T
}

inline fun <reified T : UElement> UastLanguagePlugin.convert(element: PsiElement, parent: UElement?): T {
    return convertElement(element, parent, T::class.java) as T
}

inline fun <reified T : UElement> UastLanguagePlugin.convertWithParent(element: PsiElement?): T? {
    if (element == null) return null
    return convertElementWithParent(element, T::class.java) as? T
}