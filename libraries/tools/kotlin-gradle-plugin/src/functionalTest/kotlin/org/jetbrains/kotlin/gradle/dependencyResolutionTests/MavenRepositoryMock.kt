/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dependencyResolutionTests

import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import java.io.File

fun KotlinMultiplatformExtension.publishAsMockedLibrary(repoDir: File, name: String, version: String) {
    val moduleRootDir = repoDir.resolve("test/$name/$version")
    moduleRootDir.mkdirs()
    val moduleFile = moduleRootDir.resolve("$name-$version.module")
    moduleFile.writeText(publishedMockedGradleMetadata(name, version))
    val pomFile = moduleFile.parentFile.resolve("$name-$version.pom")
    pomFile.writeText(
        """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd"
                xmlns="http://maven.apache.org/POM/4.0.0"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
              <modelVersion>4.0.0</modelVersion>
              <groupId>test</groupId>
              <artifactId>$name</artifactId>
              <version>$version</version>
            </project>
        """.trimIndent()
    )
}

private fun KotlinMultiplatformExtension.publishedMockedGradleMetadata(name: String, version: String): String {
    val variants = targets.joinToString(",") { it.variantJson() + "\n" }
    return """
            {
              "formatVersion": "1.1",
              "component": {
                "group": "test",
                "module": "$name",
                "version": "$version",
                "attributes": {
                  "org.gradle.status": "release"
                }
              },
              "variants": [
                 $variants
              ]
            }
        """.trimIndent()
}

private fun KotlinTarget.variantJson(): String {
    val apiElements = project.configurations.getByName(apiElementsConfigurationName)
    val attributesString = apiElements
        .attributes
        .keySet()
        .map { it to apiElements.attributes.getAttribute(it) }
        .joinToString(",\n") { "\"${it.first.name}\": \"${it.second}\"" }

    return """
                {
                  "name": "$name",
                  "attributes": {
                    $attributesString
                  }
                }
            """.trimIndent()
}