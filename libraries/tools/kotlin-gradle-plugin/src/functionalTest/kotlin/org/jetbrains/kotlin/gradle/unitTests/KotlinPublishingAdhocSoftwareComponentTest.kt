/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.attributes.Attribute
import org.gradle.api.internal.component.SoftwareComponentInternal
import org.jetbrains.kotlin.gradle.dsl.kotlinJvmExtension
import org.jetbrains.kotlin.gradle.util.buildProjectWithJvm
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import org.jetbrains.kotlin.gradle.utils.createConsumable
import org.junit.Test
import kotlin.test.fail

class KotlinPublishingAdhocSoftwareComponentTest {
    @Test
    fun `adhocSoftwareComponent is the same instance as java component so users can extend it`() {
        val project = buildProjectWithJvm {}
        val adhocSoftwareComponent = project.kotlinJvmExtension.publishing.adhocSoftwareComponent
        val javaComponent = project.components.getByName("java")

        if (adhocSoftwareComponent !== javaComponent)
            fail(
                "adhocSoftwareComponent (${adhocSoftwareComponent::class.simpleName}) " +
                        "is not the same instance as java component (${javaComponent::class.simpleName})"
            )
    }

    @Test
    fun `users can add custom variants via adhocSoftwareComponent API in KMP`() {
        val project = buildProjectWithMPP {
            kotlin {
                project.plugins.apply("maven-publish")

                jvm()
                linuxX64()

                val customAttribute = Attribute.of("customAttribute", String::class.java)
                val customConfiguration = configurations.createConsumable("customConfiguration")
                customConfiguration.attributes.attribute(customAttribute, "customValue")
                val artifact1 = project.artifacts.add(customConfiguration.name, file("customConfigurationArtifact1.txt"))
                val artifact2 = project.artifacts.add(customConfiguration.name, file("customConfigurationArtifact2.txt"))
                customConfiguration.artifacts.add(artifact1)

                customConfiguration.outgoing.variants.create("secondaryVariant") {
                    it.attributes.attribute(customAttribute, "customValue2")
                    it.artifacts.add(artifact2)
                }

                publishing.adhocSoftwareComponent {
                    addVariantsFromConfiguration(customConfiguration) {}
                }
            }
        }

        project.evaluate()

        val rootKotlinComponent = project.components.getByName("kotlin") as SoftwareComponentInternal
        if(!rootKotlinComponent.usages.any { usage -> usage.name == "customConfiguration" })
            fail("Missing customConfiguration variant")

        if(!rootKotlinComponent.usages.any { usage -> usage.name == "customConfigurationSecondaryVariant" })
            fail("Missing customConfiguration variant")
    }
}