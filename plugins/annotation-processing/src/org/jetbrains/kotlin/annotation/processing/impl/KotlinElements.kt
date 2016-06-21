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

import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiSuperMethodUtil
import com.intellij.psi.util.PsiTypesUtil
import org.jetbrains.kotlin.java.model.JeAnnotationOwner
import org.jetbrains.kotlin.java.model.JeElement
import org.jetbrains.kotlin.java.model.JeName
import org.jetbrains.kotlin.java.model.elements.JeAnnotationMirror
import org.jetbrains.kotlin.java.model.elements.JeMethodExecutableElement
import org.jetbrains.kotlin.java.model.elements.JePackageElement
import org.jetbrains.kotlin.java.model.elements.JeTypeElement
import java.io.PrintWriter
import java.io.Writer
import javax.lang.model.element.*
import javax.lang.model.util.Elements

class KotlinElements(val javaPsiFacade: JavaPsiFacade, val scope: GlobalSearchScope) : Elements {
    override fun hides(hider: Element, hidden: Element): Boolean {
        val hiderMethod = (hider as? JeMethodExecutableElement)?.psi ?: return false
        val hiddenMethod = (hidden as? JeMethodExecutableElement)?.psi ?: return false
        
        if (hiderMethod.name != hiddenMethod.name) return false
        if (hiderMethod.parameterList.parametersCount != hiddenMethod.parameterList.parametersCount) return false
        
        val hiderMethodClass = hiderMethod.containingClass ?: return false
        val hiddenMethodClass = hiddenMethod.containingClass ?: return false
        
        if (PsiTypesUtil.getClassType(hiddenMethodClass) !in hiderMethodClass.superTypes) return false

        if (hiderMethod.returnType != hiddenMethod.returnType) return false
        for (i in 0..hiderMethod.parameterList.parametersCount - 1) {
            if (hiderMethod.parameterList.parameters[i].type != hiddenMethod.parameterList.parameters[i].type) return false
        }
        
        return true
    }

    override fun overrides(overrider: ExecutableElement, overridden: ExecutableElement, type: TypeElement): Boolean {
        overrider as? JeMethodExecutableElement ?: return false
        overridden as? JeMethodExecutableElement ?: return false
        
        return PsiSuperMethodUtil.isSuperMethod(overrider.psi, overridden.psi)
    }

    override fun getName(cs: CharSequence?) = JeName(cs?.toString())

    override fun getElementValuesWithDefaults(a: AnnotationMirror): Map<out ExecutableElement, AnnotationValue> {
        a as? JeAnnotationMirror ?: return emptyMap()
        return a.getAllElementValues()
    }

    override fun getBinaryName(type: TypeElement) = JeName((type as JeTypeElement).psi.qualifiedName)

    override fun getDocComment(e: Element?) = ""

    override fun isDeprecated(e: Element?): Boolean {
        return (e as? JeAnnotationOwner)?.annotationOwner?.findAnnotation("java.lang.Deprecated") != null
    }

    override fun getAllMembers(type: TypeElement) = (type as? JeTypeElement)?.getAllMembers() ?: emptyList()

    override fun printElements(w: Writer, vararg elements: Element) {
        val printWriter = PrintWriter(w)
        for (element in elements) {
            printWriter.println(element.simpleName.toString() + " (" + element.javaClass.name + ")")
        }
    }

    override fun getPackageElement(name: CharSequence): PackageElement? {
        val psiPackage = javaPsiFacade.findPackage(name.toString()) ?: return null
        return JePackageElement(psiPackage)
    }

    override fun getTypeElement(name: CharSequence): TypeElement? {
        val psiClass = javaPsiFacade.findClass(name.toString(), scope) ?: return null
        return JeTypeElement(psiClass)
    }

    override fun getConstantExpression(value: Any?) = Constants.format(value)

    override tailrec fun getPackageOf(element: Element): PackageElement? {
        if (element is PackageElement) return element
        val parent = element.enclosingElement ?: return null
        return getPackageOf(parent)
    }

    override fun getAllAnnotationMirrors(e: Element): List<AnnotationMirror> {
        val annotations = (e as? JeElement)?.annotationMirrors?.toMutableList() ?: return emptyList()
        
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
        if (jeTypeElement.psi.allMethods.size != 1) return false
        return true
    }
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
                    (if (value == null) "is a null value." else "has class " + value.javaClass.name) + ".")
        }
    }

    private fun formatByte(b: Byte) = String.format("(byte)0x%02x", b)
    private fun formatShort(s: Short) = String.format("(short)%d", s)
    private fun formatLong(lng: Long) = "${lng}L"
    private fun formatChar(c: Char) = '\'' + quote(c) + '\''
    private fun formatString(s: String) = '"' + quote(s) + '"'
    
    private fun formatFloat(f: Float): String {
        if (java.lang.Float.isNaN(f))
            return "0.0f/0.0f"
        else if (java.lang.Float.isInfinite(f))
            return if (f < 0) "-1.0f/0.0f" else "1.0f/0.0f"
        else
            return "${f}f"
    }

    private fun formatDouble(d: Double): String {
        if (java.lang.Double.isNaN(d))
            return "0.0/0.0"
        else if (java.lang.Double.isInfinite(d))
            return if (d < 0) "-1.0/0.0" else "1.0/0.0"
        else
            return d.toString()
    }

    fun quote(ch: Char): String {
        return when (ch) {
            '\b' -> "\\b"
            '\n' -> "\\n"
            '\r' -> "\\r"
            '\t' -> "\\t"
            '\'' -> "\\'"
            '\"' -> "\\\""
            '\\' -> "\\\\"
            else -> if (isPrintableAscii(ch)) ch.toString() else String.format("\\u%04x", ch.toInt())
        }
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