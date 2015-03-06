/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.stubs

import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.stubs.elements.JetFileStubBuilder
import com.intellij.psi.stubs.StubElement
import org.jetbrains.kotlin.psi.stubs.elements.JetStubElementTypes
import org.junit.Assert
import org.jetbrains.kotlin.psi.JetPackageDirective
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub
import org.jetbrains.kotlin.psi.JetImportList
import org.jetbrains.kotlin.psi.JetNamedFunction
import org.jetbrains.kotlin.psi.stubs.KotlinFunctionStub
import org.jetbrains.kotlin.psi.JetTypeReference
import org.jetbrains.kotlin.psi.stubs.KotlinClassStub
import org.jetbrains.kotlin.psi.JetClass
import org.jetbrains.kotlin.psi.stubs.KotlinObjectStub
import org.jetbrains.kotlin.psi.JetObjectDeclaration
import org.jetbrains.kotlin.psi.JetProperty
import org.jetbrains.kotlin.psi.stubs.KotlinPropertyStub
import kotlin.test.assertEquals
import org.jetbrains.kotlin.psi.JetClassBody
import org.jetbrains.kotlin.psi.JetClassInitializer
import org.jetbrains.kotlin.psi.debugText.getDebugText

public class DebugTextByStubTest : LightCodeInsightFixtureTestCase() {
    private fun createFileAndStubTree(text: String): Pair<JetFile, StubElement<*>> {
        val file = myFixture.configureByText("test.kt", text) as JetFile
        val stub = JetFileStubBuilder().buildStubTree(file)!!
        return Pair(file, stub)
    }

    private fun createStubTree(text: String) = createFileAndStubTree(text).second

    fun packageDirective(text: String) {
        val (file, tree) = createFileAndStubTree(text)
        val packageDirective = tree.findChildStubByType(JetStubElementTypes.PACKAGE_DIRECTIVE)
        val psi = JetPackageDirective(packageDirective as KotlinPlaceHolderStub)
        Assert.assertEquals(file.getPackageDirective()!!.getText(), psi.getDebugText())
    }

    fun function(text: String) {
        val (file, tree) = createFileAndStubTree(text)
        val function = tree.findChildStubByType(JetStubElementTypes.FUNCTION)
        val psi = JetNamedFunction(function as KotlinFunctionStub)
        Assert.assertEquals("STUB: " + file.findChildByClass(javaClass<JetNamedFunction>())!!.getText(), psi.getDebugText())
    }

    fun typeReference(text: String) {
        val (file, tree) = createFileAndStubTree("fun foo(i: $text)")
        val function = tree.findChildStubByType(JetStubElementTypes.FUNCTION)!!
        val parameterList = function.findChildStubByType(JetStubElementTypes.VALUE_PARAMETER_LIST)!!
        val valueParameter = parameterList.findChildStubByType(JetStubElementTypes.VALUE_PARAMETER)!!
        val typeReferenceStub = valueParameter.findChildStubByType(JetStubElementTypes.TYPE_REFERENCE)
        val psiFromStub = JetTypeReference(typeReferenceStub as KotlinPlaceHolderStub)
        val typeReferenceByPsi = file.findChildByClass(javaClass<JetNamedFunction>())!!.getValueParameters()[0].getTypeReference()
        Assert.assertEquals(typeReferenceByPsi!!.getText(), psiFromStub.getDebugText())
    }

    fun clazz(text: String, expectedText: String? = null) {
        val (file, tree) = createFileAndStubTree(text)
        val clazz = tree.findChildStubByType(JetStubElementTypes.CLASS)!!
        val psiFromStub = JetClass(clazz as KotlinClassStub)
        val classByPsi = file.findChildByClass(javaClass<JetClass>())
        val toCheckAgainst = "STUB: " + (expectedText ?: classByPsi!!.getText())
        Assert.assertEquals(toCheckAgainst, psiFromStub.getDebugText())
        if (expectedText != null) {
            Assert.assertNotEquals("Expected text should not be specified", classByPsi.getDebugText(), psiFromStub.getDebugText())
        }
    }

