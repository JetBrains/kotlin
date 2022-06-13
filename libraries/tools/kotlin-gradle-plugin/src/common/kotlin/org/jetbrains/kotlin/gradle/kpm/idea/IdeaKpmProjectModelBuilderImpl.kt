/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExternalVariantApi::class)

package org.jetbrains.kotlin.gradle.kpm.idea

import org.gradle.api.Project
import org.gradle.tooling.provider.model.ToolingModelBuilder
import org.jetbrains.kotlin.gradle.kpm.external.ExternalVariantApi
import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKpmProjectModelBuilder.*
import org.jetbrains.kotlin.gradle.kpm.idea.serialize.IdeaKpmExtrasSerializationExtension
import org.jetbrains.kotlin.gradle.kpm.idea.serialize.IdeaKpmSerializationContext
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinPm20ProjectExtension
import org.jetbrains.kotlin.kpm.idea.proto.toByteArray
import org.jetbrains.kotlin.tooling.core.UnsafeApi

internal class IdeaKpmProjectModelBuilderImpl @UnsafeApi("Use factory methods instead") constructor(
    private val extension: KotlinPm20ProjectExtension,
) : ToolingModelBuilder, IdeaKpmProjectModelBuilder {

    private inner class IdeaKpmBuildingContextImpl : IdeaKpmProjectBuildingContext {
        override val dependencyResolver = createDependencyResolver()
    }

    private data class RegisteredDependencyResolver(
        val resolver: IdeaKpmDependencyResolver,
        val constraint: FragmentConstraint,
        val phase: DependencyResolutionPhase,
        val level: DependencyResolutionLevel,
    )

    private data class RegisteredDependencyTransformer(
        val transformer: IdeaKpmDependencyTransformer,
        val constraint: FragmentConstraint,
        val phase: DependencyTransformationPhase
    )

    private data class RegisteredDependencyEffect(
        val effect: IdeaKpmDependencyEffect,
        val constraint: FragmentConstraint,
    )

    private val registeredDependencyResolvers = mutableListOf<RegisteredDependencyResolver>()
    private val registeredDependencyTransformers = mutableListOf<RegisteredDependencyTransformer>()
    private val registeredDependencyEffects = mutableListOf<RegisteredDependencyEffect>()
    private val registeredExtrasSerializationExtensions = mutableListOf<IdeaKpmExtrasSerializationExtension>()

    override fun registerDependencyResolver(
        resolver: IdeaKpmDependencyResolver,
        constraint: FragmentConstraint,
        phase: DependencyResolutionPhase,
        level: DependencyResolutionLevel
    ) {
        registeredDependencyResolvers.add(
            RegisteredDependencyResolver(resolver, constraint, phase, level)
        )
    }

    override fun registerDependencyTransformer(
        transformer: IdeaKpmDependencyTransformer,
        constraint: FragmentConstraint,
        phase: DependencyTransformationPhase
    ) {
        registeredDependencyTransformers.add(
            RegisteredDependencyTransformer(transformer, constraint, phase)
        )
    }

    override fun registerDependencyEffect(
        effect: IdeaKpmDependencyEffect,
        constraint: FragmentConstraint
    ) {
        registeredDependencyEffects.add(
            RegisteredDependencyEffect(effect, constraint)
        )
    }

    override fun registerExtrasSerializationExtension(
        extension: IdeaKpmExtrasSerializationExtension
    ) {
        registeredExtrasSerializationExtensions.add(extension)
    }

    override fun buildSerializationContext(): IdeaKpmSerializationContext {
        return IdeaKpmSerializationContext(
            logger = extension.project.logger,
            extrasSerializationExtensions = registeredExtrasSerializationExtensions.toList()
        )
    }

    override fun buildIdeaKpmProject(): IdeaKpmProject =
        IdeaKpmBuildingContextImpl().IdeaKpmProject(extension)


    override fun canBuild(modelName: String): Boolean =
        modelName == IdeaKpmProject::class.java.name || modelName == IdeaKpmProjectContainer::class.java.name

    override fun buildAll(modelName: String, project: Project): Any {
        check(project === extension.project) { "Expected project ${extension.project.path}, found ${project.path}" }

        return when (modelName) {
            IdeaKpmProject::class.java.name -> buildIdeaKpmProject()
            IdeaKpmProjectContainer::class.java.name -> IdeaKpmProjectContainer(
                buildIdeaKpmProject().toByteArray(buildSerializationContext())
            )

            else -> throw IllegalArgumentException("Unexpected modelName: $modelName")
        }
    }

    private fun createDependencyResolver(): IdeaKpmDependencyResolver {
        return IdeaKpmDependencyResolver(DependencyResolutionPhase.values().map { phase ->
            createDependencyResolver(phase)
        }).withTransformer(createDependencyTransformer())
            .withEffect(createDependencyEffect())
    }

    private fun createDependencyResolver(phase: DependencyResolutionPhase) = IdeaKpmDependencyResolver resolve@{ fragment ->
        val applicableResolvers = registeredDependencyResolvers
            .filter { it.phase == phase }
            .filter { it.constraint(fragment) }
            .groupBy { it.level }

        /* Find resolvers in the highest resolution level and only consider those */
        DependencyResolutionLevel.values().reversed().forEach { level ->
            val resolvers = applicableResolvers[level].orEmpty().map { it.resolver }
            if (resolvers.isNotEmpty()) {
                return@resolve IdeaKpmDependencyResolver(resolvers).resolve(fragment)
            }
        }

        /* No resolvers found */
        emptySet()
    }

    private fun createDependencyTransformer(): IdeaKpmDependencyTransformer {
        return IdeaKpmDependencyTransformer(DependencyTransformationPhase.values().map { phase ->
            createDependencyTransformer(phase)
        })
    }

    private fun createDependencyTransformer(phase: DependencyTransformationPhase): IdeaKpmDependencyTransformer {
        return IdeaKpmDependencyTransformer { fragment, dependencies ->
            IdeaKpmDependencyTransformer(
                registeredDependencyTransformers
                    .filter { it.phase == phase }
                    .filter { it.constraint(fragment) }
                    .map { it.transformer }
            ).transform(fragment, dependencies)
        }
    }

    private fun createDependencyEffect(): IdeaKpmDependencyEffect = IdeaKpmDependencyEffect { fragment, dependencies ->
        registeredDependencyEffects
            .filter { it.constraint(fragment) }
            .forEach { it.effect(fragment, dependencies) }
    }
}
