/*
 * Copyright 2010-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.jps.build;

import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.cli.common.messages.CompilerMessageLocation;
import org.jetbrains.jet.cli.common.messages.CompilerMessageSeverity;
import org.jetbrains.jet.cli.common.messages.MessageCollector;
import org.jetbrains.jet.compiler.runner.*;
import org.jetbrains.jet.utils.PathUtil;
import org.jetbrains.jps.ModuleChunk;
import org.jetbrains.jps.builders.DirtyFilesHolder;
import org.jetbrains.jps.builders.java.JavaSourceRootDescriptor;
import org.jetbrains.jps.incremental.*;
import org.jetbrains.jps.incremental.messages.BuildMessage;
import org.jetbrains.jps.incremental.messages.CompilerMessage;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import static org.jetbrains.jet.cli.common.messages.CompilerMessageSeverity.ERROR;
import static org.jetbrains.jet.cli.common.messages.CompilerMessageSeverity.EXCEPTION;

public class KotlinBuilder extends ModuleLevelBuilder {

    private static final String KOTLIN_BUILDER_NAME = "Kotlin Builder";

    protected KotlinBuilder() {
        super(BuilderCategory.SOURCE_PROCESSOR);
    }

    @NotNull
    @Override
    public String getPresentableName() {
        return KOTLIN_BUILDER_NAME;
    }

    @Override
    public ExitCode build(
            CompileContext context,
            ModuleChunk chunk,
            DirtyFilesHolder<JavaSourceRootDescriptor, ModuleBuildTarget> dirtyFilesHolder,
            OutputConsumer outputConsumer
    ) throws ProjectBuildException, IOException {

        MessageCollector messageCollector = new MessageCollectorAdapter(context);

        if (chunk.getModules().size() > 1) {
            messageCollector.report(
                    ERROR, "Circular dependencies are not supported: " + chunk.getModules(),
                    CompilerMessageLocation.NO_LOCATION);
            return ExitCode.ABORT;
        }

        ModuleBuildTarget representativeTarget = chunk.representativeTarget();

        // For non-incremental build: take all sources
        if (!KotlinSourceFileCollector.hasDirtyFiles(dirtyFilesHolder)) {
            return ExitCode.NOTHING_DONE;
        }
        List<File> sourceFiles = KotlinSourceFileCollector.getAllKotlinSourceFiles(representativeTarget);
        //List<File> sourceFiles = KotlinSourceFileCollector.getDirtySourceFiles(dirtyFilesHolder);

        if (sourceFiles.isEmpty()) {
            return ExitCode.NOTHING_DONE;
        }

        File moduleFile = KotlinBuilderModuleScriptGenerator.generateModuleDescription(context, representativeTarget, sourceFiles);

        File outputDir = representativeTarget.getOutputDir();

        CompilerEnvironment environment = CompilerEnvironment.getEnvironmentFor(PathUtil.getKotlinPathsForJpsPluginOrJpsTests(), outputDir);
        if (!environment.success()) {
            environment.reportErrorsTo(messageCollector);
            return ExitCode.ABORT;
        }

        assert outputDir != null : "CompilerEnvironment must have checked for outputDir to be not null, but it didn't";

        OutputItemsCollectorImpl outputItemCollector = new OutputItemsCollectorImpl(outputDir);

        KotlinCompilerRunner.runCompiler(
                messageCollector,
                environment,
                moduleFile,
                outputItemCollector,
                /*runOutOfProcess = */false);

        for (SimpleOutputItem outputItem : outputItemCollector.getOutputs()) {
            outputConsumer.registerOutputFile(
                    representativeTarget,
                    outputItem.getOutputFile(),
                    paths(outputItem.getSourceFiles()));
        }

        return ExitCode.OK;
    }

    private static Collection<String> paths(Collection<File> files) {
        Collection<String> result = ContainerUtil.newArrayList();
        for (File file : files) {
            result.add(file.getPath());
        }
        return result;
    }

    public static class MessageCollectorAdapter implements MessageCollector {

        private final CompileContext context;

        public MessageCollectorAdapter(@NotNull CompileContext context) {
            this.context = context;
        }

        @Override
        public void report(
                @NotNull CompilerMessageSeverity severity,
                @NotNull String message,
                @NotNull CompilerMessageLocation location
        ) {
            String prefix = "";
            if (severity == EXCEPTION) {
                prefix = CompilerRunnerConstants.INTERNAL_ERROR_PREFIX;
            }
            context.processMessage(new CompilerMessage(
                    CompilerRunnerConstants.KOTLIN_COMPILER_NAME,
                    kind(severity),
                    prefix + message,
                    location.getPath(),
                    -1, -1, -1,
                    location.getLine(),
                    location.getColumn()
            ));
        }

        @NotNull
        private static BuildMessage.Kind kind(@NotNull CompilerMessageSeverity severity) {
            switch (severity) {
                case INFO:
                    return BuildMessage.Kind.INFO;
                case ERROR:
                case EXCEPTION:
                    return BuildMessage.Kind.ERROR;
                case WARNING:
                    return BuildMessage.Kind.WARNING;
                case LOGGING:
                    return BuildMessage.Kind.PROGRESS;
                default:
                    throw new IllegalArgumentException("Unsupported severity: " + severity);
            }
        }

    }
}