    fun obj(text: String, expectedText: String? = null) {
        val (file, tree) = createFileAndStubTree(text)
        val obj = tree.findChildStubByType(JetStubElementTypes.OBJECT_DECLARATION)!!
        val psiFromStub = JetObjectDeclaration(obj as KotlinObjectStub)
        val objectByPsi = file.findChildByClass(javaClass<JetObjectDeclaration>())
        val toCheckAgainst = "STUB: " + (expectedText ?: objectByPsi!!.getText())
        Assert.assertEquals(toCheckAgainst, psiFromStub.getDebugText())
    }

    fun property(text: String, expectedText: String? = null) {
        val (file, tree) = createFileAndStubTree(text)
        val property = tree.findChildStubByType(JetStubElementTypes.PROPERTY)!!
        val psiFromStub = JetProperty(property as KotlinPropertyStub)
        val propertyByPsi = file.findChildByClass(javaClass<JetProperty>())
        val toCheckAgainst = "STUB: " + (expectedText ?: propertyByPsi!!.getText())
        Assert.assertEquals(toCheckAgainst, psiFromStub.getDebugText())
    }

    fun importList(text: String) {
        val (file, tree) = createFileAndStubTree(text)
        val importList = tree.findChildStubByType(JetStubElementTypes.IMPORT_LIST)
        val psi = JetImportList(importList as KotlinPlaceHolderStub)
        Assert.assertEquals(file.getImportList()!!.getText(), psi.getDebugText())
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
        function("fun foo<T>()")
        function("fun foo<T, G>()")
        function("fun foo(a: Int, b: String)")
        function("fun Int.foo()")
        function("fun foo(): String")
        function("fun <T> T.foo(b: T): List<T>")
        function("fun <T, G> f() where T : G")
        function("fun <T, G> f() where T : G, G : T")
        function("fun <T, G> f() where class object T : G")
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
        val classBody = tree.findChildStubByType(JetStubElementTypes.CLASS)!!.findChildStubByType(JetStubElementTypes.CLASS_BODY)
        assertEquals("class body for STUB: class A", JetClassBody(classBody as KotlinPlaceHolderStub).getDebugText())
    }

    fun testClassInitializer() {
        val tree = createStubTree("class A {\n {} }")
        val initializer = tree.findChildStubByType(JetStubElementTypes.CLASS)!!.findChildStubByType(JetStubElementTypes.CLASS_BODY)!!
                .findChildStubByType(JetStubElementTypes.ANONYMOUS_INITIALIZER)
        assertEquals("initializer in STUB: class A", JetClassInitializer(initializer as KotlinPlaceHolderStub).getDebugText())
    }

    fun testClassObject() {
        val tree = createStubTree("class A { class object Def {} }")
        val defaultObject = tree.findChildStubByType(JetStubElementTypes.CLASS)!!.findChildStubByType(JetStubElementTypes.CLASS_BODY)!!
                .findChildStubByType(JetStubElementTypes.OBJECT_DECLARATION)
        assertEquals("STUB: class object Def", JetObjectDeclaration(defaultObject as KotlinObjectStub).getDebugText())
    }

    fun testPropertyAccessors() {
        val tree = createStubTree("var c: Int\nget() = 3\nset(i: Int) {}")
        val propertyStub = tree.findChildStubByType(JetStubElementTypes.PROPERTY)!!
        val accessors = propertyStub.getChildrenByType(JetStubElementTypes.PROPERTY_ACCESSOR, JetStubElementTypes.PROPERTY_ACCESSOR.getArrayFactory())!!
        assertEquals("getter for STUB: var c: Int", accessors[0].getDebugText())
        assertEquals("setter for STUB: var c: Int", accessors[1].getDebugText())
    }

    fun testEnumEntry() {
        val tree = createStubTree("enum class Enum { E1 E2: Enum() E3: Enum(1, 2)}")
        val enumClass = tree.findChildStubByType(JetStubElementTypes.CLASS)!!.findChildStubByType(JetStubElementTypes.CLASS_BODY)!!
        val entries = enumClass.getChildrenByType(JetStubElementTypes.ENUM_ENTRY, JetStubElementTypes.ENUM_ENTRY.getArrayFactory())!!
        assertEquals("STUB: enum entry E1", entries[0].getDebugText())
        assertEquals("STUB: enum entry E2 : Enum", entries[1].getDebugText())
        assertEquals("STUB: enum entry E3 : Enum", entries[2].getDebugText())
    }
}
