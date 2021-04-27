/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle

import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.testfixtures.ProjectBuilder
import org.hamcrest.MatcherAssert.assertThat
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.junit.Assert.fail
import org.junit.Test

class MppPublicationTest {

    private val project = ProjectBuilder.builder().build() as ProjectInternal

    init {
        project.plugins.apply("kotlin-multiplatform")
        project.plugins.apply("maven-publish")
    }

    private val kotlin = project.extensions.getByType(KotlinMultiplatformExtension::class.java)

    init {
        kotlin.jvm()
        kotlin.js().nodejs()
    }

    @Test
    fun `contains kotlinMultiplatform publication`() {
        project.evaluate()
        val publishing = project.extensions.getByType(PublishingExtension::class.java)
        publishing.publications
            .withType(MavenPublication::class.java)
            .findByName("kotlinMultiplatform") ?: fail("Missing 'kotlinMultiplatform' publication")
    }


    @Test
    fun `all publication contains sourcesJar`() {
        project.evaluate()
        val publishing = project.extensions.getByType(PublishingExtension::class.java)
        publishing.publications
            .filterIsInstance<MavenPublication>()
            .forEach { publication ->
                val sources = publication.artifacts.filter { artifact -> artifact.classifier == "sources" }
                assertThat(
                    "Expected at least one sources artifact for ${publication.name}",
                    sources.isNotEmpty()
                )
            }
    }
}
