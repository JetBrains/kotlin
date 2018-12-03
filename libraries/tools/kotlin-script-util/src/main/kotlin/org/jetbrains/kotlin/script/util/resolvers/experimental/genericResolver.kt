/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.script.util.resolvers.experimental

import org.jetbrains.kotlin.script.util.DependsOn
import org.jetbrains.kotlin.script.util.Repository
import org.jetbrains.kotlin.script.util.resolvers.Resolver
import org.jetbrains.kotlin.script.util.resolvers.toRepositoryFileOrNull
import org.jetbrains.kotlin.script.util.resolvers.toRepositoryUrlOrNull
import java.io.File
import java.net.URL

interface GenericArtifactCoordinates {
    val string: String
}

interface GenericRepositoryCoordinates {
    val string: String
    val name: String? get() = null
    val url: URL? get() = string.toRepositoryUrlOrNull()
    val file: File? get() = (url?.takeIf { it.protocol == "file" }?.path ?: string).toRepositoryFileOrNull()
}

interface GenericResolver {
    fun tryResolve(artifactCoordinates: GenericArtifactCoordinates): Iterable<File>?
    fun tryAddRepository(repositoryCoordinates: GenericRepositoryCoordinates): Boolean

    fun tryResolve(artifactCoordinates: String): Iterable<File>? = tryResolve(BasicArtifactCoordinates(artifactCoordinates))

    fun tryAddRepository(repositoryCoordinates: String, id: String? = null): Boolean =
        tryAddRepository(BasicRepositoryCoordinates(repositoryCoordinates, id))
}

open class BasicArtifactCoordinates(override val string: String) : GenericArtifactCoordinates

open class BasicRepositoryCoordinates(override val string: String, override val name: String? = null) : GenericRepositoryCoordinates

interface GenericRepositoryWithBridge : GenericResolver, Resolver {
    override fun tryResolve(dependsOn: DependsOn): Iterable<File>? =
        tryResolve(
            with(dependsOn) {
                MavenArtifactCoordinates(value, groupId, artifactId, version)
            }
        )

    override fun tryAddRepo(annotation: Repository): Boolean =
        with(annotation) {
            tryAddRepository(
                value.takeIf { it.isNotBlank() } ?: url,
                id.takeIf { it.isNotBlank() }
            )
        }
}

open class MavenArtifactCoordinates(
    val value: String?,
    val groupId: String?,
    val artifactId: String?,
    val version: String?
) : GenericArtifactCoordinates {
    override val string: String
        get() = value.takeIf { it?.isNotBlank() ?: false }
            ?: listOf(groupId, artifactId, version).filter { it?.isNotBlank() ?: false }.joinToString(":")
}
