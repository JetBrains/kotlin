/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide

import org.jetbrains.kotlin.gradle.ExternalKotlinTargetApi
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.idea.proto.tcs.toByteArray
import org.jetbrains.kotlin.gradle.idea.proto.toByteArray
import org.jetbrains.kotlin.gradle.idea.serialize.IdeaKotlinExtrasSerializationExtension
import org.jetbrains.kotlin.gradle.idea.serialize.IdeaKotlinSerializationContext
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinDependency
import org.jetbrains.kotlin.gradle.kpm.idea.IdeaSerializationContext
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.ide.IdeDependencyResolver.Companion.resolvedBy
import org.jetbrains.kotlin.gradle.plugin.ide.IdeMultiplatformImport.*
import org.jetbrains.kotlin.gradle.plugin.ide.IdeMultiplatformImport.Companion.logger
import org.jetbrains.kotlin.tooling.core.Extras
import org.jetbrains.kotlin.tooling.core.HasMutableExtras
import org.jetbrains.kotlin.utils.addToStdlib.measureTimeMillisWithResult
import kotlin.system.measureTimeMillis


@Suppress("LoggingStringTemplateAsArgument")
internal class IdeMultiplatformImportImpl(
    private val extension: KotlinProjectExtension
) : IdeMultiplatformImport {

    override fun resolveDependencies(sourceSetName: String): Set<IdeaKotlinDependency> {
        return resolveDependencies(extension.sourceSets.getByName(sourceSetName))
    }

    override fun resolveDependencies(sourceSet: KotlinSourceSet): Set<IdeaKotlinDependency> {
        return createDependencyResolver().resolve(sourceSet)
    }

    override fun resolveDependenciesSerialized(sourceSetName: String): List<ByteArray> {
        return serialize(resolveDependencies(sourceSetName))
    }

    override fun resolveExtrasSerialized(owner: Any): ByteArray? {
        if (owner !is HasMutableExtras) return null
        return owner.extras.toByteArray(createSerializationContext())
    }

    override fun serialize(dependencies: Iterable<IdeaKotlinDependency>): List<ByteArray> {
        val context = createSerializationContext()
        return dependencies.map { dependency -> dependency.toByteArray(context) }
    }

    override fun <T : Any> serialize(key: Extras.Key<T>, value: T): ByteArray? {
        val context = createSerializationContext()
        return context.extrasSerializationExtension.serializer(key)?.serialize(context, value)
    }

    private val registeredDependencyResolvers = mutableListOf<RegisteredDependencyResolver>()
    private val registeredAdditionalArtifactResolvers = mutableListOf<RegisteredAdditionalArtifactResolver>()
    private val registeredDependencyTransformers = mutableListOf<RegisteredDependencyTransformer>()
    private val registeredDependencyEffects = mutableListOf<RegisteredDependencyEffect>()
    private val registeredExtrasSerializationExtensions = mutableListOf<IdeaKotlinExtrasSerializationExtension>()

    @OptIn(Idea222Api::class)
    @ExternalKotlinTargetApi
    override fun registerDependencyResolver(
        resolver: IdeDependencyResolver,
        constraint: SourceSetConstraint,
        phase: DependencyResolutionPhase,
        level: DependencyResolutionLevel
    ) {
        registeredDependencyResolvers.add(
            RegisteredDependencyResolver(extension.project.kotlinIdeMultiplatformImportStatistics, resolver, constraint, phase, level)
        )

        if (resolver is IdeDependencyResolver.WithBuildDependencies) {
            val project = extension.project
            val dependencies = project.provider { resolver.dependencies(project) }
            extension.project.locateOrRegisterIdeResolveDependenciesTask().configure { it.dependsOn(dependencies) }
            extension.project.prepareKotlinIdeaImportTask.configure { it.dependsOn(dependencies) }
        }
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
    override fun registerAdditionalArtifactResolver(
        resolver: IdeAdditionalArtifactResolver,
        constraint: SourceSetConstraint,
        phase: AdditionalArtifactResolutionPhase,
        level: AdditionalArtifactResolutionLevel
    ) {
        registeredAdditionalArtifactResolvers.add(
            RegisteredAdditionalArtifactResolver(
                extension.project.kotlinIdeMultiplatformImportStatistics, resolver, constraint, phase, level
            )
        )
    }

    @ExternalKotlinTargetApi
    override fun registerDependencyEffect(effect: IdeDependencyEffect, constraint: SourceSetConstraint) {
        registeredDependencyEffects.add(
            RegisteredDependencyEffect(effect, constraint)
        )
    }

    @ExternalKotlinTargetApi
    override fun registerExtrasSerializationExtension(extension: IdeaKotlinExtrasSerializationExtension) {
        registeredExtrasSerializationExtensions.add(extension)
    }

    private fun createDependencyResolver(): IdeDependencyResolver {
        return IdeDependencyResolver(
            DependencyResolutionPhase.values().map { phase -> createDependencyResolver(phase) }
        )
            .withAdditionalArtifactResolver(createAdditionalArtifactsResolver())
            .withTransformer(createDependencyTransformer())
            .withEffect(createDependencyEffect())
    }

    private fun createDependencyResolver(phase: DependencyResolutionPhase) = IdeDependencyResolver resolve@{ sourceSet ->
        val applicableResolvers = registeredDependencyResolvers
            .filter { it.phase == phase }
            .filter { it.constraint(sourceSet) }
            .groupBy { it.level }

        /* Find resolvers in the highest resolution level and only consider those */
        DependencyResolutionLevel.values().reversed().forEach { level ->
            val resolvers = applicableResolvers[level].orEmpty()
            if (resolvers.isNotEmpty()) {
                return@resolve IdeDependencyResolver(resolvers).resolve(sourceSet)
            }
        }

        /* No resolvers found */
        emptySet()
    }

    private fun createAdditionalArtifactsResolver() = IdeAdditionalArtifactResolver(
        AdditionalArtifactResolutionPhase.values().map { phase -> createAdditionalArtifactsResolver(phase) })

    private fun createAdditionalArtifactsResolver(phase: AdditionalArtifactResolutionPhase) =
        IdeAdditionalArtifactResolver resolve@{ sourceSet, dependencies ->
            val applicableResolvers = registeredAdditionalArtifactResolvers
                .filter { it.phase == phase }
                .filter { it.constraint(sourceSet) }
                .groupBy { it.level }

            AdditionalArtifactResolutionLevel.values().reversed().forEach { level ->
                val resolvers = applicableResolvers[level].orEmpty()
                if (resolvers.isNotEmpty()) {
                    resolvers.forEach { resolver -> resolver.resolve(sourceSet, dependencies) }
                    return@resolve
                }
            }
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

    private fun createSerializationContext(): IdeaKotlinSerializationContext {
        return IdeaSerializationContext(
            logger = extension.project.logger,
            extrasSerializationExtensions = registeredExtrasSerializationExtensions.toList()
        )
    }

    private data class RegisteredDependencyTransformer(
        val transformer: IdeDependencyTransformer,
        val constraint: SourceSetConstraint,
        val phase: DependencyTransformationPhase
    )

    private data class RegisteredDependencyEffect(
        val effect: IdeDependencyEffect,
        val constraint: SourceSetConstraint,
    )

    private data class RegisteredDependencyResolver(
        private val statistics: IdeMultiplatformImportStatistics,
        private val resolver: IdeDependencyResolver,
        val constraint: SourceSetConstraint,
        val phase: DependencyResolutionPhase,
        val level: DependencyResolutionLevel,
    ) : IdeDependencyResolver {

        private class TimeMeasuredResult(val timeInMillis: Long, val dependencies: Set<IdeaKotlinDependency>)

        override fun resolve(sourceSet: KotlinSourceSet): Set<IdeaKotlinDependency> {
            return runCatching { resolveTimed(sourceSet) }
                .onFailure { error -> reportError(sourceSet, error) }
                .onSuccess { result -> reportSuccess(sourceSet, result) }
                .onSuccess { result -> attachResolvedByExtra(result.dependencies) }
                .getOrNull()?.dependencies.orEmpty()
        }

        private fun resolveTimed(sourceSet: KotlinSourceSet): TimeMeasuredResult {
            val (time, result) = measureTimeMillisWithResult { resolver.resolve(sourceSet) }
            statistics.addExecutionTime(resolver::class.java, time)
            return TimeMeasuredResult(time, result)
        }

        private fun reportError(sourceSet: KotlinSourceSet, error: Throwable) {
            logger.error("e: ${resolver::class.java.name} failed on ${IdeaKotlinSourceCoordinates(sourceSet)}", error)
        }

        private fun reportSuccess(sourceSet: KotlinSourceSet, result: TimeMeasuredResult) {
            if (!logger.isDebugEnabled) return
            logger.debug(
                "${resolver::class.java.name} resolved on ${IdeaKotlinSourceCoordinates(sourceSet)}: " +
                        "${result.dependencies} (${result.timeInMillis} ms)"
            )
        }

        private fun attachResolvedByExtra(dependencies: Iterable<IdeaKotlinDependency>) {
            dependencies.forEach { dependency ->
                if (dependency.resolvedBy == null) dependency.resolvedBy = resolver
            }
        }
    }

    private class RegisteredAdditionalArtifactResolver(
        private val statistics: IdeMultiplatformImportStatistics,
        private val resolver: IdeAdditionalArtifactResolver,
        val constraint: SourceSetConstraint,
        val phase: AdditionalArtifactResolutionPhase,
        val level: AdditionalArtifactResolutionLevel
    ) : IdeAdditionalArtifactResolver {
        override fun resolve(sourceSet: KotlinSourceSet, dependencies: Set<IdeaKotlinDependency>) {
            runCatching { measureTimeMillis { resolver.resolve(sourceSet, dependencies) } }
                .onFailure { logger.error("e: ${resolver::class.java.name} failed on ${IdeaKotlinSourceCoordinates(sourceSet)}", it) }
                .onSuccess { statistics.addExecutionTime(resolver::class.java, it) }
        }
    }
}
