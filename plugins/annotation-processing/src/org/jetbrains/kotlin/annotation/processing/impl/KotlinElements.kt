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

package org.jetbrains.kotlin.annotation.processing.impl

import com.intellij.openapi.Disposable
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiSuperMethodUtil
import com.intellij.psi.util.TypeConversionUtil
import org.jetbrains.kotlin.java.model.JeAnnotationOwner
import org.jetbrains.kotlin.java.model.JeElement
import org.jetbrains.kotlin.java.model.JeName
import org.jetbrains.kotlin.java.model.elements.JeAnnotationMirror
import org.jetbrains.kotlin.java.model.elements.JeMethodExecutableElement
import org.jetbrains.kotlin.java.model.elements.JePackageElement
import org.jetbrains.kotlin.java.model.elements.JeTypeElement
import org.jetbrains.kotlin.java.model.internal.getTypeWithTypeParameters
import org.jetbrains.kotlin.java.model.util.disposeAll
import org.jetbrains.kotlin.java.model.util.toDisposable
import java.io.PrintWriter
import java.io.Writer
import javax.lang.model.element.*
import javax.lang.model.util.Elements

class KotlinElements(
        javaPsiFacade: JavaPsiFacade, 
        scope: GlobalSearchScope
) : Elements, Disposable {
    internal val javaPsiFacade = javaPsiFacade.toDisposable()
    internal val scope = scope.toDisposable()

    override fun dispose() = disposeAll(javaPsiFacade, scope)

    override fun hides(hider: Element, hidden: Element): Boolean {
        val hiderMethod = (hider as? JeMethodExecutableElement)?.psi ?: return false
        val hiddenMethod = (hidden as? JeMethodExecutableElement)?.psi ?: return false

        val hiderMethodClass = hiderMethod.containingClass ?: return false
        val hiddenMethodClass = hiddenMethod.containingClass ?: return false

        if (hiddenMethodClass.getTypeWithTypeParameters() !in hiderMethodClass.superTypes) return false
        
        return isSubSignature(hiderMethod, hiddenMethod)
    }

    override fun overrides(overrider: ExecutableElement, overridden: ExecutableElement, type: TypeElement): Boolean {
        overrider as? JeMethodExecutableElement ?: return false
        overridden as? JeMethodExecutableElement ?: return false
        type as? JeTypeElement ?: return false

        if (overrider.psi == overridden.psi) return false

        // if 'type' is a subtype of overrider's containing class
        if (type.psi.isSubclassOf(overridden.psi.containingClass) && type.psi.isSubclassOf(overrider.psi.containingClass)) {
            return isSubSignature(overrider.psi, overridden.psi)
        }
        
        return PsiSuperMethodUtil.isSuperMethod(overrider.psi, overridden.psi)
    }

    override fun getName(cs: CharSequence?) = JeName(cs?.toString())

    override fun getElementValuesWithDefaults(a: AnnotationMirror): Map<out ExecutableElement, AnnotationValue> {
        a as? JeAnnotationMirror ?: return emptyMap()
        return a.getAllElementValues()
    }

    override fun getBinaryName(type: TypeElement) = JeName((type as JeTypeElement).psi.qualifiedName)
    
    //TODO
    override fun getDocComment(e: Element?) = null

    override fun isDeprecated(e: Element?): Boolean {
        val deprecated = ((e as? JeElement)?.psi as? PsiDocCommentOwner)?.isDeprecated ?: false
        if (deprecated) return true
        return (e as? JeAnnotationOwner)?.psi?.modifierList?.findAnnotation("java.lang.Deprecated") != null
    }

    override fun getAllMembers(type: TypeElement) = (type as? JeTypeElement)?.getAllMembers() ?: emptyList()

    override fun printElements(w: Writer, vararg elements: Element) {
        val printWriter = PrintWriter(w)
        for (element in elements) {
            printWriter.println(element.simpleName.toString() + " (" + element::class.java.name + ")")
        }
    }

    override fun getPackageElement(name: CharSequence): PackageElement? {
        val psiPackage = javaPsiFacade().findPackage(name.toString()) ?: return null
        return JePackageElement(psiPackage)
    }

    override fun getTypeElement(name: CharSequence): TypeElement? {
        val psiClass = javaPsiFacade().findClass(name.toString(), scope()) ?: return null
        return JeTypeElement(psiClass)
    }

    override fun getConstantExpression(value: Any?) = Constants.format(value)

    override tailrec fun getPackageOf(element: Element): PackageElement? {
        if (element is PackageElement) return element
        val parent = element.enclosingElement ?: return null
        return getPackageOf(parent)
    }

    override fun getAllAnnotationMirrors(e: Element): List<AnnotationMirror> {
        val annotations = (e as? JeElement)?.annotationMirrors?.toMutableList() ?: mutableListOf()
        
        if (e is JeTypeElement) {
            var parent = e.psi.superClass
            while (parent != null) {
                val parentAnnotations = parent.modifierList?.annotations
                if (parentAnnotations == null) {
                    parent = parent.superClass
                    continue
                }
                
                for (parentAnnotation in parentAnnotations) {
                    val annotationClass = parentAnnotation.nameReferenceElement?.resolve() as? PsiClass ?: continue
                    annotationClass.modifierList?.findAnnotation("java.lang.annotation.Inherited") ?: continue
                    annotations += JeAnnotationMirror(parentAnnotation)
                }
                
                parent = parent.superClass
            }
        }
        
        return annotations
    }

    override fun isFunctionalInterface(type: TypeElement): Boolean {
        val jeTypeElement = type as? JeTypeElement ?: return false
        if (!jeTypeElement.psi.isInterface) return false
        if (jeTypeElement.psi.allMethods.count { it.hasModifierProperty(PsiModifier.ABSTRACT) } != 1) return false
        return true
    }
}

