/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.stubs

import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.stubs.elements.KtFileStubBuilder
import com.intellij.psi.stubs.StubElement
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import org.junit.Assert
import org.jetbrains.kotlin.psi.KtPackageDirective
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub
import org.jetbrains.kotlin.psi.KtImportList
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.stubs.KotlinFunctionStub
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.stubs.KotlinClassStub
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.stubs.KotlinObjectStub
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.stubs.KotlinPropertyStub
import kotlin.test.assertEquals
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtClassInitializer
import org.jetbrains.kotlin.psi.debugText.getDebugText

class DebugTextByStubTest : LightCodeInsightFixtureTestCase() {
    private fun createFileAndStubTree(text: String): Pair<KtFile, StubElement<*>> {
        val file = myFixture.configureByText("test.kt", text) as KtFile
        val stub = KtFileStubBuilder().buildStubTree(file)!!
        return Pair(file, stub)
    }

    private fun createStubTree(text: String) = createFileAndStubTree(text).second

    fun packageDirective(text: String) {
        val (file, tree) = createFileAndStubTree(text)
        val packageDirective = tree.findChildStubByType(KtStubElementTypes.PACKAGE_DIRECTIVE)
        val psi = KtPackageDirective(packageDirective as KotlinPlaceHolderStub)
        Assert.assertEquals(file.packageDirective!!.text, psi.getDebugText())
    }

    fun function(text: String) {
        val (file, tree) = createFileAndStubTree(text)
        val function = tree.findChildStubByType(KtStubElementTypes.FUNCTION)
        val psi = KtNamedFunction(function as KotlinFunctionStub)
        Assert.assertEquals("STUB: " + file.findChildByClass(KtNamedFunction::class.java)!!.text, psi.getDebugText())
    }

    fun typeReference(text: String) {
        val (file, tree) = createFileAndStubTree("fun foo(i: $text)")
        val function = tree.findChildStubByType(KtStubElementTypes.FUNCTION)!!
        val parameterList = function.findChildStubByType(KtStubElementTypes.VALUE_PARAMETER_LIST)!!
        val valueParameter = parameterList.findChildStubByType(KtStubElementTypes.VALUE_PARAMETER)!!
        val typeReferenceStub = valueParameter.findChildStubByType(KtStubElementTypes.TYPE_REFERENCE)
        val psiFromStub = KtTypeReference(typeReferenceStub as KotlinPlaceHolderStub)
        val typeReferenceByPsi = file.findChildByClass(KtNamedFunction::class.java)!!.valueParameters[0].typeReference
        Assert.assertEquals(typeReferenceByPsi!!.text, psiFromStub.getDebugText())
    }

    fun clazz(text: String, expectedText: String? = null) {
        val (file, tree) = createFileAndStubTree(text)
        val clazz = tree.findChildStubByType(KtStubElementTypes.CLASS)!!
        val psiFromStub = KtClass(clazz as KotlinClassStub)
        val classByPsi = file.findChildByClass(KtClass::class.java)
        val toCheckAgainst = "STUB: " + (expectedText ?: classByPsi!!.text)
        Assert.assertEquals(toCheckAgainst, psiFromStub.getDebugText())
        if (expectedText != null) {
            Assert.assertNotEquals("Expected text should not be specified", classByPsi!!.getDebugText(), psiFromStub.getDebugText())
        }
    }

    fun obj(text: String, expectedText: String? = null) {
        val (file, tree) = createFileAndStubTree(text)
        val obj = tree.findChildStubByType(KtStubElementTypes.OBJECT_DECLARATION)!!
        val psiFromStub = KtObjectDeclaration(obj as KotlinObjectStub)
        val objectByPsi = file.findChildByClass(KtObjectDeclaration::class.java)
        val toCheckAgainst = "STUB: " + (expectedText ?: objectByPsi!!.text)
        Assert.assertEquals(toCheckAgainst, psiFromStub.getDebugText())
    }

    fun property(text: String, expectedText: String? = null) {
        val (file, tree) = createFileAndStubTree(text)
        val property = tree.findChildStubByType(KtStubElementTypes.PROPERTY)!!
        val psiFromStub = KtProperty(property as KotlinPropertyStub)
        val propertyByPsi = file.findChildByClass(KtProperty::class.java)
        val toCheckAgainst = "STUB: " + (expectedText ?: propertyByPsi!!.text)
        Assert.assertEquals(toCheckAgainst, psiFromStub.getDebugText())
    }

    fun importList(text: String) {
        val (file, tree) = createFileAndStubTree(text)
        val importList = tree.findChildStubByType(KtStubElementTypes.IMPORT_LIST)
        val psi = KtImportList(importList as KotlinPlaceHolderStub)
        Assert.assertEquals(file.importList!!.text, psi.getDebugText())
    }

    fun testPackageDirective() {
        packageDirective("package a.b.c")
        packageDirective("")
        packageDirective("package b")
    }

