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

import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.StreamUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import gnu.trove.THashSet;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.cli.common.KotlinVersion;
import org.jetbrains.jet.cli.common.arguments.CommonCompilerArguments;
import org.jetbrains.jet.cli.common.arguments.K2JSCompilerArguments;
import org.jetbrains.jet.cli.common.arguments.K2JVMCompilerArguments;
import org.jetbrains.jet.cli.common.messages.CompilerMessageLocation;
import org.jetbrains.jet.cli.common.messages.CompilerMessageSeverity;
import org.jetbrains.jet.cli.common.messages.MessageCollector;
import org.jetbrains.jet.compiler.CompilerSettings;
import org.jetbrains.jet.compiler.runner.CompilerEnvironment;
import org.jetbrains.jet.compiler.runner.CompilerRunnerConstants;
import org.jetbrains.jet.compiler.runner.OutputItemsCollectorImpl;
import org.jetbrains.jet.compiler.runner.SimpleOutputItem;
import org.jetbrains.jet.config.IncrementalCompilation;
import org.jetbrains.jet.jps.JpsKotlinCompilerSettings;
import org.jetbrains.jet.jps.incremental.IncrementalCacheImpl;
import org.jetbrains.jet.preloading.ClassLoaderFactory;
import org.jetbrains.jet.utils.PathUtil;
import org.jetbrains.jps.ModuleChunk;
import org.jetbrains.jps.builders.BuildTarget;
import org.jetbrains.jps.builders.DirtyFilesHolder;
import org.jetbrains.jps.builders.java.JavaSourceRootDescriptor;
import org.jetbrains.jps.incremental.*;
import org.jetbrains.jps.incremental.java.JavaBuilder;
import org.jetbrains.jps.incremental.messages.BuildMessage;
import org.jetbrains.jps.incremental.messages.CompilerMessage;
import org.jetbrains.jps.model.JpsProject;
import org.jetbrains.jps.model.module.JpsModule;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.*;

import static org.jetbrains.jet.cli.common.messages.CompilerMessageLocation.NO_LOCATION;
import static org.jetbrains.jet.cli.common.messages.CompilerMessageSeverity.*;
import static org.jetbrains.jet.compiler.runner.CompilerRunnerConstants.INTERNAL_ERROR_PREFIX;
import static org.jetbrains.jet.compiler.runner.KotlinCompilerRunner.runK2JsCompiler;
import static org.jetbrains.jet.compiler.runner.KotlinCompilerRunner.runK2JvmCompiler;

public class KotlinBuilder extends ModuleLevelBuilder {
    private static final Key<Set<File>> ALL_COMPILED_FILES_KEY = Key.create("_all_kotlin_compiled_files_");
    private static final Key<Set<ModuleBuildTarget>> PROCESSED_TARGETS_WITH_REMOVED_FILES = Key.create("_processed_targets_with_removed_files_");

    public static final String KOTLIN_BUILDER_NAME = "Kotlin Builder";
    private static final List<String> COMPILABLE_FILE_EXTENSIONS = Collections.singletonList("kt");

    private static final Function<JpsModule,String> MODULE_NAME = new Function<JpsModule, String>() {
        @Override
        public String fun(JpsModule module) {
            return module.getName();
        }
    };

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
        // Workaround for Android Studio
        if (!isJavaPluginEnabled(context)) {
            messageCollector.report(INFO, "Kotlin JPS plugin is disabled", NO_LOCATION);
            return ExitCode.NOTHING_DONE;
        }

        ModuleBuildTarget representativeTarget = chunk.representativeTarget();

        // For non-incremental build: take all sources
        if (!dirtyFilesHolder.hasDirtyFiles() && !dirtyFilesHolder.hasRemovedFiles()) {
            return ExitCode.NOTHING_DONE;
        }

        boolean hasKotlinFiles = hasKotlinDirtyOrRemovedFiles(dirtyFilesHolder, chunk);
        if (!hasKotlinFiles) {
            return ExitCode.NOTHING_DONE;
        }

