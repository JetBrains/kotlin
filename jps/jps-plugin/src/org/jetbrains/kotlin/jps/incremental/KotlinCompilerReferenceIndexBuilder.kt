// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.jetbrains.kotlin.jps.incremental

import com.intellij.util.containers.ContainerUtil
import org.jetbrains.jps.ModuleChunk
import org.jetbrains.jps.builders.BuildTargetRegistry
import org.jetbrains.jps.builders.DirtyFilesHolder
import org.jetbrains.jps.builders.JpsBuildBundle
import org.jetbrains.jps.builders.java.JavaSourceRootDescriptor
import org.jetbrains.jps.incremental.BuilderCategory
import org.jetbrains.jps.incremental.CompileContext
import org.jetbrains.jps.incremental.ModuleBuildTarget
import org.jetbrains.jps.incremental.ModuleLevelBuilder
import org.jetbrains.jps.incremental.messages.CustomBuilderMessage
import org.jetbrains.kotlin.config.SettingConstants

/**
 * Based on [org.jetbrains.jps.backwardRefs.JavaBackwardReferenceIndexBuilder]
 */
class KotlinCompilerReferenceIndexBuilder : ModuleLevelBuilder(BuilderCategory.CLASS_POST_PROCESSOR) {
    private val compiledTargets = ContainerUtil.newConcurrentSet<ModuleBuildTarget>()

    override fun getPresentableName(): String = JpsBuildBundle.message("builder.name.backward.references.indexer")

    override fun build(
        context: CompileContext,
        chunk: ModuleChunk,
        dirtyFilesHolder: DirtyFilesHolder<JavaSourceRootDescriptor, ModuleBuildTarget>,
        outputConsumer: OutputConsumer,
    ): ExitCode = ExitCode.OK.also {
        chunk.targets.mapNotNullTo(compiledTargets) { buildTarget ->
            buildTarget.takeIf { context.scope.isWholeTargetAffected(it) }
        }
    }

    override fun buildFinished(context: CompileContext) {
        val targetIndex = context.projectDescriptor.buildTargetIndex
        for (module in context.projectDescriptor.project.modules) {
            val allAreDummyOrCompiled = targetIndex.getModuleBasedTargets(module, BuildTargetRegistry.ModuleTargetSelector.ALL)
                .none { target -> target is ModuleBuildTarget && target !in compiledTargets && !targetIndex.isDummy(target) }

            if (allAreDummyOrCompiled) {
                context.processMessage(
                    CustomBuilderMessage(SettingConstants.KOTLIN_COMPILER_REFERENCE_INDEX_BUILDER_ID, MESSAGE_TYPE, module.name)
                )
            }
        }

        compiledTargets.clear()
    }

    override fun getCompilableFileExtensions(): List<String> = emptyList()

    companion object {
        private const val MESSAGE_TYPE = "processed module"
    }
}