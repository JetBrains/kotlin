/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.test.documentable

import org.jetbrains.dokka.analysis.test.api.javaTestProject
import org.jetbrains.dokka.analysis.test.api.kotlinJvmTestProject
import org.jetbrains.dokka.analysis.test.api.useServices
import org.jetbrains.dokka.analysis.test.api.util.getResourceAbsolutePath
import org.jetbrains.dokka.model.*
import org.jetbrains.kotlin.documentation.KdElementId
import kotlin.test.*

class ExternalDocumentableProviderTest {

    @Test
    fun `should find a valid external class from java stdlib`() {
        val project = javaTestProject {
            javaFile("org/jetbrains/dokka/test/MyProjectJavaClass.java") {
                +"public class MyProjectJavaClass {}"
            }
        }

        project.useServices { context ->
            // first check that Dokka only returns documentables declared in the project by default
            // to make sure later that the external documentable is indeed external and not local
            val projectPackages = context.module.packages
            assertEquals(1, projectPackages.size, "Expected only a single package to be returned")

            val projectPackageChildren = projectPackages.single().children
            assertEquals(1, projectPackageChildren.size, "Expected the project to contain only 1 child")
            assertEquals("MyProjectJavaClass", projectPackageChildren.single().name)


            // query for an external documentable that is not part of the project itself
            val sourceSet = context.configuration.sourceSets.single()
            val arrayListId = KdElementId("java.util/ArrayList")

            val arrayListClasslike = externalDocumentableProvider.getClasslike(arrayListId, sourceSet)
            assertNotNull(arrayListClasslike)

            assertEquals("ArrayList", arrayListClasslike.name)
            assertEquals(arrayListId, arrayListClasslike.declarationId)
            assertTrue(arrayListClasslike is DClass)
            assertTrue(arrayListClasslike.functions.size > 10, "java.util.ArrayList is expected to have >10 functions")

            val superTypes = arrayListClasslike.supertypes.entries.single().value
            val abstractListSuperType = superTypes.firstOrNull {
                it.typeConstructor.declarationId == KdElementId("java.util/AbstractList")
            }
            assertNotNull(abstractListSuperType, "java.util.ArrayList is expected to extend java.util.AbstractList")

        }
    }

    @Test
    fun `should find a valid external annotation from kotlin jvm stdlib`() {
        val project = kotlinJvmTestProject {
            ktFile("org/jetbrains/test/dokka/MyKotlinFile.kt") {
                +"class MyKotlinClass {}"
            }
        }

        project.useServices { context ->
            // first check that Dokka only returns documentables declared in the project by default
            // to make sure later that the external documentable is indeed external and not local
            val projectPackages = context.module.packages
            assertEquals(1, projectPackages.size, "Expected only a single package to be returned")

            val projectPackageChildren = projectPackages.single().children
            assertEquals(1, projectPackageChildren.size, "Expected the project to contain only 1 child")
            assertEquals("MyKotlinClass", projectPackageChildren.single().name)


            // query for an external documentable that is not part of the project itself
            val sourceSet = context.configuration.sourceSets.single()
            val jvmFieldId = KdElementId("kotlin.jvm/JvmField")

            val jvmFieldAnnotation = externalDocumentableProvider.getClasslike(jvmFieldId, sourceSet)
            assertNotNull(jvmFieldAnnotation)

            assertEquals("JvmField", jvmFieldAnnotation.name)
            assertEquals(jvmFieldId, jvmFieldAnnotation.declarationId)
            assertTrue(jvmFieldAnnotation is DAnnotation)
        }
    }

    @Test
    fun `should find a valid external enum from kotlin stdlib`() {
        val project = kotlinJvmTestProject {
            ktFile("org/jetbrains/test/dokka/MyKotlinFile.kt") {
                +"class MyKotlinClass {}"
            }
        }

        project.useServices { context ->
            // first check that Dokka only returns documentables declared in the project by default
            // to make sure later that the external documentable is indeed external and not local
            val projectPackages = context.module.packages
            assertEquals(1, projectPackages.size, "Expected only a single package to be returned")

            val projectPackageChildren = projectPackages.single().children
            assertEquals(1, projectPackageChildren.size, "Expected the project to contain only 1 child")
            assertEquals("MyKotlinClass", projectPackageChildren.single().name)


            // query for an external documentable that is not part of the project itself
            val sourceSet = context.configuration.sourceSets.single()
            val deprecationLevelId = KdElementId("kotlin/DeprecationLevel")

            val deprecationLevelEnum = externalDocumentableProvider.getClasslike(deprecationLevelId, sourceSet)
            assertNotNull(deprecationLevelEnum)

            assertEquals("DeprecationLevel", deprecationLevelEnum.name)
            assertEquals(deprecationLevelId, deprecationLevelEnum.declarationId)
            assertTrue(deprecationLevelEnum is DEnum)
            assertEquals(3, deprecationLevelEnum.entries.size)

            val warningLevel = deprecationLevelEnum.entries[0]
            assertEquals("WARNING", warningLevel.name)
        }
    }

