/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental

import org.jetbrains.kotlin.script.util.DependsOn
import org.jetbrains.kotlin.script.util.Repository
import org.jetbrains.kotlin.script.util.resolvers.Resolver
import org.jetbrains.kotlin.script.util.resolvers.experimental.MavenArtifactCoordinates
import java.io.File

fun GenericDependenciesResolver.asOldResolver() : Resolver {
    return object: Resolver{
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
}