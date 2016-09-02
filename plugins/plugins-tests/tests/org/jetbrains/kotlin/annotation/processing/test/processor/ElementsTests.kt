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

package org.jetbrains.kotlin.annotation.processing.test.processor

import org.jetbrains.kotlin.annotation.processing.impl.KotlinElements
import org.jetbrains.kotlin.java.model.elements.JeTypeElement
import org.jetbrains.kotlin.java.model.types.JeArrayType
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement

class ElementsTests : AbstractProcessorTest() {
    override val testDataDir = "plugins/annotation-processing/testData/elements"
    
    fun testOverrides() = test("Overrides", "*") { set, roundEnv, env -> 
        val (parent, child, childChild) = Triple(env.findClass("Parent"), env.findClass("Child"), env.findClass("ChildOfChild"))
        
        val parentA = parent.findMethod("a")
        
        val childA = child.findMethod("a")
        val childAString = child.findMethod("a", "java.lang.String")
        val childB = child.findMethod("b")
        
        val childChildA = childChild.findMethod("a")
        val childChildACharSequence = childChild.findMethod("a", "java.lang.CharSequence")
        val childChildB = childChild.findMethod("b")
        
        fun ExecutableElement.assertOverrides(overridden: ExecutableElement, result: Boolean) {
            assertEquals(result, env.elementUtils.overrides(this, overridden, enclosingElement as JeTypeElement))
        }

        parentA.assertOverrides(parentA, false)
        childA.assertOverrides(parentA, true)
        childAString.assertOverrides(childA, false)
        childB.assertOverrides(parentA, false)
        childChildA.assertOverrides(parentA, true)
        childChildA.assertOverrides(childA, true)
        childChildB.assertOverrides(childB, true)
        childChildACharSequence.assertOverrides(childA, false)
    }
    
    fun testOverrides2() = test("Overrides2", "*") { set, roundEnv, env ->
        val classes = listOf(env.findClass("Intf"), env.findClass("A"), env.findClass("B"))
        val (intf, a, b) = classes.map { it.findMethod("a") }

        fun ExecutableElement.assertOverrides(overridden: ExecutableElement, context: TypeElement, result: Boolean) {
            assertEquals(result, env.elementUtils.overrides(this, overridden, context))
        }

        a.assertOverrides(intf, classes[1], false)
        b.assertOverrides(a, classes[2], true)
        b.assertOverrides(intf, classes[2], true)
        a.assertOverrides(intf, classes[2], true)
    }
    
    fun testIsFunctionalInterface() = test("IsFunctionalInterface", "*") { set, roundEnv, env ->
        with (env.elementUtils as KotlinElements) {
            fun assertIsFunctionalInterface(fqName: String, isIntf: Boolean) {
                assertEquals(isIntf, isFunctionalInterface(env.findClass(fqName)))
            }
            
            assertIsFunctionalInterface("A", false)
            assertIsFunctionalInterface("A.AA", true)
            assertIsFunctionalInterface("B", false)
            assertIsFunctionalInterface("C", true)
            assertIsFunctionalInterface("D", false)
            assertIsFunctionalInterface("E", true)
            assertIsFunctionalInterface("F", false)
        }
    }
    
    fun testIsDeprecated() = test("IsDeprecated", "*") { set, roundEnv, env -> 
        with (env.elementUtils) {
            assertEquals(true, isDeprecated(env.findClass("Depr")))
            assertEquals(false, isDeprecated(env.findClass("NoDepr")))
            assertEquals(true, isDeprecated(env.findClass("JavaDepr")))
        }
    }
    
    fun testGetElementValuesWithDefaults() = test("GetElementValuesWithDefaults", "Anno") { set, roundEnv, env ->
        val (a, b, c, d) = listOf(env.findClass("A"), env.findClass("B"), env.findClass("C"), env.findClass("D"))
        fun getValues(e: JeTypeElement) = env.elementUtils
                .getElementValuesWithDefaults(e.annotationMirrors.first { it.psi.qualifiedName == "Anno" })
                .values.map { it.value }
        
        assertEquals(listOf("Tom", 10), getValues(a))
        assertEquals(listOf("Tom", 20), getValues(b))
        assertEquals(listOf("Mary", 10), getValues(c))
        assertEquals(listOf("Mary", 20), getValues(d))
    }
    
    fun testGetAllMembers() = test("GetAllMembers", "*") { set, roundEnv, env ->
        val clazz = env.findClass("MyClass")
        val members = clazz.enclosedElements
        assertEquals(5, members.size) // constructor, field, getter/setter, arbitrary method

        val allMembers = env.elementUtils.getAllMembers(clazz)
        assertEquals(17, allMembers.size)
        assertEquals("<init>, clone, equals, finalize, getClass, getMyProperty, hashCode, myFunction, myProperty, " +
                     "notify, notifyAll, registerNatives, setMyProperty, toString, wait, wait, wait", 
                     allMembers.sortedBy { it.simpleName.toString() }.joinToString { it.simpleName })
    }
    
    fun testGetPackageOf() = test("GetPackageOf", "*") { set, roundEnv, env ->
        val e = env.elementUtils

        val myClass = env.findClass("test.MyClass")
        val packageElement = e.getPackageOf(myClass)
        assertEquals("test", packageElement.qualifiedName)
        assertEquals(packageElement, e.getPackageOf(packageElement))
        
        assertNull(e.getPackageElement("SomethingStrange"))
        val testPackageElement = e.getPackageElement("test")
        assertEquals(packageElement, testPackageElement)
        assertFalse(packageElement.isUnnamed)
        val classes = packageElement.enclosedElements.filterIsInstance<JeTypeElement>()
        assertEquals(3, classes.size)
    }
    
    fun testGetArrayType() = test("GetPackageOf", "*") { set, roundEnv, env ->
        val myClass = env.findClass("test.MyClass").asType()
        val array = env.typeUtils.getArrayType(myClass)
        assert(array is JeArrayType)
    }
}