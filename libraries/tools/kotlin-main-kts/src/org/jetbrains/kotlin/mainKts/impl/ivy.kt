/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.mainKts.impl

import org.apache.ivy.Ivy
import org.apache.ivy.core.LogOptions
import org.apache.ivy.core.module.descriptor.DefaultDependencyArtifactDescriptor
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor
import org.apache.ivy.core.module.id.ModuleRevisionId
import org.apache.ivy.core.resolve.ResolveOptions
import org.apache.ivy.core.settings.IvySettings
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorWriter
import org.apache.ivy.plugins.resolver.ChainResolver
import org.apache.ivy.plugins.resolver.IBiblioResolver
import org.apache.ivy.plugins.resolver.IBiblioResolver.DEFAULT_M2_ROOT
import org.apache.ivy.plugins.resolver.URLResolver
import org.apache.ivy.util.DefaultMessageLogger
import org.apache.ivy.util.Message
import org.jetbrains.kotlin.script.util.KotlinAnnotatedScriptDependenciesResolver
import org.jetbrains.kotlin.script.util.resolvers.DirectResolver
import org.jetbrains.kotlin.script.util.resolvers.experimental.GenericArtifactCoordinates
import org.jetbrains.kotlin.script.util.resolvers.experimental.GenericRepositoryCoordinates
import org.jetbrains.kotlin.script.util.resolvers.experimental.GenericRepositoryWithBridge
import org.jetbrains.kotlin.script.util.resolvers.experimental.MavenArtifactCoordinates
import java.io.File

class IvyResolver : GenericRepositoryWithBridge {

    private fun String?.isValidParam() = this?.isNotBlank() ?: false

    override fun tryResolve(artifactCoordinates: GenericArtifactCoordinates): Iterable<File>? = with (artifactCoordinates) {
        if (this is MavenArtifactCoordinates && (groupId.isValidParam() || artifactId.isValidParam())) {
            resolveArtifact(groupId.orEmpty(), artifactId.orEmpty(), version.orEmpty())
        } else {
            val artifactType = string.substringAfterLast('@', "").trim()
            val stringCoordinates = if (artifactType.isNotEmpty()) string.removeSuffix("@$artifactType") else string
            if (stringCoordinates.isValidParam() && stringCoordinates.count { it == ':' }.let { it == 2 || it == 3 }) {
                val artifactId = stringCoordinates.split(':')
                resolveArtifact(
                    artifactId[0], artifactId[1], artifactId[2],
                    if (artifactId.size > 3) artifactId[3] else null,
                    if (artifactType.isNotEmpty()) artifactType else null
                )
            } else {
                error("Unrecognized set of arguments to ivy resolver: $stringCoordinates")
            }
        }
    }

    private val ivyResolvers = arrayListOf<URLResolver>()

    private fun resolveArtifact(
        groupId: String, artifactName: String, revision: String, conf: String? = null, type: String? = null
    ): List<File> {

        if (ivyResolvers.isEmpty() || ivyResolvers.none { it.name == "central" }) {
            ivyResolvers.add(
                IBiblioResolver().apply {
                    isM2compatible = true
                    name = "central"
                }
            )
        }
        val ivySettings = IvySettings().apply {
            val resolver =
                if (ivyResolvers.size == 1) ivyResolvers.first()
                else ChainResolver().also {
                    for (resolver in ivyResolvers) {
                        it.add(resolver)
                    }
                }
            addResolver(resolver)
            setDefaultResolver(resolver.name)
        }

        val ivy = Ivy.newInstance(ivySettings)

        val moduleDescriptor = DefaultModuleDescriptor.newDefaultInstance(
            ModuleRevisionId.newInstance(groupId, "$artifactName-caller", "working")
        )

        val depsDescriptor = DefaultDependencyDescriptor(
            moduleDescriptor,
            ModuleRevisionId.newInstance(groupId, artifactName, conf, revision),
            false, false, true
        )
        if (type != null) {
            val depArtifact = DefaultDependencyArtifactDescriptor(depsDescriptor, artifactName, type, type, null, null)
            depsDescriptor.addDependencyArtifact(conf, depArtifact)
        }
        moduleDescriptor.addDependency(depsDescriptor)

        val resolveOptions = ResolveOptions().apply {
            confs = arrayOf("default")
            log = LogOptions.LOG_QUIET
            isOutputReport = false
        }

        //init resolve report

        // TODO: find out why direct resolving doesn't work
        // val report = ivy.resolve(moduleDescriptor, resolveOptions)

        //creates an ivy configuration file
        val ivyFile = createTempFile("ivy", ".xml").apply { deleteOnExit() }
        XmlModuleDescriptorWriter.write(moduleDescriptor, ivyFile)
        val report = ivy.resolve(ivyFile.toURI().toURL(), resolveOptions)

        return report.allArtifactsReports.map { it.localFile }
    }

    override fun tryAddRepository(repositoryCoordinates: GenericRepositoryCoordinates): Boolean {
        val url = repositoryCoordinates.url
        if (url != null) {
            ivyResolvers.add(
                URLResolver().apply {
                    isM2compatible = true
                    name = repositoryCoordinates.name.takeIf { it.isValidParam() } ?: url.host
                    addArtifactPattern("${url.toString().let { if (it.endsWith('/')) it else "$it/" }}$DEFAULT_ARTIFACT_PATTERN")
                }
            )
            return true
        }
        return false
    }

    companion object {
        const val DEFAULT_ARTIFACT_PATTERN = "[organisation]/[module]/[revision]/[artifact]-[revision](-[classifier]).[ext]"

        init {
            Message.setDefaultLogger(DefaultMessageLogger(1))
        }
    }
}

class FilesAndIvyResolver :
    KotlinAnnotatedScriptDependenciesResolver(emptyList(), arrayListOf(DirectResolver(), IvyResolver()).asIterable())
