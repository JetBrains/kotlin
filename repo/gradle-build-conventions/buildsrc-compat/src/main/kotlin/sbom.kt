/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.*
import org.spdx.sbom.gradle.SpdxSbomExtension
import org.spdx.sbom.gradle.SpdxSbomPlugin
import org.spdx.sbom.gradle.SpdxSbomTask
import org.spdx.sbom.gradle.extensions.DefaultSpdxSbomTaskExtension
import plugins.mainPublicationName
import java.net.URI
import java.util.*


fun Project.configureSbom(
    target: String? = null,
    documentName: String? = null,
    gradleConfigurations: Iterable<String> = setOf("runtimeClasspath"),
    publication: NamedDomainObjectProvider<MavenPublication>? = null,
): TaskProvider<SpdxSbomTask> {
    assert(target == null && publication != null) { "provided publication will be ignored when target is null" }
    val project = this
    val targetName = target ?: "${mainPublicationName}Publication"
    apply<SpdxSbomPlugin>()

    configure<SpdxSbomExtension> {
        targets.create(targetName) {
            configurations.set(gradleConfigurations)
            scm {
                tool.set("git")
                uri.set("https://github.com/JetBrains/kotlin.git")
                revision.set("v${project.version}")
            }

            // adjust properties of the document
            document {
                name.set(documentName ?: project.name)
                // NOTE: The URI does not have to be accessible. It is only intended to provide a unique ID.
                // In many cases, the URI will point to a Web accessible document, but this should not be assumed to be the case.
                namespace.set("https://www.jetbrains.com/spdxdocs/${UUID.randomUUID()}")
                creator.set("Organization: JetBrains s.r.o.")
                packageSupplier.set("Organization: JetBrains s.r.o.")
            }
        }
    }

    val NOASSERTION = URI.create("NOASSERTION")
    tasks.withType<SpdxSbomTask>().configureEach {
        taskExtension.set(object : DefaultSpdxSbomTaskExtension() {
            override fun mapRepoUri(input: URI?, moduleId: ModuleVersionIdentifier): URI {
                return NOASSERTION
            }
        })
    }

    val sbomOutputDirectory = layout.buildDirectory.dir("spdx/$targetName")
    val spdxSbomTask = tasks.named<SpdxSbomTask>("spdxSbomFor$targetName") {
        outputDirectory.set(sbomOutputDirectory)
    }
    val sbomFile = sbomOutputDirectory.map { it.file("$targetName.spdx.json") }
    val sbomCfg = configurations.maybeCreate("sbomFor$targetName").apply {
        isCanBeResolved = false
        isCanBeConsumed = true
    }

    val sbomArtifact = artifacts.add(sbomCfg.name, sbomFile) {
        type = "sbom"
        extension = "spdx.json"
        builtBy(spdxSbomTask)
    }

    if (target == null) {
        pluginManager.withPlugin("kotlin-build-publishing") {
            val mainPublication = the<PublishingExtension>().publications[mainPublicationName] as MavenPublication
            mainPublication.artifact(sbomArtifact)
        }
    } else if (publication != null) {
        publication.configure {
            artifact(sbomArtifact)
        }
    }

    return spdxSbomTask
}