    @Test
    fun `should find a valid external interface from kotlin stdlib`() {
        val project = kotlinJvmTestProject {
            ktFile("org/jetbrains/test/dokka/MyKotlinFile.kt") {
                +"class MyKotlinClass {}"
            }
        }

        project.useServices { context ->
            // first check that Dokka only returns documentables declared in the project by default
            // to make sure later that the external documentable is indeed external and not local
            val projectPackages = context.module.packages
            assertEquals(1, projectPackages.size, "Expected only a single package to be returned")

            val projectPackageChildren = projectPackages.single().children
            assertEquals(1, projectPackageChildren.size, "Expected the project to contain only 1 child")
            assertEquals("MyKotlinClass", projectPackageChildren.single().name)


            // query for an external documentable that is not part of the project itself
            val sourceSet = context.configuration.sourceSets.single()
            val sequenceId = KdElementId("kotlin.sequences/Sequence")

            val sequenceInterface = externalDocumentableProvider.getClasslike(sequenceId, sourceSet)
            assertNotNull(sequenceInterface)

            assertEquals("Sequence", sequenceInterface.name)
            assertEquals(sequenceId, sequenceInterface.declarationId)
            assertTrue(sequenceInterface is DInterface)

            val iteratorFunction = sequenceInterface.functions.firstOrNull { it.name == "iterator" }
            assertNotNull(iteratorFunction)
        }
    }

    @Test
    fun `should find a valid external object from kotlin stdlib`() {
        val project = kotlinJvmTestProject {
            ktFile("org/jetbrains/test/dokka/MyKotlinFile.kt") {
                +"class MyKotlinClass {}"
            }
        }

        project.useServices { context ->
            // first check that Dokka only returns documentables declared in the project by default
            // to make sure later that the external documentable is indeed external and not local
            val projectPackages = context.module.packages
            assertEquals(1, projectPackages.size, "Expected only a single package to be returned")

            val projectPackageChildren = projectPackages.single().children
            assertEquals(1, projectPackageChildren.size, "Expected the project to contain only 1 child")
            assertEquals("MyKotlinClass", projectPackageChildren.single().name)


            // query for an external documentable that is not part of the project itself
            val sourceSet = context.configuration.sourceSets.single()
            val emptyCoroutineContextId = KdElementId("kotlin.coroutines/EmptyCoroutineContext")

            val emptyCoroutineContext = externalDocumentableProvider.getClasslike(emptyCoroutineContextId, sourceSet)
            assertNotNull(emptyCoroutineContext)

            assertEquals("EmptyCoroutineContext", emptyCoroutineContext.name)
            assertEquals(emptyCoroutineContextId, emptyCoroutineContext.declarationId)
            assertTrue(emptyCoroutineContext is DObject)
        }
    }

    @Test
    fun `should find a valid external class from a third party library`() {
        val project = kotlinJvmTestProject {
            dokkaConfiguration {
                kotlinSourceSet {
                    additionalClasspath = setOf(
                        getResourceAbsolutePath("jars/kotlinx-cli-jvm-0.3.6.jar")
                    )
                }
            }

            ktFile("org/jetbrains/test/dokka/MyKotlinFile.kt") {
                +"class MyKotlinClass {}"
            }
        }

        project.useServices { context ->
            // first check that Dokka only returns documentables declared in the project by default
            // to make sure later that the external documentable is indeed external and not local
            val projectPackages = context.module.packages
            assertEquals(1, projectPackages.size, "Expected only a single package to be returned")

            val projectPackageChildren = projectPackages.single().children
            assertEquals(1, projectPackageChildren.size, "Expected the project to contain only 1 child")
            assertEquals("MyKotlinClass", projectPackageChildren.single().name)


            // query for an external documentable that is not part of the project itself
            val sourceSet = context.configuration.sourceSets.single()
            val argTypeId = KdElementId("kotlinx.cli/ArgType")

            val argTypeClass = externalDocumentableProvider.getClasslike(argTypeId, sourceSet)
            assertNotNull(argTypeClass)

            assertEquals("ArgType", argTypeClass.name)
            assertEquals(argTypeId, argTypeClass.declarationId)
            assertTrue(argTypeClass is DClass)
            assertEquals(KotlinModifier.Abstract, argTypeClass.modifier.values.single())
        }
    }