private fun PsiClass.isSubclassOf(other: PsiClass?): Boolean {
    if (other == null) return false
    return TypeConversionUtil.isAssignable(other.getTypeWithTypeParameters(), this.getTypeWithTypeParameters(), false)
}

private fun isSubSignature(childMethod: PsiMethod, superMethod: PsiMethod): Boolean {
    if (childMethod.name != superMethod.name) return false
    if (childMethod.parameterList.parametersCount != superMethod.parameterList.parametersCount) return false

    if (childMethod.returnType != superMethod.returnType) return false
    for (i in 0..childMethod.parameterList.parametersCount - 1) {
        if (childMethod.parameterList.parameters[i].type != superMethod.parameterList.parameters[i].type) return false
    }
    
    return true
}

object Constants {
    fun format(value: Any?): String {
        return when (value) {
            is Byte -> formatByte((value as Byte?)!!)
            is Short -> formatShort((value as Short?)!!)
            is Long -> formatLong((value as Long?)!!)
            is Float -> formatFloat((value as Float?)!!)
            is Double -> formatDouble((value as Double?)!!)
            is Char -> formatChar(value)
            is String -> formatString(value)
            is Int, is Boolean -> value.toString()
            else -> throw IllegalArgumentException(
                    "Argument is not a primitive type or a string; it " +
                    (if (value == null) "is a null value." else "has class " + value::class.java.name) + ".")
        }
    }

    private fun formatByte(b: Byte) = String.format("(byte)0x%02x", b)
    private fun formatShort(s: Short) = String.format("(short)%d", s)
    private fun formatLong(lng: Long) = "${lng}L"
    private fun formatChar(c: Char) = '\'' + quote(c) + '\''
    private fun formatString(s: String) = '"' + quote(s) + '"'
    
    private fun formatFloat(f: Float): String = when {
        java.lang.Float.isNaN(f) -> "0.0f/0.0f"
        java.lang.Float.isInfinite(f) -> if (f < 0) "-1.0f/0.0f" else "1.0f/0.0f"
        else -> "${f}f"
    }

    private fun formatDouble(d: Double): String = when {
        java.lang.Double.isNaN(d) -> "0.0/0.0"
        java.lang.Double.isInfinite(d) -> if (d < 0) "-1.0/0.0" else "1.0/0.0"
        else -> d.toString()
    }

    fun quote(ch: Char): String = when (ch) {
        '\b' -> "\\b"
        '\n' -> "\\n"
        '\r' -> "\\r"
        '\t' -> "\\t"
        '\'' -> "\\'"
        '\"' -> "\\\""
        '\\' -> "\\\\"
        else -> if (isPrintableAscii(ch)) ch.toString() else String.format("\\u%04x", ch.toInt())
    }

    fun quote(s: String): String {
        val buf = StringBuilder()
        for (i in 0..s.length - 1) {
            buf.append(quote(s[i]))
        }
        return buf.toString()
    }

    private fun isPrintableAscii(ch: Char) = ch >= ' ' && ch <= '~'
}