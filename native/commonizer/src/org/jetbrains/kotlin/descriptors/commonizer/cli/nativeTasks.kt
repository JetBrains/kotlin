/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cli

import org.jetbrains.kotlin.descriptors.commonizer.*
import org.jetbrains.kotlin.descriptors.commonizer.konan.*
import org.jetbrains.kotlin.descriptors.commonizer.konan.CopyUnconsumedModulesAsIsConsumer
import org.jetbrains.kotlin.descriptors.commonizer.konan.CopyStdlibResultsConsumer
import org.jetbrains.kotlin.descriptors.commonizer.konan.LibraryCommonizer
import org.jetbrains.kotlin.descriptors.commonizer.konan.ModuleSerializer
import org.jetbrains.kotlin.descriptors.commonizer.repository.*
import org.jetbrains.kotlin.descriptors.commonizer.repository.EmptyRepository
import org.jetbrains.kotlin.descriptors.commonizer.repository.FilesRepository
import org.jetbrains.kotlin.descriptors.commonizer.repository.KonanDistributionRepository
import org.jetbrains.kotlin.descriptors.commonizer.repository.Repository
import org.jetbrains.kotlin.descriptors.commonizer.stats.FileStatsOutput
import org.jetbrains.kotlin.descriptors.commonizer.stats.StatsCollector
import org.jetbrains.kotlin.descriptors.commonizer.stats.StatsType
import org.jetbrains.kotlin.descriptors.commonizer.utils.ProgressLogger
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
        val targetLibraries = getMandatory<List<File>, InputLibrariesOptionType>()
        val dependencyLibraries = getMandatory<List<File>, DependencyLibrariesOptionType>()
        val outputCommonizerTarget = getMandatory<SharedCommonizerTarget, OutputCommonizerTargetOptionType>()
        val statsType = getOptional<StatsType, StatsTypeOptionType> { it == "log-stats" } ?: StatsType.NONE

        val konanTargets = outputCommonizerTarget.konanTargets
        val commonizerTargets = konanTargets.map(::CommonizerTarget)

        val progressLogger = ProgressLogger(CliLoggerAdapter(2), startImmediately = true)
        val libraryLoader = DefaultNativeLibraryLoader(progressLogger)
        val statsCollector = StatsCollector(statsType, commonizerTargets)
        val repository = FilesRepository(targetLibraries.toSet(), libraryLoader)

        val resultsConsumer = buildResultsConsumer {
            this add ModuleSerializer(destination, HierarchicalCommonizerOutputLayout)
            this add CopyUnconsumedModulesAsIsConsumer(
                repository, destination, commonizerTargets.toSet(), NativeDistributionCommonizerOutputLayout, progressLogger
            )
            this add LoggingResultsConsumer(outputCommonizerTarget, progressLogger)
        }

        LibraryCommonizer(
            konanDistribution = distribution,
            repository = repository,
            dependencies = KonanDistributionRepository(distribution, commonizerTargets.toSet(), libraryLoader) +
                    FilesRepository(dependencyLibraries.toSet(), libraryLoader),
            libraryLoader = libraryLoader,
            targets = commonizerTargets,
            resultsConsumer = resultsConsumer,
            statsCollector = statsCollector,
            progressLogger = progressLogger
        ).run()

        statsCollector?.writeTo(FileStatsOutput(destination, statsType.name.toLowerCase()))
    }
}

internal class NativeDistributionCommonize(options: Collection<Option<*>>) : Task(options) {
    override val category get() = Category.COMMONIZATION

    override fun execute(logPrefix: String) {
        val distribution = KonanDistribution(getMandatory<File, NativeDistributionOptionType>())
        val destination = getMandatory<File, OutputOptionType>()
        val konanTargets = getMandatory<List<KonanTarget>, NativeTargetsOptionType>()
        val commonizerTargets = konanTargets.map(::CommonizerTarget)

        val copyStdlib = getOptional<Boolean, BooleanOptionType> { it == "copy-stdlib" } ?: false
        val copyEndorsedLibs = getOptional<Boolean, BooleanOptionType> { it == "copy-endorsed-libs" } ?: false
        val statsType = getOptional<StatsType, StatsTypeOptionType> { it == "log-stats" } ?: StatsType.NONE

        val progressLogger = ProgressLogger(CliLoggerAdapter(2), startImmediately = true)
        val libraryLoader = DefaultNativeLibraryLoader(progressLogger)
        val repository = KonanDistributionRepository(distribution, commonizerTargets.toSet(), libraryLoader)
        val existingTargets = commonizerTargets.filter { repository.getLibraries(it).isNotEmpty() }.toSet()
        val statsCollector = StatsCollector(statsType, commonizerTargets)

        val resultsConsumer = buildResultsConsumer {
            this add ModuleSerializer(destination, NativeDistributionCommonizerOutputLayout)
            this add CopyUnconsumedModulesAsIsConsumer(
                repository, destination, commonizerTargets.toSet(), NativeDistributionCommonizerOutputLayout, progressLogger
            )
            if (copyStdlib) this add CopyStdlibResultsConsumer(distribution, destination, progressLogger)
            if (copyEndorsedLibs) this add CopyEndorsedLibrairesResultsConsumer(distribution, destination, progressLogger)
            this add LoggingResultsConsumer(SharedCommonizerTarget(existingTargets), progressLogger)
        }

        val targetNames = commonizerTargets.joinToString { it.prettyName }
        val descriptionSuffix = estimateLibrariesCount(repository, commonizerTargets).let { " ($it items)" }
        val description = "${logPrefix}Preparing commonized Kotlin/Native libraries for targets $targetNames$descriptionSuffix"
        println(description)

        LibraryCommonizer(
            repository = repository,
            konanDistribution = distribution,
            dependencies = EmptyRepository,
            libraryLoader = libraryLoader,
            targets = commonizerTargets,
            resultsConsumer = resultsConsumer,
            statsCollector = statsCollector,
            progressLogger = progressLogger
        ).run()

        statsCollector?.writeTo(FileStatsOutput(destination, statsType.name.toLowerCase()))

        println("$description: Done")
    }

    companion object {
        private fun estimateLibrariesCount(repository: Repository, targets: List<LeafCommonizerTarget>): Int {
            return targets.flatMap { repository.getLibraries(it) }.count()
        }
    }
}
