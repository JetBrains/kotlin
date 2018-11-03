/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.idea

import org.jetbrains.idea.maven.project.MavenProject
import org.jetbrains.kotlin.idea.facet.KotlinFacet
import org.jetbrains.kotlin.idea.maven.MavenProjectImportHandler

class KotlinSerializationMavenImportHandler: MavenProjectImportHandler {
    override fun invoke(facet: KotlinFacet, mavenProject: MavenProject) {
        KotlinSerializationImportHandler.modifyCompilerArguments(facet, PLUGIN_MAVEN_JAR)
    }

    private val PLUGIN_MAVEN_JAR = "kotlinx-maven-serialization-plugin"
}