    fun testImportList() {
        importList("import a\nimport b.c.d")
        importList("import a.*")
        importList("import a.c as Alias")
    }

    fun testFunction() {
        function("fun foo()")
        function("fun <T> foo()")
        function("fun <T> foo()")
        function("fun <T, G> foo()")
        function("fun foo(a: Int, b: String)")
        function("fun Int.foo()")
        function("fun foo(): String")
        function("fun <T> T.foo(b: T): List<T>")
        function("fun <T, G> f() where T : G")
        function("fun <T, G> f() where T : G, G : T")
        function("private final fun f()")
    }

    fun testTypeReference() {
        typeReference("T")
        typeReference("T<G>")
        typeReference("T<G, H>")
        typeReference("T<in G>")
        typeReference("T<out G>")
        typeReference("T<*>")
        typeReference("T<*, in G>")
        typeReference("T?")
        typeReference("T<G?>")
        typeReference("() -> T")
        typeReference("(G?, H) -> T?")
        typeReference("L.(G?, H) -> T?")
        typeReference("L?.(G?, H) -> T?")
    }

    fun testClass() {
        clazz("class A")
        clazz("open private class A")
        clazz("public class private A")
        clazz("class A()")
        clazz("class A() : B()", expectedText = "class A() : B")
        clazz("class A() : B<T>")
        clazz("class A() : B(), C()", expectedText = "class A() : B, C")
        clazz("class A() : B by g", expectedText = "class A() : B")
        clazz("class A() : B by g, C(), T", expectedText = "class A() : B, C, T")
        clazz("class A(i: Int, g: String)")
        clazz("class A(val i: Int, var g: String)")
    }

    fun testObject() {
        obj("object Foo")
        obj("public final object Foo")
        obj("object Foo : A()", expectedText = "object Foo : A")
        obj("object Foo : A by foo", expectedText = "object Foo : A")
        obj("object Foo : A, T, C by g, B()", expectedText = "object Foo : A, T, C, B")
    }

    fun testProperty() {
        property("val c: Int")
        property("var c: Int")
        property("var : Int")
        property("private final var c: Int")
        property("val g")
        property("val g = 3", expectedText = "val g")
        property("val g by z", expectedText = "val g")
        property("val g: Int by z", expectedText = "val g: Int")
    }

    fun testClassBody() {
        val tree = createStubTree("class A {\n {} fun f(): Int val c: Int}")
        val classBody = tree.findChildStubByType(KtStubElementTypes.CLASS)!!.findChildStubByType(KtStubElementTypes.CLASS_BODY)
        assertEquals("class body for STUB: class A", KtClassBody(classBody as KotlinPlaceHolderStub).getDebugText())
    }

    fun testClassInitializer() {
        val tree = createStubTree("class A {\n init {} }")
        val initializer = tree.findChildStubByType(KtStubElementTypes.CLASS)!!.findChildStubByType(KtStubElementTypes.CLASS_BODY)!!
                .findChildStubByType(KtStubElementTypes.CLASS_INITIALIZER)
        assertEquals("initializer in STUB: class A", KtClassInitializer(initializer as KotlinPlaceHolderStub).getDebugText())
    }

    fun testClassObject() {
        val tree = createStubTree("class A { companion object Def {} }")
        val companionObject = tree.findChildStubByType(KtStubElementTypes.CLASS)!!.findChildStubByType(KtStubElementTypes.CLASS_BODY)!!
                .findChildStubByType(KtStubElementTypes.OBJECT_DECLARATION)
        assertEquals("STUB: companion object Def", KtObjectDeclaration(companionObject as KotlinObjectStub).getDebugText())
    }

    fun testPropertyAccessors() {
        val tree = createStubTree("var c: Int\nget() = 3\nset(i: Int) {}")
        val propertyStub = tree.findChildStubByType(KtStubElementTypes.PROPERTY)!!
        val accessors = propertyStub.getChildrenByType(KtStubElementTypes.PROPERTY_ACCESSOR, KtStubElementTypes.PROPERTY_ACCESSOR.arrayFactory)
        assertEquals("getter for STUB: var c: Int", accessors[0].getDebugText())
        assertEquals("setter for STUB: var c: Int", accessors[1].getDebugText())
    }

    fun testEnumEntry() {
        val tree = createStubTree("enum class Enum { E1, E2(), E3(1, 2)}")
        val enumClass = tree.findChildStubByType(KtStubElementTypes.CLASS)!!.findChildStubByType(KtStubElementTypes.CLASS_BODY)!!
        val entries = enumClass.getChildrenByType(KtStubElementTypes.ENUM_ENTRY, KtStubElementTypes.ENUM_ENTRY.arrayFactory)
        assertEquals("STUB: enum entry E1", entries[0].getDebugText())
        assertEquals("STUB: enum entry E2 : Enum", entries[1].getDebugText())
        assertEquals("STUB: enum entry E3 : Enum", entries[2].getDebugText())
    }
}
