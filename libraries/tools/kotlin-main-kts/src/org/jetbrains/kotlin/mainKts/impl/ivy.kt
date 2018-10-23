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
import org.jetbrains.kotlin.script.util.DependsOn
import org.jetbrains.kotlin.script.util.KotlinAnnotatedScriptDependenciesResolver
import org.jetbrains.kotlin.script.util.Repository
import org.jetbrains.kotlin.script.util.resolvers.DirectResolver
import org.jetbrains.kotlin.script.util.resolvers.Resolver
import java.io.File
import java.net.MalformedURLException
import java.net.URL

class IvyResolver : Resolver {

    private fun String.isValidParam() = isNotBlank()

    override fun tryResolve(dependsOn: DependsOn): Iterable<File>? {
        val artifactId = when {
            dependsOn.groupId.isValidParam() || dependsOn.artifactId.isValidParam() -> {
                listOf(dependsOn.groupId, dependsOn.artifactId, dependsOn.version)
            }
            dependsOn.value.isValidParam() && dependsOn.value.count { it == ':' } == 2 -> {
                dependsOn.value.split(':')
            }
            else -> {
                error("Unknown set of arguments to maven resolver: ${dependsOn.value}")
            }
        }
        return resolveArtifact(artifactId)
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

    override fun tryAddRepo(annotation: Repository): Boolean {
        val urlStr = annotation.url.takeIf { it.isValidParam() } ?: annotation.value.takeIf { it.isValidParam() } ?: return false
        val url = urlStr.toRepositoryUrlOrNull() ?: return false
        ivyResolvers.add(
            URLResolver().apply {
                isM2compatible = true
                name = annotation.id.takeIf { it.isValidParam() } ?: url.host
                addArtifactPattern("${url.toString().let { if (it.endsWith('/')) it else "$it/" }}$DEFAULT_ARTIFACT_PATTERN")
            }
        )
        return true
    }

    companion object {
        const val DEFAULT_ARTIFACT_PATTERN = "[organisation]/[module]/[revision]/[artifact](-[revision]).[ext]"

        init {
            Message.setDefaultLogger(DefaultMessageLogger(1))
        }
    }
}

private fun String.toRepositoryUrlOrNull(): URL? =
    try {
        URL(this)
    } catch (_: MalformedURLException) {
        null
    }

class FilesAndIvyResolver :
    KotlinAnnotatedScriptDependenciesResolver(emptyList(), arrayListOf(DirectResolver(), IvyResolver()).asIterable())
