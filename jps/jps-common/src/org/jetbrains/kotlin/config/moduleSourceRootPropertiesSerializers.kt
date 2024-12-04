/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config

import org.jdom.Element
import org.jetbrains.jps.model.java.JavaResourceRootProperties
import org.jetbrains.jps.model.java.JavaSourceRootProperties
import org.jetbrains.jps.model.java.JpsJavaExtensionService
import org.jetbrains.jps.model.module.JpsModuleSourceRootType
import org.jetbrains.jps.model.serialization.module.JpsModuleRootModelSerializer
import org.jetbrains.jps.model.serialization.module.JpsModuleSourceRootPropertiesSerializer

private const val IS_GENERATED_ATTRIBUTE = "generated"
private const val RELATIVE_OUTPUT_PATH_ATTRIBUTE = "relativeOutputPath"

const val KOTLIN_SOURCE_ROOT_TYPE_ID = "kotlin-source"
const val KOTLIN_TEST_ROOT_TYPE_ID = "kotlin-test"
const val KOTLIN_RESOURCE_ROOT_TYPE_ID = "kotlin-resource"
const val KOTLIN_TEST_RESOURCE_ROOT_TYPE_ID = "kotlin-test-resource"

// Based on org.jetbrains.jps.model.serialization.java.JpsJavaModelSerializerExtension.JavaSourceRootPropertiesSerializer class
sealed class KotlinSourceRootPropertiesSerializer(
    type: JpsModuleSourceRootType<JavaSourceRootProperties>,
    typeId: String
) : JpsModuleSourceRootPropertiesSerializer<JavaSourceRootProperties>(type, typeId) {
    object Source : KotlinSourceRootPropertiesSerializer(
        SourceKotlinRootType,
        KOTLIN_SOURCE_ROOT_TYPE_ID
    )

    object TestSource : KotlinSourceRootPropertiesSerializer(
        TestSourceKotlinRootType,
        KOTLIN_TEST_ROOT_TYPE_ID
    )

    override fun loadProperties(sourceRootTag: Element): JavaSourceRootProperties {
        val packagePrefix = sourceRootTag.getAttributeValue(JpsModuleRootModelSerializer.PACKAGE_PREFIX_ATTRIBUTE) ?: ""
        val isGenerated = sourceRootTag.getAttributeValue(IS_GENERATED_ATTRIBUTE)?.toBoolean() ?: false
        return JpsJavaExtensionService.getInstance().createSourceRootProperties(packagePrefix, isGenerated)
    }

    override fun saveProperties(properties: JavaSourceRootProperties, sourceRootTag: Element) {
        val packagePrefix = properties.packagePrefix
        if (packagePrefix.isNotEmpty()) {
            sourceRootTag.setAttribute(JpsModuleRootModelSerializer.PACKAGE_PREFIX_ATTRIBUTE, packagePrefix)
        }
        if (properties.isForGeneratedSources) {
            sourceRootTag.setAttribute(IS_GENERATED_ATTRIBUTE, "true")
        }
    }
}

// Based on org.jetbrains.jps.model.serialization.java.JpsJavaModelSerializerExtension.JavaResourceRootPropertiesSerializer
sealed class KotlinResourceRootPropertiesSerializer(
    type: JpsModuleSourceRootType<JavaResourceRootProperties>,
    typeId: String
) : JpsModuleSourceRootPropertiesSerializer<JavaResourceRootProperties>(type, typeId) {
    object Resource : KotlinResourceRootPropertiesSerializer(
        ResourceKotlinRootType,
        KOTLIN_RESOURCE_ROOT_TYPE_ID
    )

    object TestResource : KotlinResourceRootPropertiesSerializer(
        TestResourceKotlinRootType,
        KOTLIN_TEST_RESOURCE_ROOT_TYPE_ID
    )

    override fun loadProperties(sourceRootTag: Element): JavaResourceRootProperties {
        val relativeOutputPath = sourceRootTag.getAttributeValue(RELATIVE_OUTPUT_PATH_ATTRIBUTE) ?: ""
        val isGenerated = sourceRootTag.getAttributeValue(IS_GENERATED_ATTRIBUTE)?.toBoolean() ?: false
        return JpsJavaExtensionService.getInstance().createResourceRootProperties(relativeOutputPath, isGenerated)
    }

    override fun saveProperties(properties: JavaResourceRootProperties, sourceRootTag: Element) {
        val relativeOutputPath = properties.relativeOutputPath
        if (relativeOutputPath.isNotEmpty()) {
            sourceRootTag.setAttribute(RELATIVE_OUTPUT_PATH_ATTRIBUTE, relativeOutputPath)
        }
        if (properties.isForGeneratedSources) {
            sourceRootTag.setAttribute(IS_GENERATED_ATTRIBUTE, "true")
        }
    }
}