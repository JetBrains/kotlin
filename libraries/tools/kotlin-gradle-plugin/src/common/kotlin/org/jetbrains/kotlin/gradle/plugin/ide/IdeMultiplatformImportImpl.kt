/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide

import org.jetbrains.kotlin.gradle.ExternalKotlinTargetApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinDependency
import org.jetbrains.kotlin.gradle.idea.serialize.IdeaExtrasSerializationExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.ide.IdeMultiplatformImport.*
import org.jetbrains.kotlin.tooling.core.Extras


internal class IdeMultiplatformImportImpl(
    private val extension: KotlinMultiplatformExtension
) : IdeMultiplatformImport {

    override fun resolveDependencies(sourceSet: KotlinSourceSet): Set<IdeaKotlinDependency> {
        return createDependencyResolver().resolve(sourceSet)
    }

    override fun <T : Any> serialize(key: Extras.Key<T>, value: T): ByteArray? {
        return null
    }

    private data class RegisteredDependencyResolver(
        val resolver: IdeDependencyResolver,
        val constraint: SourceSetConstraint,
        val phase: DependencyResolutionPhase,
        val level: DependencyResolutionLevel,
    )

    private data class RegisteredDependencyTransformer(
        val transformer: IdeDependencyTransformer,
        val constraint: SourceSetConstraint,
        val phase: DependencyTransformationPhase
    )

    private data class RegisteredDependencyEffect(
        val effect: IdeDependencyEffect,
        val constraint: SourceSetConstraint,
    )

    private val registeredDependencyResolvers = mutableListOf<RegisteredDependencyResolver>()
    private val registeredDependencyTransformers = mutableListOf<RegisteredDependencyTransformer>()
    private val registeredDependencyEffects = mutableListOf<RegisteredDependencyEffect>()
    private val registeredExtrasSerializationExtensions = mutableListOf<IdeaExtrasSerializationExtension>()

    @ExternalKotlinTargetApi
    override fun registerDependencyResolver(
        resolver: IdeDependencyResolver,
        constraint: SourceSetConstraint,
        phase: DependencyResolutionPhase,
        level: DependencyResolutionLevel
    ) {
        registeredDependencyResolvers.add(
            RegisteredDependencyResolver(resolver, constraint, phase, level)
        )
    }

    @ExternalKotlinTargetApi
    override fun registerDependencyTransformer(
        transformer: IdeDependencyTransformer,
        constraint: SourceSetConstraint,
        phase: DependencyTransformationPhase
    ) {
        registeredDependencyTransformers.add(
            RegisteredDependencyTransformer(transformer, constraint, phase)
        )
    }

    @ExternalKotlinTargetApi
    override fun registerDependencyEffect(effect: IdeDependencyEffect, constraint: SourceSetConstraint) {
        registeredDependencyEffects.add(
            RegisteredDependencyEffect(effect, constraint)
        )
    }

    @ExternalKotlinTargetApi
    override fun registerExtrasSerializationExtension(extension: IdeaExtrasSerializationExtension) {
        registeredExtrasSerializationExtensions.add(extension)
    }

    private fun createDependencyResolver(): IdeDependencyResolver {
        return IdeDependencyResolver(DependencyResolutionPhase.values().map { phase ->
            createDependencyResolver(phase)
        }).withTransformer(createDependencyTransformer())
            .withEffect(createDependencyEffect())
    }

    private fun createDependencyResolver(phase: DependencyResolutionPhase) = IdeDependencyResolver resolve@{ sourceSet ->
        val applicableResolvers = registeredDependencyResolvers
            .filter { it.phase == phase }
            .filter { it.constraint(sourceSet) }
            .groupBy { it.level }

        /* Find resolvers in the highest resolution level and only consider those */
        DependencyResolutionLevel.values().reversed().forEach { level ->
            val resolvers = applicableResolvers[level].orEmpty().map { it.resolver }
            if (resolvers.isNotEmpty()) {
                return@resolve IdeDependencyResolver(resolvers).resolve(sourceSet)
            }
        }

        /* No resolvers found */
        emptySet()
    }

    private fun createDependencyTransformer(): IdeDependencyTransformer {
        return IdeDependencyTransformer(DependencyTransformationPhase.values().map { phase ->
            createDependencyTransformer(phase)
        })
    }

    private fun createDependencyTransformer(phase: DependencyTransformationPhase): IdeDependencyTransformer {
        return IdeDependencyTransformer { sourceSet, dependencies ->
            IdeDependencyTransformer(
                registeredDependencyTransformers
                    .filter { it.phase == phase }
                    .filter { it.constraint(sourceSet) }
                    .map { it.transformer }
            ).transform(sourceSet, dependencies)
        }
    }

    private fun createDependencyEffect(): IdeDependencyEffect = IdeDependencyEffect { sourceSet, dependencies ->
        registeredDependencyEffects
            .filter { it.constraint(sourceSet) }
            .forEach { it.effect(sourceSet, dependencies) }
    }
}