        messageCollector.report(INFO, "Kotlin JPS plugin version " + KotlinVersion.VERSION, NO_LOCATION);

        File outputDir = representativeTarget.getOutputDir();

        CompilerEnvironment environment = CompilerEnvironment.getEnvironmentFor(
                PathUtil.getKotlinPathsForJpsPluginOrJpsTests(), outputDir, new ClassLoaderFactory() {
                    @Override
                    public ClassLoader create(ClassLoader compilerClassLoader) {
                        return new MyClassLoader(compilerClassLoader);
                    }
                }
        );
        if (!environment.success()) {
            environment.reportErrorsTo(messageCollector);
            return ExitCode.ABORT;
        }

        assert outputDir != null : "CompilerEnvironment must have checked for outputDir to be not null, but it didn't";

        OutputItemsCollectorImpl outputItemCollector = new OutputItemsCollectorImpl();

        JpsProject project = representativeTarget.getModule().getProject();
        CommonCompilerArguments commonArguments = JpsKotlinCompilerSettings.getCommonCompilerArguments(project);
        CompilerSettings compilerSettings = JpsKotlinCompilerSettings.getCompilerSettings(project);

        final Set<File> allCompiledFiles = getAllCompiledFilesContainer(context);

        if (JpsUtils.isJsKotlinModule(representativeTarget)) {
            if (chunk.getModules().size() > 1) {
                // We do not support circular dependencies, but if they are present, we do our best should not break the build,
                // so we simply yield a warning and report NOTHING_DONE
                messageCollector.report(
                        WARNING, "Circular dependencies are not supported. " +
                                 "The following JS modules depend on each other: " + StringUtil.join(chunk.getModules(), MODULE_NAME, ", ") + ". " +
                                 "Kotlin is not compiled for these modules",
                        NO_LOCATION);
                return ExitCode.NOTHING_DONE;
            }

            Collection<File> sourceFiles = KotlinSourceFileCollector.getAllKotlinSourceFiles(representativeTarget);
            //List<File> sourceFiles = KotlinSourceFileCollector.getDirtySourceFiles(dirtyFilesHolder);

            if (sourceFiles.isEmpty()) {
                return ExitCode.NOTHING_DONE;
            }

            File outputFile = new File(outputDir, representativeTarget.getModule().getName() + ".js");
            List<String> libraryFiles = JpsJsModuleUtils.getLibraryFilesAndDependencies(representativeTarget);
            K2JSCompilerArguments k2JsArguments = JpsKotlinCompilerSettings.getK2JsCompilerArguments(project);

            runK2JsCompiler(commonArguments, k2JsArguments, compilerSettings, messageCollector, environment,
                            outputItemCollector, sourceFiles, libraryFiles, outputFile);
        }
        else {
            if (chunk.getModules().size() > 1) {
                messageCollector.report(
                        WARNING, "Circular dependencies are only partially supported. " +
                                 "The following modules depend on each other: " + StringUtil.join(chunk.getModules(), MODULE_NAME, ", ") + ". " +
                                 "Kotlin will compile them, but some strange effect may happen",
                        NO_LOCATION);
            }

            List<File> filesToCompile = KotlinSourceFileCollector.getDirtySourceFiles(dirtyFilesHolder);
            filesToCompile.removeAll(allCompiledFiles);
            allCompiledFiles.addAll(filesToCompile);

            Set<ModuleBuildTarget> processedTargetsWithRemoved = getProcessedTargetsWithRemovedFilesContainer(context);

            boolean haveRemovedFiles = false;
            for (ModuleBuildTarget target : chunk.getTargets()) {
                if (!KotlinSourceFileCollector.getRemovedKotlinFiles(dirtyFilesHolder, target).isEmpty()) {
                    if (processedTargetsWithRemoved.add(target)) {
                        haveRemovedFiles = true;
                    }
                }
            }

            File moduleFile = KotlinBuilderModuleScriptGenerator
                    .generateModuleDescription(context, chunk, filesToCompile, haveRemovedFiles);
            if (moduleFile == null) {
                // No Kotlin sources found
                return ExitCode.NOTHING_DONE;
            }

            K2JVMCompilerArguments k2JvmArguments = JpsKotlinCompilerSettings.getK2JvmCompilerArguments(project);

            runK2JvmCompiler(commonArguments, k2JvmArguments, compilerSettings, messageCollector, environment,
                             moduleFile, outputItemCollector);
            moduleFile.delete();
        }

