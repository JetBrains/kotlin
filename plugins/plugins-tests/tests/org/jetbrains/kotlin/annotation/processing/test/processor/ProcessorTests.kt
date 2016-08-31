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

import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.java.model.elements.*
import org.jetbrains.kotlin.java.model.types.JeDeclaredType
import org.jetbrains.kotlin.java.model.types.JeMethodExecutableTypeMirror
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisCompletedHandlerExtension
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeMirror
import javax.lang.model.type.TypeVariable

class ProcessorTests : AbstractProcessorTest() {
    override val testDataDir = "plugins/annotation-processing/testData/processors"
    
    fun testSimple() = test("Simple", "Anno") { set, roundEnv, env ->
        assertEquals(1, set.size)
        val annotated = roundEnv.getElementsAnnotatedWith(set.first())
        assertEquals(3, annotated.size)
        
        val clazz = annotated.firstIsInstance<JeTypeElement>()
        val method = annotated.firstIsInstance<JeMethodExecutableElement>()
        val field = annotated.firstIsInstance<JeVariableElement>()
        
        listOf(clazz, method, field).forEach {
            it.assertHasAnnotation("Anno", it.simpleName.toString() + "Anno")
        }
    }
    
    fun testSeveralAnnotations() = test("SeveralAnnotations", "Name", "Age") { set, roundEnv, env ->
        val annos = set.toList()
        
        assertEquals(2, annos.size)
        val annotated1 = roundEnv.getElementsAnnotatedWith(annos[0]).sortedBy { it.simpleName.toString() }
        val annotated2 = roundEnv.getElementsAnnotatedWith(annos[1]).sortedBy { it.simpleName.toString() }
        assertEquals(2, annotated1.size)
        assertEquals(annotated1, annotated2)
        
        val (kate, mary) = Pair(annotated1[0] as JeTypeElement, annotated1[1] as JeTypeElement)
        assertEquals("Mary", mary.simpleName)
        with (kate) {
            assertEquals("Kate", simpleName)
            assertHasAnnotation("Name", "Kate")
            assertHasAnnotation("Age", 22)
        }
    }

    fun testStar() = test("Star", "*") { set, roundEnv, env ->
        assertEquals(true, set.any { it.qualifiedName.toString() == "Anno" })
    }
    
    fun testStar2() = test("Star", "Anno", "*") { set, roundEnv, env ->
        assertEquals(true, set.any { it.qualifiedName.toString() == "Anno" })
    }

    fun testStar3() = test("Star3", "Anno", "*") { set, roundEnv, env ->
        assertEquals(true, set.any { it.qualifiedName.toString() == "Anno2" })
    }
    
    fun testDoesNotRun() = testShouldNotRun("DoesNotRun", "Anno")
    fun testDoesNotRun2() = testShouldNotRun("DoesNotRun2", "Anno")

    fun testInheritedAnnotations() {
        val handledClasses = mutableListOf<String>()
        test("InheritedAnnotations", "Anno") { set, roundEnv, env ->
            assertEquals(1, set.size)
            val annotatedElements = roundEnv.getElementsAnnotatedWith(set.first())
            for (element in annotatedElements) {
                handledClasses += element.simpleName.toString()
            }

            val implAnnotations = annotatedElements.first { it.simpleName.toString() == "Impl" }.annotationMirrors
            // Should contain the inherited annotation
            assertEquals(1, implAnnotations.size)
            assertEquals("Anno", (implAnnotations.first() as JeAnnotationMirror).psi.qualifiedName)
        }
        // IntfImpl should not be here. Annotations can be inherited only from superclasses (not interfaces).
        assertEquals(listOf("Base", "Impl", "Intf"), handledClasses.sorted())
    }

    fun testInheritedAnnotationsOverridden() = test("InheritedAnnotationsOverridden", "Anno") { set, roundEnv, env ->
        assertEquals(1, set.size)
        val annotatedElements = roundEnv.getElementsAnnotatedWith(set.first())
        assertEquals(2, annotatedElements.size)
        val implAnnotations = annotatedElements.first { it.simpleName.toString() == "Impl" }.annotationMirrors
        assertEquals(1, implAnnotations.size)
        assertEquals("Tom", implAnnotations.first().elementValues.values.first().value)
    }
    
