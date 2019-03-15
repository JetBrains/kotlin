/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.jvm

import com.intellij.lang.jvm.JvmClass
import com.intellij.lang.jvm.JvmElement
import com.intellij.lang.jvm.JvmMethod
import com.intellij.lang.jvm.JvmParameter
import com.intellij.lang.jvm.source.JvmDeclarationSearch
import com.intellij.lang.jvm.source.JvmDeclarationSearcher
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementVisitor
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.psi.KtFile
import kotlin.reflect.KClass

class KotlinJvmDeclarationSearcherTest : KotlinLightCodeInsightFixtureTestCase() {
    override fun getProjectDescriptor() = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    fun testClassWithFieldsAndMethods() = assertDeclares(
        """

            class SomeClass(val field: String) {

                private var privateField: Int = 1

                lateinit var anotherField:String

                constructor(i: Int): this(i.toString()){}

                fun foo():Unit {}

                @JvmOverloads
                fun bar(a: Long = 1L, b: Int = 2){}

            }

        """,
        JvmDeclared("class SomeClass", JvmClass::class, JvmMethod::class),
        JvmDeclared("(val field: String)", JvmMethod::class),
        JvmDeclared("private var privateField", com.intellij.lang.jvm.JvmField::class),
        JvmDeclared("lateinit var anotherField", JvmMethod::class, JvmMethod::class, com.intellij.lang.jvm.JvmField::class),
        JvmDeclared("val field: String", JvmParameter::class, JvmMethod::class, com.intellij.lang.jvm.JvmField::class),
        JvmDeclared("constructor(i: Int)", JvmMethod::class),
        JvmDeclared("i: Int", JvmParameter::class),
        JvmDeclared("a: Long", JvmParameter::class, JvmParameter::class),
        JvmDeclared("b: Int", JvmParameter::class),
        JvmDeclared("fun foo()", JvmMethod::class),
        JvmDeclared("fun bar", JvmMethod::class, JvmMethod::class, JvmMethod::class)
    )


    fun testLocalObject() = assertDeclares(
        """ /* Facade class */
            open class SomeClass(field: String)

            fun foo(){
               val obj1 = object : SomeClass("foo")

               object obj2 {
                   fun bar(){}
               }
            }

        """,
        JvmDeclared("class SomeClass", JvmClass::class, JvmMethod::class),
        JvmDeclared("Facade class", JvmClass::class),
        JvmDeclared("(field: String)", JvmMethod::class),
        JvmDeclared("field: String", JvmParameter::class),
        JvmDeclared("fun foo()", JvmMethod::class),
        JvmDeclared("object : SomeClass(\"foo\")", JvmClass::class),
        JvmDeclared("object obj2", JvmClass::class),
        JvmDeclared("fun bar", JvmMethod::class)
    )


    fun testClassDeclaration() = assertElementsByIdentifier("""
            class Some<caret>Class(val field: String)
        """, { it is JvmClass }, { it is com.intellij.lang.jvm.JvmMethod && it.isConstructor })


    fun testLocalObjectDeclaration() = assertElementsByIdentifier("""
            val e = obje<caret>ct {}
        """, { it is JvmClass })


    fun testClassDeclarationWithConstructor() = assertElementsByIdentifier("""
            class Some<caret>Class constructor(val field: String)
        """, { it is JvmClass })


    fun testPrimaryConstructorByConstructorKeyword() = assertElementsByIdentifier("""
            class SomeClass constr<caret>uctor(val field: String)
        """, { it is JvmMethod && it.isConstructor })


    private fun assertElementsByIdentifier(text: String, vararg matches: (JvmElement) -> Boolean) {
        myFixture.configureByText("Declaraions.kt", text.trimIndent())
        val elementsByIdentifier = JvmDeclarationSearch.getElementsByIdentifier(myFixture.file.findElementAt(myFixture.caretOffset)!!)
        assertMatches(elementsByIdentifier.toList(), *matches)
    }


    private fun assertDeclares(text: String, vararg declarations: JvmDeclared) {
        val file = myFixture.addFileToProject("Declaraions.kt", text.trimIndent()) as KtFile
        assertMatches(collectJvmDeclarations(file).entries, *declarations)
    }

    private fun collectJvmDeclarations(file: KtFile): MutableMap<PsiElement, List<JvmElement>> {
        val declarationSearcher = JvmDeclarationSearcher.EP.forLanguage(KotlinLanguage.INSTANCE)!!

        val map = mutableMapOf<PsiElement, List<JvmElement>>()

        file.accept(object : PsiRecursiveElementVisitor() {

            override fun visitElement(element: PsiElement) {
                val declarations = declarationSearcher.findDeclarations(element)
                if (declarations.isNotEmpty()) {
                    map[element] = declarations.toList()
                }
                super.visitElement(element)
            }
        })
        return map
    }

}

private class JvmDeclared(val textToContain: String, vararg jvmClasses: KClass<out JvmElement>) :
    Function1<Map.Entry<PsiElement, List<JvmElement>>, Boolean> {
    private val jvmClasses = jvmClasses.toList()

    override fun invoke(p1: Map.Entry<PsiElement, List<JvmElement>>): Boolean {
        val (psi, jvmElements) = p1
        if (!psi.text.contains(textToContain)) return false

        return matchElementsToConditions(jvmElements, jvmClasses.map { { value: JvmElement -> it.isInstance(value) } }).succeed
    }

    override fun toString(): String = "JvmDeclaration contains text '$textToContain' and produces $jvmClasses"
}

fun <T> assertMatches(elements: Collection<T>, vararg conditions: (T) -> Boolean) {
    val matchResult = matchElementsToConditions(elements, conditions.toList())
    when (matchResult) {
        is MatchResult.UnmatchedCondition ->
            throw AssertionError("no one matches the ${matchResult.condition}, elements = ${elements.joinToString { it.toString() }}")
        is MatchResult.UnmatchedElements ->
            throw AssertionError("elements ${matchResult.elements.joinToString { it.toString() }} wasn't matched by any condition")
    }
}

private fun <T> matchElementsToConditions(elements: Collection<T>, conditions: List<(T) -> Boolean>): MatchResult<T> {
    val checkList = conditions.toMutableList()
    val elementsToCheck = elements.toMutableList()

    while (checkList.isNotEmpty()) {
        val condition = checkList.removeAt(0)
        val matched = elementsToCheck.find { condition(it) }
                ?: return MatchResult.UnmatchedCondition(condition)
        if (!elementsToCheck.remove(matched))
            throw IllegalStateException("cant remove matched element: $matched")
    }
    if (elementsToCheck.isEmpty())
        return MatchResult.Matched
    return MatchResult.UnmatchedElements(elementsToCheck)
}

private sealed class MatchResult<out T>(val succeed: Boolean) {
    object Matched : MatchResult<Nothing>(true)
    class UnmatchedCondition<T>(val condition: (T) -> Boolean) : MatchResult<T>(false)
    class UnmatchedElements<T>(val elements: List<T>) : MatchResult<T>(false)
}