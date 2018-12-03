/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.mainKts.impl

import org.apache.ivy.Ivy
import org.apache.ivy.core.LogOptions
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor
import org.apache.ivy.core.module.id.ModuleRevisionId
import org.apache.ivy.core.resolve.ResolveOptions
import org.apache.ivy.core.settings.IvySettings
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorWriter
import org.apache.ivy.plugins.resolver.ChainResolver
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
import java.net.MalformedURLException
import java.net.URL

class IvyResolver : GenericRepositoryWithBridge {

    private fun String?.isValidParam() = this?.isNotBlank() ?: false

    override fun tryResolve(artifactCoordinates: GenericArtifactCoordinates): Iterable<File>? = with (artifactCoordinates) {
        val artifactId =
            if (this is MavenArtifactCoordinates && (groupId.isValidParam() || artifactId.isValidParam())) {
                listOf(groupId.orEmpty(), artifactId.orEmpty(), version.orEmpty())
            } else {
                val stringCoordinates = string
                if (stringCoordinates.isValidParam() && stringCoordinates.count { it == ':' } == 2) {
                    stringCoordinates.split(':')
                } else {
                    error("Unknown set of arguments to maven resolver: $stringCoordinates")
                }
            }
        resolveArtifact(artifactId)
    }

    private val ivyResolvers = arrayListOf<URLResolver>()

    private fun resolveArtifact(artifactId: List<String>): List<File> {

        if (ivyResolvers.isEmpty() || ivyResolvers.none { it.name == "central" }) {
            ivyResolvers.add(
                URLResolver().apply {
                    isM2compatible = true
                    name = "central"
                    addArtifactPattern("http://repo1.maven.org/maven2/$DEFAULT_ARTIFACT_PATTERN")
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

        val ivyfile = File.createTempFile("ivy", ".xml")
        ivyfile.deleteOnExit()

        val moduleDescriptor = DefaultModuleDescriptor.newDefaultInstance(
            ModuleRevisionId.newInstance(artifactId[0], artifactId[1] + "-caller", "working")
        )

        val depsDescriptor = DefaultDependencyDescriptor(
            moduleDescriptor,
            ModuleRevisionId.newInstance(artifactId[0], artifactId[1], artifactId[2]),
            false, false, true
        )
        moduleDescriptor.addDependency(depsDescriptor)

        //creates an ivy configuration file
        XmlModuleDescriptorWriter.write(moduleDescriptor, ivyfile)

        val resolveOptions = ResolveOptions().apply {
            confs = arrayOf("default")
            log = LogOptions.LOG_QUIET
            isOutputReport = false
        }

        //init resolve report
        val report = ivy.resolve(ivyfile.toURI().toURL(), resolveOptions)

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
        const val DEFAULT_ARTIFACT_PATTERN = "[organisation]/[module]/[revision]/[artifact](-[revision]).[ext]"

        init {
            Message.setDefaultLogger(DefaultMessageLogger(1))
        }
    }
}

class FilesAndIvyResolver :
    KotlinAnnotatedScriptDependenciesResolver(emptyList(), arrayListOf(DirectResolver(), IvyResolver()).asIterable())