        // If there's only one target, this map is empty: get() always returns null, and the representativeTarget will be used below
        Map<File, BuildTarget<?>> sourceToTarget = new HashMap<File, BuildTarget<?>>();
        if (chunk.getTargets().size() > 1) {
            for (ModuleBuildTarget target : chunk.getTargets()) {
                for (File file : KotlinSourceFileCollector.getAllKotlinSourceFiles(target)) {
                    sourceToTarget.put(file, target);
                }
            }
        }

        IncrementalCacheImpl cache = new IncrementalCacheImpl(KotlinBuilderModuleScriptGenerator.getIncrementalCacheDir(context));

        try {
            List<Pair<String, File>> moduleIdsAndSourceFiles = new ArrayList<Pair<String, File>>();
            Map<String, File> outDirectories = new HashMap<String, File>();

            for (ModuleBuildTarget target : chunk.getTargets()) {
                String targetId = target.getId();
                outDirectories.put(targetId, target.getOutputDir());

                for (String file : KotlinSourceFileCollector.getRemovedKotlinFiles(dirtyFilesHolder, target)) {
                    moduleIdsAndSourceFiles.add(new Pair<String, File>(targetId, new File(file)));
                }
            }
            cache.clearCacheForRemovedFiles(moduleIdsAndSourceFiles, outDirectories);

            IncrementalCacheImpl.RecompilationDecision recompilationDecision = IncrementalCacheImpl.RecompilationDecision.DO_NOTHING;

            for (SimpleOutputItem outputItem : outputItemCollector.getOutputs()) {
                BuildTarget<?> target = null;
                Collection<File> sourceFiles = outputItem.getSourceFiles();
                if (!sourceFiles.isEmpty()) {
                    target = sourceToTarget.get(sourceFiles.iterator().next());
                }

                if (target == null) {
                    target = representativeTarget;
                }

                File outputFile = outputItem.getOutputFile();

                if (IncrementalCompilation.ENABLED) {
                    IncrementalCacheImpl.RecompilationDecision newDecision = cache.saveFileToCache(target.getId(), sourceFiles, outputFile);
                    recompilationDecision = recompilationDecision.merge(newDecision);
                }

                outputConsumer.registerOutputFile(target, outputFile, paths(sourceFiles));
            }

            if (IncrementalCompilation.ENABLED) {
                if (recompilationDecision == IncrementalCacheImpl.RecompilationDecision.RECOMPILE_ALL) {
                    allCompiledFiles.clear();
                    return ExitCode.CHUNK_REBUILD_REQUIRED;
                }
                if (recompilationDecision == IncrementalCacheImpl.RecompilationDecision.COMPILE_OTHERS) {
                    // TODO should mark dependencies as dirty, as well
                    FSOperations.markDirty(context, chunk, new FileFilter() {
                        @Override
                        public boolean accept(@NotNull File file) {
                            return !allCompiledFiles.contains(file);
                        }
                    });
                }
                return ExitCode.ADDITIONAL_PASS_REQUIRED;
            }
            else {
                return ExitCode.OK;
            }
        }
        finally {
            cache.close();
        }
    }

    private static Set<File> getAllCompiledFilesContainer(CompileContext context) {
        Set<File> allCompiledFiles = ALL_COMPILED_FILES_KEY.get(context);
        if (allCompiledFiles == null) {
            allCompiledFiles = new THashSet<File>(FileUtil.FILE_HASHING_STRATEGY);
            ALL_COMPILED_FILES_KEY.set(context, allCompiledFiles);
        }
        return allCompiledFiles;
    }

    private static Set<ModuleBuildTarget> getProcessedTargetsWithRemovedFilesContainer(CompileContext context) {
        Set<ModuleBuildTarget> set = PROCESSED_TARGETS_WITH_REMOVED_FILES.get(context);
        if (set == null) {
            set = new HashSet<ModuleBuildTarget>();
            PROCESSED_TARGETS_WITH_REMOVED_FILES.set(context, set);
        }
        return set;
    }

    private static boolean hasKotlinDirtyOrRemovedFiles(
            @NotNull DirtyFilesHolder<JavaSourceRootDescriptor, ModuleBuildTarget> dirtyFilesHolder,
            @NotNull ModuleChunk chunk
    )
            throws IOException {
        if (!KotlinSourceFileCollector.getDirtySourceFiles(dirtyFilesHolder).isEmpty()) {
            return true;
        }

        for (ModuleBuildTarget target : chunk.getTargets()) {
            if (!KotlinSourceFileCollector.getRemovedKotlinFiles(dirtyFilesHolder, target).isEmpty()) {
                return true;
            }
        }

        return false;
    }

    private static boolean isJavaPluginEnabled(@NotNull CompileContext context) {
        try {
            // Using reflection for backward compatibility with IDEA 12
            Field javaPluginIsEnabledField = JavaBuilder.class.getDeclaredField("IS_ENABLED");
            return Modifier.isPublic(javaPluginIsEnabledField.getModifiers()) ? JavaBuilder.IS_ENABLED.get(context, Boolean.TRUE) : true;
        }
        catch (NoSuchFieldException e) {
            throw new IllegalArgumentException("Cannot check if Java Jps Plugin is enabled", e);
        }
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
                prefix = INTERNAL_ERROR_PREFIX;
            }
            context.processMessage(new CompilerMessage(
                    CompilerRunnerConstants.KOTLIN_COMPILER_NAME,
                    kind(severity),
                    prefix + message + renderLocationIfNeeded(location),
                    location.getPath(),
                    -1, -1, -1,
                    location.getLine(),
                    location.getColumn()
            ));
        }

        private static String renderLocationIfNeeded(@NotNull CompilerMessageLocation location) {
            if (location == NO_LOCATION) return "";

            // Sometimes we report errors in JavaScript library stubs, i.e. files like core/javautil.kt
            // IDEA can't find these files, and does not display paths in Messages View, so we add the position information
            // to the error message itself:
            String pathname = String.valueOf(location.getPath());
            return new File(pathname).exists() ? "" : " (" + location + ")";
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

    @Override
    public List<String> getCompilableFileExtensions() {
        return COMPILABLE_FILE_EXTENSIONS;
    }

    private class MyClassLoader extends ClassLoader {
        private final ClassLoader compilerClassLoader;
        private final ClassLoader jpsPluginClassLoader = KotlinBuilder.this.getClass().getClassLoader();

        private MyClassLoader(ClassLoader compilerClassLoader) {
            this.compilerClassLoader = compilerClassLoader;
        }

        @NotNull
        @Override
        public Enumeration<URL> getResources(String name) throws IOException {
            return jpsPluginClassLoader.getResources(name);
        }

        @Override
        public Class<?> loadClass(@NotNull String name) throws ClassNotFoundException {
            if (name.startsWith("org.jetbrains.jet.jps.incremental.")) {
                return loadClassFromBytes(name);
            }
            else if (name.startsWith("org.jetbrains.jet.lang.resolve.kotlin.incremental.")) {
                return compilerClassLoader.loadClass(name);
            }
            else {
                return jpsPluginClassLoader.loadClass(name);
            }
        }

        private Class<?> loadClassFromBytes(String name) throws ClassNotFoundException {
            String classResource = name.replace('.', '/') + ".class";
            InputStream resource = jpsPluginClassLoader.getResourceAsStream(classResource);
            if (resource == null) {
                return null;
            }
            byte[] bytes;
            try {
                bytes = StreamUtil.loadFromStream(resource);
            }
            catch (IOException e) {
                throw new ClassNotFoundException("Couldn't load class " + name, e);
            }

            return defineClass(name, bytes, 0, bytes.length);
        }
    }
}
