/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental

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
import org.apache.ivy.plugins.resolver.URLResolver
import org.apache.ivy.util.DefaultMessageLogger
import org.apache.ivy.util.Message
import java.io.File
import kotlin.script.experimental.api.ResultWithDiagnostics

class IvyDependenciesResolver : GenericDependenciesResolver() {
    data class IvyArtifactCoordinates(
        val groupId: String,
        val artifactName: String,
        val revision: String,
        val conf: String? = null,
        val type: String? = null
    )

    override fun acceptsArtifact(artifactCoordinates: String) = artifactCoordinates.ivyCoordinates != null

    override fun acceptsRepository(repositoryCoordinates: String): Boolean {
        return repositoryCoordinates.toRepositoryUrlOrNull() != null
    }

    private fun String?.isValidParam() = this?.isNotBlank() ?: false

    val String.ivyCoordinates: IvyArtifactCoordinates?
        get() {
            val artifactType = this.substringAfterLast('@', "").trim()
            val stringCoordinates = if (artifactType.isNotEmpty()) this.removeSuffix("@$artifactType") else this
            if (stringCoordinates.isValidParam() && stringCoordinates.count { it == ':' }.let { it == 2 || it == 3 }) {
                val artifactId = stringCoordinates.split(':')
                return IvyArtifactCoordinates(
                    artifactId[0], artifactId[1], artifactId[2],
                    if (artifactId.size > 3) artifactId[3] else null,
                    if (artifactType.isNotEmpty()) artifactType else null
                )
            } else {
                return null
            }
        }

    override suspend fun resolve(artifactCoordinates: String): ResultWithDiagnostics<Iterable<File>> =
        resolveArtifact(artifactCoordinates.ivyCoordinates!!)


    private val ivyResolvers = arrayListOf<URLResolver>()

    private fun resolveArtifact(
        artifact: IvyArtifactCoordinates
    ): ResultWithDiagnostics<Iterable<File>> {

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

        with(artifact) {
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

            return ResultWithDiagnostics.Success(report.allArtifactsReports.map { it.localFile })
        }
    }

    override fun addRepository(repositoryCoordinates: String) {
        val url = repositoryCoordinates.toRepositoryUrlOrNull() ?: throw Exception("Invalid Ivy repository URL: $repositoryCoordinates")
        ivyResolvers.add(
            URLResolver().apply {
                isM2compatible = true
                name = repositoryCoordinates
                addArtifactPattern("${url.toString().let { if (it.endsWith('/')) it else "$it/" }}$DEFAULT_ARTIFACT_PATTERN")
            }
        )
    }

    companion object {
        const val DEFAULT_ARTIFACT_PATTERN = "[organisation]/[module]/[revision]/[artifact]-[revision](-[classifier]).[ext]"

        init {
            Message.setDefaultLogger(DefaultMessageLogger(1))
        }
    }
}