    fun testNested() = test("Nested", "Anno") { set, roundEnv, env ->
        assertEquals(1, set.size)
        val annotatedElements = roundEnv.getElementsAnnotatedWith(set.first())
        assertEquals(1, annotatedElements.size)
        val test = annotatedElements.first()
        val anno2 = test.annotationMirrors.first { it is JeAnnotationMirror && it.psi.qualifiedName == "Anno" }
        
        fun AnnotationMirror.getParam(name: String) = elementValues.entries.first { it.key.simpleName.toString() == name }.value
        assertTrue(anno2.getParam("a") is JeAnnotationAnnotationValue)
        assertTrue((anno2.getParam("b") as JeArrayAnnotationValue).value.first() is JeAnnotationAnnotationValue)
        assertTrue(anno2.getParam("c") is JePrimitiveAnnotationValue)
        assertTrue(anno2.getParam("d") is JeTypeAnnotationValue)
        assertTrue((anno2.getParam("e") as JeArrayAnnotationValue).value.first() is JeTypeAnnotationValue)
    }
    
    fun testStringArray() = test("StringArray", "*") { set, roundEnv, env ->
        val testClass = env.findClass("Test")
        val suppress = testClass.getAnnotation(Suppress::class.java)
        assertNotNull(suppress)
        assertEquals(listOf("Tom", "Mary"), suppress!!.names.toList())
    }

    fun testTypeArguments() = test("TypeArguments", "*") { set, roundEnv, env ->
        val classA = env.findClass("A")
        val superB = classA.superclass as JeDeclaredType
        val interfaceC = classA.interfaces[0] as JeDeclaredType

        assertTrue(superB.typeArguments.size == 1)
        assertTrue(interfaceC.typeArguments.size == 1)
    }
    
    fun testTypeArguments2() = test("TypeArguments2", "*") { set, roundEnv, env ->
        val b = env.findClass("B")
        val bSuperTypes = env.typeUtils.directSupertypes(b.asType())
        assertEquals(1, bSuperTypes.size)
        val bASuperTypes = env.typeUtils.directSupertypes(bSuperTypes.first())
        assertEquals(2, bASuperTypes.size) // Object and I
        
        fun List<TypeMirror>.iInterface() = first { it.toString().matches("I(<.*>)?".toRegex()) } as DeclaredType
        
        val bai = bASuperTypes.iInterface()
        assertEquals(1, bai.typeArguments.size)
        assertEquals("java.lang.String", bai.typeArguments.first().toString())
        
        val c = env.findClass("C")
        val cSuperTypes = env.typeUtils.directSupertypes(c.asType())
        assertEquals(1, cSuperTypes.size)
        val cai = env.typeUtils.directSupertypes(cSuperTypes.first()).iInterface()
        assertEquals(1, cai.typeArguments.size)
        val typeArg = cai.typeArguments.first()
        assertTrue(typeArg is TypeVariable)
    }
    
    fun testErasureSimple() = test("ErasureSimple", "*") { set, roundEnv, env -> 
        val test = env.findClass("Test")
        val int = test.findMethod("a").returnType
        val void = test.findMethod("b").returnType
        assertEquals(int, env.typeUtils.erasure(int))
        assertEquals(void, env.typeUtils.erasure(void))
    }
    
    fun testErasure2() = test("Erasure2", "*") { set, roundEnv, env ->
        val test = env.findClass("Test")
        
        val erasure = fun (t: JeMethodExecutableTypeMirror) = env.typeUtils.erasure(t)
        fun check(methodName: String, toString: String, transform: (JeMethodExecutableTypeMirror) -> TypeMirror = { it }) {
            val method = test.enclosedElements.first { it is JeMethodExecutableElement && it.simpleName.toString() == methodName }
            assertEquals(toString, transform((method as JeMethodExecutableElement).asType()).toString())
        }
        
        check("a", "()java.lang.String")
        check("b", "(java.lang.String,java.lang.CharSequence)void")
        check("c", "()int")
        
        check("d", "()T")
        check("e", "<D>(D)D")
        check("e", "(java.lang.Object)java.lang.Object", erasure)
        check("f", "(java.util.List<? extends java.util.Map<java.lang.String,java.lang.Integer>>,int)void")
        check("f", "(java.util.List,int)void", erasure)
        check("g", "<D>(D)void")
        check("g", "(java.lang.String)void", erasure)
    }
    
    fun testIncrementalDataSimple() = incrementalDataTest(
            "IncrementalDataSimple",
            "i Intf, i Test, i Test2, i Test3, i Test8")
    
    private fun incrementalDataTest(fileName: String, @Language("TEXT") expectedText: String) {
        test(fileName, "Anno", "Anno2", "Anno3") { set, roundEnv, env -> }
        val ext = AnalysisCompletedHandlerExtension.getInstances(myEnvironment.project)
                .firstIsInstance<AnnotationProcessingExtensionForTests>()
        val incrementalDataFile = ext.incrementalDataFile
        assertNotNull(incrementalDataFile)
        val text = incrementalDataFile!!.readText().lines().sorted().joinToString(", ")
        assertEquals(expectedText, text)
    }
}