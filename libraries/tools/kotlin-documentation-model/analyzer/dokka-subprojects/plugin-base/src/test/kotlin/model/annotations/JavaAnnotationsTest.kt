/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package model.annotations

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.model.*
import translators.findClasslike
import kotlin.test.*

class JavaAnnotationsTest : BaseAbstractTest() {

    val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/main/java")
            }
        }
    }

    @Test // see https://github.com/Kotlin/dokka/issues/2350
    fun `should hande array used as annotation param value`() {
        testInline(
            """
            |/src/main/java/annotation/TestClass.java
            |package annotation;
            |public class TestClass {
            |    @SimpleAnnotation(clazz = String[].class)
            |    public boolean simpleAnnotation() {
            |        return false;
            |    }
            |}
            |
            |/src/main/java/annotation/SimpleAnnotation.java
            |package annotation;
            |@Retention(RetentionPolicy.RUNTIME)
            |@Target(ElementType.METHOD)
            |public @interface SimpleAnnotation {
            |    Class<?> clazz();
            |}
            """.trimIndent(),
            configuration
        ) {
            documentablesTransformationStage = { module ->
                val testClass = module.findClasslike("annotation", "TestClass") as DClass
                assertNotNull(testClass)

                val annotatedFunction = testClass.functions.single { it.name == "simpleAnnotation" }
                val annotation =
                    annotatedFunction.extra[Annotations]?.directAnnotations?.entries?.single()?.value?.single()
                assertNotNull(annotation) { "Expected to find an annotation on simpleAnnotation function, found none" }
                assertEquals("annotation", annotation.dri.packageName)
                assertEquals("SimpleAnnotation", annotation.dri.classNames)
                assertEquals(1, annotation.params.size)

                val param = annotation.params.values.single()
                assertTrue(param is ClassValue)
                // should probably be Array instead
                // String matches parsing of Kotlin sources as of now
                assertEquals("String", param.className)
                assertEquals("java.lang", param.classDRI.packageName)
                assertEquals("String", param.classDRI.classNames)
            }
        }
    }

    @Test // see https://github.com/Kotlin/dokka/issues/2551
    fun `should hande annotation used within annotation params with class param value`() {
        testInline(
            """
            |/src/main/java/annotation/TestClass.java
            |package annotation;
            |public class TestClass {
            |    @XmlElementRefs({
            |            @XmlElementRef(name = "NotOffered", namespace = "http://www.gaeb.de/GAEB_DA_XML/DA86/3.3", type = JAXBElement.class, required = false)
            |    })
            |    public List<JAXBElement<Object>> content;
            |}
            |
            |/src/main/java/annotation/XmlElementRefs.java
            |package annotation;
            |public @interface XmlElementRefs {
            |    XmlElementRef[] value();
            |}
            |
            |/src/main/java/annotation/XmlElementRef.java
            |package annotation;
            |public @interface XmlElementRef {
            |    String name();
            |
            |    String namespace();
            |
            |    boolean required();
            |    
            |    Class<JAXBElement> type();
            |}
            |
            |/src/main/java/annotation/JAXBElement.java
            |package annotation;
            |public class JAXBElement<T> {
            |}
            """.trimIndent(),
            configuration
        ) {
            documentablesTransformationStage = { module ->
                val testClass = module.findClasslike("annotation", "TestClass") as DClass
                assertNotNull(testClass)

                val contentField = testClass.properties.find { it.name == "content" }
                assertNotNull(contentField)

                val annotation = contentField.extra[Annotations]?.directAnnotations?.entries?.single()?.value?.single()
                assertNotNull(annotation) { "Expected to find an annotation on content field, found none" }
                assertEquals("XmlElementRefs", annotation.dri.classNames)
                assertEquals(1, annotation.params.size)

                val arrayParam = annotation.params.values.single()
                assertTrue(arrayParam is ArrayValue, "Expected single annotation param to be array")
                assertEquals(1, arrayParam.value.size)

                val arrayParamValue = arrayParam.value.single()
                assertTrue(arrayParamValue is AnnotationValue)

                val arrayParamAnnotationValue = arrayParamValue.annotation
                assertEquals(4, arrayParamAnnotationValue.params.size)
                assertEquals("XmlElementRef", arrayParamAnnotationValue.dri.classNames)

                val annotationParams = arrayParamAnnotationValue.params.values.toList()

                val nameParam = annotationParams[0]
                assertTrue(nameParam is StringValue)
                assertEquals("NotOffered", nameParam.value)

                val namespaceParam = annotationParams[1]
                assertTrue(namespaceParam is StringValue)
                assertEquals("http://www.gaeb.de/GAEB_DA_XML/DA86/3.3", namespaceParam.value)

                val typeParam = annotationParams[2]
                assertTrue(typeParam is ClassValue)
                assertEquals("JAXBElement", typeParam.className)
                assertEquals("annotation", typeParam.classDRI.packageName)
                assertEquals("JAXBElement", typeParam.classDRI.classNames)

                val requiredParam = annotationParams[3]
                assertTrue(requiredParam is BooleanValue)
                assertFalse(requiredParam.value)
            }
        }
    }

    @Test // see https://github.com/Kotlin/dokka/issues/2509
    fun `should handle generic class in annotation`() {
        testInline(
            """
            |/src/main/java/annotation/Breaking.java
            |package annotation;
            |public class Breaking<Y> { 
            |}
            |
            |/src/main/java/annotation/TestAnnotate.java
            |package annotation;
            |public @interface TestAnnotate {
            |    Class<?> value();
            |}
            |
            |/src/main/java/annotation/TestClass.java
            |package annotation;
            |@TestAnnotate(Breaking.class)
            |public class TestClass {
            |}
            """.trimIndent(),
            configuration
        ) {
            documentablesTransformationStage = { module ->
                val testClass = module.findClasslike("annotation", "TestClass") as DClass
                assertNotNull(testClass)

                val annotation = testClass.extra[Annotations]?.directAnnotations?.entries?.single()?.value?.single()
                assertNotNull(annotation) { "Expected to find an annotation on TestClass, found none" }

                assertEquals("TestAnnotate", annotation.dri.classNames)
                assertEquals(1, annotation.params.size)

                val valueParameter = annotation.params.values.single()
                assertTrue(valueParameter is ClassValue)

                assertEquals("Breaking", valueParameter.className)

                assertEquals("annotation", valueParameter.classDRI.packageName)
                assertEquals("Breaking", valueParameter.classDRI.classNames)
            }
        }
    }
}
