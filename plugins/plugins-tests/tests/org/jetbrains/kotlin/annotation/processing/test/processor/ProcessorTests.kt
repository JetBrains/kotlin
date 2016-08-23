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

import org.jetbrains.kotlin.java.model.elements.*
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import javax.lang.model.element.AnnotationMirror

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
}