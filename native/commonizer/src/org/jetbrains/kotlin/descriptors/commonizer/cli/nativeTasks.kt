/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cli

import org.jetbrains.kotlin.commonizer.api.*
import org.jetbrains.kotlin.descriptors.commonizer.*
import org.jetbrains.kotlin.descriptors.commonizer.EmptyRepository
import org.jetbrains.kotlin.descriptors.commonizer.KonanDistribution
import org.jetbrains.kotlin.descriptors.commonizer.KonanDistributionRepository
import org.jetbrains.kotlin.descriptors.commonizer.Repository
import org.jetbrains.kotlin.descriptors.commonizer.konan.*
import org.jetbrains.kotlin.descriptors.commonizer.konan.CopyStdlibCommonizerResultSerializer
import org.jetbrains.kotlin.descriptors.commonizer.konan.DefaultCommonizerResultSerializer
import org.jetbrains.kotlin.descriptors.commonizer.konan.LibraryCommonizer
import org.jetbrains.kotlin.descriptors.commonizer.stats.StatsCollector
import org.jetbrains.kotlin.descriptors.commonizer.stats.StatsType
import org.jetbrains.kotlin.konan.library.KONAN_DISTRIBUTION_KLIB_DIR
import org.jetbrains.kotlin.konan.library.KONAN_DISTRIBUTION_PLATFORM_LIBS_DIR
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File

internal class NativeDistributionListTargets(options: Collection<Option<*>>) : Task(options) {
    override val category get() = Category.INFORMATIONAL

    override fun execute(logPrefix: String) {
        val distributionPath = getMandatory<File, NativeDistributionOptionType>()

        val targets = distributionPath.resolve(KONAN_DISTRIBUTION_KLIB_DIR)
            .resolve(KONAN_DISTRIBUTION_PLATFORM_LIBS_DIR)
            .list()
            ?.sorted()
            ?: emptyList()

        println()
        if (targets.isEmpty())
            println("No hardware targets found inside of the Kotlin/Native distribution \"$distributionPath\".")
        else {
            println("${targets.size} hardware targets found inside of the Kotlin/Native distribution \"$distributionPath\":")
            targets.forEach(::println)
        }
        println()
    }
}

internal class NativeKlibCommonize(options: Collection<Option<*>>) : Task(options) {
    override val category: Category = Category.COMMONIZATION

    override fun execute(logPrefix: String) {
        val distribution = KonanDistribution(getMandatory<File, NativeDistributionOptionType>())
        val destination = getMandatory<File, OutputOptionType>()
        val targetLibraries = getMandatory<List<File>, TargetLibrariesOptionType>()
        val dependencyLibraries = getMandatory<List<File>, DependencyLibrariesOptionType>()
        val outputHierarchy = getMandatory<SharedCommonizerTarget, OutputHierarchyOptionType>()

        val statsType = getOptional<StatsType, StatsTypeOptionType> { it == "log-stats" } ?: StatsType.NONE

        val logger = CliLoggerAdapter(2)
        val libraryLoader = DefaultNativeLibraryLoader(logger)
        val resultSerializer = DefaultCommonizerResultSerializer(destination, HierarchicalCommonizerOutputLayout, logger)
        val statsCollector = StatsCollector(statsType, outputHierarchy.konanTargets.toList(), destination)

        LibraryCommonizer(
            konanDistribution = distribution,
            repository = FilesRepository(targetLibraries.toSet(), libraryLoader),
            dependencies = KonanDistributionRepository(distribution, outputHierarchy.konanTargets, libraryLoader) +
                    FilesRepository(dependencyLibraries.toSet(), libraryLoader),
            libraryLoader = libraryLoader,
            targets = outputHierarchy.konanTargets.toList(),
            resultSerializer = resultSerializer,
            statsCollector = statsCollector,
            logger = CliLoggerAdapter(2)
        ).run()
    }
}

internal class NativeDistributionCommonize(options: Collection<Option<*>>) : Task(options) {
    override val category get() = Category.COMMONIZATION

    override fun execute(logPrefix: String) {
        val distribution = KonanDistribution(getMandatory<File, NativeDistributionOptionType>())
        val destination = getMandatory<File, OutputOptionType>()
        val targets = getMandatory<List<KonanTarget>, NativeTargetsOptionType>()
        val statsType = getOptional<StatsType, StatsTypeOptionType> { it == "log-stats" } ?: StatsType.NONE
        val copyStdlib = getOptional<Boolean, BooleanOptionType> { it == "copy-stdlib" } ?: false
        val copyEndorsedLibs = getOptional<Boolean, BooleanOptionType> { it == "copy-endorsed-libs" } ?: false

        val targetNames = targets.joinToString { "[${it.name}]" }

        val logger = CliLoggerAdapter(2)
        val libraryLoader = DefaultNativeLibraryLoader(logger)
        val repository = KonanDistributionRepository(distribution, targets.toSet(), DefaultNativeLibraryLoader(logger))
        val resultSerializer = DefaultCommonizerResultSerializer(destination, NativeDistributionCommonizerOutputLayout, logger) +
                (if (copyStdlib) CopyStdlibCommonizerResultSerializer(distribution, destination, logger) else null) +
                (if (copyEndorsedLibs) CopyEndorsedCommonizerResultSerializer(distribution, destination, logger) else null)

        val statsCollector = StatsCollector(statsType, targets, destination)

        val descriptionSuffix = estimateLibrariesCount(repository, targets).let { " ($it items)" }
        val description = "${logPrefix}Preparing commonized Kotlin/Native libraries for targets $targetNames$descriptionSuffix"
        println(description)

        LibraryCommonizer(
            konanDistribution = distribution,
            repository = repository,
            libraryLoader = libraryLoader,
            dependencies = EmptyRepository,
            targets = targets,
            resultSerializer = resultSerializer,
            statsCollector = statsCollector,
            logger = logger,
        ).run()

        println("$description: Done")
    }

    companion object {
        private fun estimateLibrariesCount(repository: Repository, targets: List<KonanTarget>): Int {
            return targets.flatMap { repository.getLibraries(LeafCommonizerTarget(it)) }.count()
        }
    }
}
