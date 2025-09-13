/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dependencyResolutionTests

import org.gradle.api.Project
import org.gradle.api.attributes.AttributeContainer
import org.gradle.kotlin.dsl.maven
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.DefaultKotlinUsageContext
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.gradle.utils.targets
import java.io.File

class MavenRepositoryMock {
    data class Module(
        val group: String,
        val name: String,
        val version: String
    ) {
        val asMavenNotation get() = moduleKey(group, name, version)
        val dependencies: MutableSet<Module> = mutableSetOf()

        fun artifactName(target: KotlinTarget, usageContext: DefaultKotlinUsageContext): String {
            val extension = if (target is KotlinJvmTarget) "jar" else "klib"
            return "$name-${usageContext.dependencyConfigurationName}-$version.$extension"
        }

        /**
         * Limitation! All modules should be defined outside of the [variantDependencies] lambda
         */
        var variantDependencies: KotlinTarget.() -> Iterable<Module> = { emptySet() }
    }

    private val declaredModules = mutableMapOf<String, Module>()

    fun module(group: String, name: String, version: String): Module {
        return declaredModules.getOrPut(moduleKey(group, name, version)) { Module(group, name, version) }
    }

    fun module(dependencyNotation: String): Module {
        val (group, name, version) = dependencyNotation.split(":")
        return module(group, name, version)
    }

    fun applyToProject(project: Project, repositoryDir: File) {
        project.allprojects { it.repositories.maven(repositoryDir) }
    }

    fun publishMocks(project: Project, repositoryDir: File) {
        val targets = project.kotlinExtension.targets.toList()
        declaredModules.values.forEach { module -> module.publishAsMockedLibrary(repositoryDir, targets) }
    }

    companion object {
        fun moduleKey(group: String, name: String, version: String) = "$group:$name:$version"
    }
}

class MavenRepositoryMockDsl(
    val repositoryMock: MavenRepositoryMock = MavenRepositoryMock()
) {
    fun module(notation: String, configure: MavenRepositoryMock.Module.() -> Unit = {}): MavenRepositoryMock.Module {
        return repositoryMock.module(notation).apply(configure)
    }

    infix fun MavenRepositoryMock.Module.dependsOn(dependency: MavenRepositoryMock.Module): MavenRepositoryMock.Module {
        this.dependencies.add(dependency)
        return dependency
    }
}

fun mockMavenRepository(init: MavenRepositoryMockDsl.() -> Unit): MavenRepositoryMock {
    val builder = MavenRepositoryMockDsl()
    builder.init()
    return builder.repositoryMock
}

fun Project.mockMavenRepository(
    repositoryDir: File = rootDir.resolve("mavenRepoMock"),
    init: MavenRepositoryMockDsl.() -> Unit,
): MavenRepositoryMock {
    val mock = mockMavenRepository(init)
    mock.publishMocks(this, repositoryDir)
    return mock
}

private fun MavenRepositoryMock.Module.publishAsMockedLibrary(
    repoDir: File,
    kotlinTargets: List<KotlinTarget>
) {
    val groupPath = group.replace(".", "/")
    val moduleRootDir = repoDir.resolve("$groupPath/$name/$version")
    moduleRootDir.mkdirs()
    val moduleFile = moduleRootDir.resolve("$name-$version.module")
    moduleFile.writeText(publishedMockedGradleMetadata(kotlinTargets))
    val pomFile = moduleFile.parentFile.resolve("$name-$version.pom")

    val pomDependencies = dependencies.joinToString("\n") { dependencyModule ->
        """
            <dependency>
              <groupId>${dependencyModule.group}</groupId>
              <artifactId>${dependencyModule.name}</artifactId>
              <version>${dependencyModule.version}</version>
            </dependency>
        """.trimIndent()
    }
    pomFile.writeText(
        """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd"
                xmlns="http://maven.apache.org/POM/4.0.0"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
              <!--  do_not_remove: published-with-gradle-metadata  -->
              <modelVersion>4.0.0</modelVersion>
              <groupId>$group</groupId>
              <artifactId>$name</artifactId>
              <version>$version</version>
              <dependencies>${pomDependencies.prependIndent("            ")}</dependencies>              
            </project>
        """.trimIndent()
    )

    kotlinTargets.forEach { target ->
        target.publishableUsageContexts().forEach { usageContext ->
            val artifactName = artifactName(target, usageContext)
            val artifactFile = moduleRootDir.resolve(artifactName)
            artifactFile.writeText("Mocked artifact content for $target / ${usageContext.name}")
        }
    }
}

private fun MavenRepositoryMock.Module.publishedMockedGradleMetadata(kotlinTargets: List<KotlinTarget>): String {
    val variants = kotlinTargets.flatMap { variantJsons(it) }.joinToString(",") { "$it\n" }
    return """
            {
              "formatVersion": "1.1",
              "component": {
                "group": "$group",
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

private fun KotlinTarget.publishableUsageContexts() = internal
    .kotlinComponents.flatMap { component ->
        if (!component.publishable) return@flatMap emptyList()
        component.internal.usages.filterIsInstance<DefaultKotlinUsageContext>()
    }

private fun MavenRepositoryMock.Module.variantJsons(target: KotlinTarget): List<String> =
    target.publishableUsageContexts().map { usageContext ->
        val artifactName = artifactName(target, usageContext)
        val variantDependencies = variantDependencies(target)
        val variantName = usageContext.name
        variantJsons(artifactName, variantDependencies, variantName, usageContext.attributes)
    }

private fun MavenRepositoryMock.Module.variantJsons(
    artifactName: String,
    variantDependencies: Iterable<MavenRepositoryMock.Module>,
    variantName: String,
    attributes: AttributeContainer
): String {
    val attributesString = attributes
        .keySet()
        .map { it to attributes.getAttribute(it) }
        .joinToString(",\n") { "\"${it.first.name}\": \"${it.second}\"" }

    val allDependencies = dependencies + variantDependencies

    val dependenciesJson = allDependencies.joinToString(", \n") { moduleDependency ->
        """
        {
          "group": "${moduleDependency.group}",
          "module": "${moduleDependency.name}",
          "version": {
            "requires": "${moduleDependency.version}"
          }
        }                        
        """.trimIndent()
    }

    return """
                {
                  "name": "$variantName",
                  "attributes": {
                    $attributesString
                  },
                  "dependencies": [ $dependenciesJson ],
                  "files": [
                    {
                      "name": "$artifactName",
                      "url": "$artifactName"
                    }
                  ]
                }
            """.trimIndent()
}