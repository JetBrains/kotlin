/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.gradle.plugin.internal

import org.gradle.api.Project
import org.gradle.api.artifacts.maven.Conf2ScopeMappingContainer
import org.gradle.api.artifacts.maven.MavenPom
import org.gradle.api.artifacts.maven.MavenResolver
import org.gradle.api.plugins.MavenPluginConvention
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Upload
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractKotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.PomDependenciesRewriter

class MavenPluginConfiguratorG6 : MavenPluginConfigurator {
    override fun applyConfiguration(
        project: Project,
        target: AbstractKotlinTarget,
        shouldRewritePoms: Provider<Boolean>
    ) {
        project.pluginManager.withPlugin("maven") {
            project.tasks.withType(Upload::class.java).all { uploadTask ->
                uploadTask.repositories.withType(MavenResolver::class.java).all { mavenResolver ->
                    val pomRewriter = PomDependenciesRewriter(project, target.kotlinComponents.single())
                    rewritePom(mavenResolver.pom, pomRewriter, shouldRewritePoms)
                }
            }

            // Setup conf2ScopeMappings so that the API dependencies are written with the compile scope in the POMs in case of 'java' plugin
            project.convention.getPlugin(MavenPluginConvention::class.java)
                .conf2ScopeMappings.addMapping(0, project.configurations.getByName("api"), Conf2ScopeMappingContainer.COMPILE)
        }
    }

    private fun rewritePom(
        pom: MavenPom,
        rewriter: PomDependenciesRewriter,
        shouldRewritePom: Provider<Boolean>
    ) {
        pom.withXml { xml ->
            if (shouldRewritePom.get())
                rewriter.rewritePomMppDependenciesToActualTargetModules(xml)
        }
    }

    class Gradle6MavenPluginConfiguratorVariantFactory : MavenPluginConfigurator.MavenPluginConfiguratorVariantFactory {
        override fun getInstance(): MavenPluginConfigurator = MavenPluginConfiguratorG6()
    }
}