    @Test
    fun `should find a nested interface from java stdlib`() {
        val project = kotlinJvmTestProject {
            ktFile("org/jetbrains/test/dokka/MyKotlinFile.kt") {
                +"class MyKotlinClass {}"
            }
        }

        project.useServices { context ->
            // first check that Dokka only returns documentables declared in the project by default
            // to make sure later that the external documentable is indeed external and not local
            val projectPackages = context.module.packages
            assertEquals(1, projectPackages.size, "Expected only a single package to be returned")

            val projectPackageChildren = projectPackages.single().children
            assertEquals(1, projectPackageChildren.size, "Expected the project to contain only 1 child")
            assertEquals("MyKotlinClass", projectPackageChildren.single().name)


            // query for an external documentable that is not part of the project itself
            val sourceSet = context.configuration.sourceSets.single()
            val mapEntryId = KdElementId("java.util/Map.Entry")

            val mapEntryInterface = externalDocumentableProvider.getClasslike(mapEntryId, sourceSet)
            assertNotNull(mapEntryInterface)

            assertEquals("Entry", mapEntryInterface.name)
            assertEquals(mapEntryId, mapEntryInterface.declarationId)
            assertTrue(mapEntryInterface is DInterface)
        }
    }

    @Test
    fun `should return null for querying non existing dri`() {
        val project = kotlinJvmTestProject {
            ktFile("org/jetbrains/test/dokka/MyKotlinFile.kt") {
                +"class MyKotlinClass {}"
            }
        }

        project.useServices { context ->
            // first check that Dokka only returns documentables declared in the project by default
            // to make sure later that the external documentable is indeed external and not local
            val projectPackages = context.module.packages
            assertEquals(1, projectPackages.size, "Expected only a single package to be returned")

            val projectPackageChildren = projectPackages.single().children
            assertEquals(1, projectPackageChildren.size, "Expected the project to contain only 1 child")
            assertEquals("MyKotlinClass", projectPackageChildren.single().name)


            // query for an external documentable that is not part of the project itself
            val sourceSet = context.configuration.sourceSets.single()

            val nonExistingId = KdElementId("com.example.pckg/NonExistingClassname")
            val nonExistingClasslike = externalDocumentableProvider.getClasslike(nonExistingId, sourceSet)
            assertNull(nonExistingClasslike)
        }
    }

    @Test
    fun `should return null for querying a classlike with a function declaration id`() {
        val project = kotlinJvmTestProject {
            ktFile("org/jetbrains/test/dokka/MyKotlinFile.kt") {
                +"class MyKotlinClass {}"
            }
        }

        project.useServices { context ->
            // first check that Dokka only returns documentables declared in the project by default
            // to make sure later that the external documentable is indeed external and not local
            val projectPackages = context.module.packages
            assertEquals(1, projectPackages.size, "Expected only a single package to be returned")

            val projectPackageChildren = projectPackages.single().children
            assertEquals(1, projectPackageChildren.size, "Expected the project to contain only 1 child")
            assertEquals("MyKotlinClass", projectPackageChildren.single().name)

            // query for an external documentable that is not part of the project itself
            val sourceSet = context.configuration.sourceSets.single()

            // a callable ID: owner package, name, and a signature hash segment
            val functionId = KdElementId("kotlin.collections#listOf#AAA")
            val queriedClasslike = externalDocumentableProvider.getClasslike(functionId, sourceSet)
            assertNull(queriedClasslike)
        }
    }

    @Test
    fun `should return a class defined in the user project itself`() {
        val project = kotlinJvmTestProject {
            ktFile("org/jetbrains/test/dokka/MyKotlinFile.kt") {
                +"class MyKotlinClass {}"
            }
        }

        project.useServices { context ->
            val sourceSet = context.configuration.sourceSets.single()

            val userProjectClassId = KdElementId("org.jetbrains.test.dokka/MyKotlinClass")
            val userProjectClass = externalDocumentableProvider.getClasslike(userProjectClassId, sourceSet)
            assertNotNull(userProjectClass)

            assertEquals("MyKotlinClass", userProjectClass.name)
            assertEquals(userProjectClassId, userProjectClass.declarationId)
            assertTrue(userProjectClass is DClass)
        }
    }

}
