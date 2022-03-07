/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExternalVariantApi::class)

package org.jetbrains.kotlin.gradle.kpm.idea

import org.gradle.api.Project
import org.gradle.tooling.provider.model.ToolingModelBuilder
import org.jetbrains.kotlin.gradle.kpm.external.ExternalVariantApi
import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKotlinProjectModelBuilder.*
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinPm20ProjectExtension
import org.jetbrains.kotlin.gradle.utils.UnsafeApi

internal class IdeaKotlinProjectModelBuilderImpl @UnsafeApi("Use factory methods instead") constructor(
    private val extension: KotlinPm20ProjectExtension,
) : ToolingModelBuilder, IdeaKotlinProjectModelBuilder {

    private data class RegisteredDependencyResolver(
        val resolver: IdeaKotlinDependencyResolver,
        val constraint: FragmentConstraint,
        val phase: DependencyResolutionPhase,
        val level: DependencyResolutionLevel,
    )

    private data class RegisteredDependencyTransformer(
        val transformer: IdeaKotlinDependencyTransformer,
        val constraint: FragmentConstraint,
        val phase: DependencyTransformationPhase
    )

    private data class RegisteredDependencyEffect(
        val effect: IdeaKotlinDependencyEffect,
        val constraint: FragmentConstraint,
    )

    private val registeredDependencyResolvers = mutableListOf<RegisteredDependencyResolver>()
    private val registeredDependencyTransformers = mutableListOf<RegisteredDependencyTransformer>()
    private val registeredDependencyEffects = mutableListOf<RegisteredDependencyEffect>()

    override fun registerDependencyResolver(
        resolver: IdeaKotlinDependencyResolver,
        constraint: FragmentConstraint,
        phase: DependencyResolutionPhase,
        level: DependencyResolutionLevel
    ) {
        registeredDependencyResolvers.add(
            RegisteredDependencyResolver(resolver, constraint, phase, level)
        )
    }

    override fun registerDependencyTransformer(
        transformer: IdeaKotlinDependencyTransformer,
        constraint: FragmentConstraint,
        phase: DependencyTransformationPhase
    ) {
        registeredDependencyTransformers.add(
            RegisteredDependencyTransformer(transformer, constraint, phase)
        )
    }

    override fun registerDependencyEffect(
        effect: IdeaKotlinDependencyEffect,
        constraint: FragmentConstraint
    ) {
        registeredDependencyEffects.add(
            RegisteredDependencyEffect(effect, constraint)
        )
    }

    override fun buildIdeaKotlinProjectModel(): IdeaKotlinProjectModel {
        return Context().toIdeaKotlinProjectModel(extension)
    }

    override fun canBuild(modelName: String): Boolean =
        modelName == IdeaKotlinProjectModel::class.java.name

    override fun buildAll(modelName: String, project: Project): IdeaKotlinProjectModel {
        check(project === extension.project) { "Expected project ${extension.project.path}, found ${project.path}" }
        return buildIdeaKotlinProjectModel()
    }

    private inner class Context : IdeaKotlinProjectModelBuildingContext {
        override val dependencyResolver = createDependencyResolver()
    }

    private fun createDependencyResolver(): IdeaKotlinDependencyResolver {
        return IdeaKotlinDependencyResolver(DependencyResolutionPhase.values().map { phase ->
            createDependencyResolver(phase)
        }).withTransformer(createDependencyTransformer())
            .withEffect(createDependencyEffect())
    }

    private fun createDependencyResolver(phase: DependencyResolutionPhase) = IdeaKotlinDependencyResolver resolve@{ fragment ->
        val applicableResolvers = registeredDependencyResolvers
            .filter { it.phase == phase }
            .filter { it.constraint(fragment) }
            .groupBy { it.level }

        /* Find resolvers in the highest resolution level and only consider those */
        DependencyResolutionLevel.values().reversed().forEach { level ->
            val resolvers = applicableResolvers[level].orEmpty().map { it.resolver }
            if (resolvers.isNotEmpty()) {
                return@resolve IdeaKotlinDependencyResolver(resolvers).resolve(fragment)
            }
        }

        /* No resolvers found */
        emptySet()
    }

    private fun createDependencyTransformer(): IdeaKotlinDependencyTransformer {
        return IdeaKotlinDependencyTransformer(DependencyTransformationPhase.values().map { phase ->
            createDependencyTransformer(phase)
        })
    }

    private fun createDependencyTransformer(phase: DependencyTransformationPhase): IdeaKotlinDependencyTransformer {
        return IdeaKotlinDependencyTransformer { fragment, dependencies ->
            IdeaKotlinDependencyTransformer(
                registeredDependencyTransformers
                    .filter { it.phase == phase }
                    .filter { it.constraint(fragment) }
                    .map { it.transformer }
            ).transform(fragment, dependencies)
        }
    }

    private fun createDependencyEffect(): IdeaKotlinDependencyEffect = IdeaKotlinDependencyEffect { fragment, dependencies ->
        registeredDependencyEffects
            .filter { it.constraint(fragment) }
            .forEach { it.effect(fragment, dependencies) }
    }
}
