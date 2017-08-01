/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.maven;

import com.google.common.base.Joiner;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.ArrayUtil;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.cli.common.CLICompiler;
import org.jetbrains.kotlin.cli.common.ExitCode;
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments;
import org.jetbrains.kotlin.cli.common.messages.MessageCollector;
import org.jetbrains.kotlin.config.KotlinCompilerVersion;
import org.jetbrains.kotlin.config.Services;

import java.io.File;
import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class KotlinCompileMojoBase<A extends CommonCompilerArguments> extends AbstractMojo {
    @Component
    protected PlexusContainer container;

    @Component
    protected MojoExecution mojoExecution;

    @Component
    protected RepositorySystem system;

    /**
     * The source directories containing the sources to be compiled.
     */
    @Parameter
    private List<String> sourceDirs;

    /**
     * A list of kotlin compiler plugins to be applied.
     */
    @Parameter
    private List<String> compilerPlugins;

    /**
     * A list of plugin options in format (pluginId):(parameter)=(value)
     */
    @Parameter
    private List<String> pluginOptions;

    @Parameter
    private boolean multiPlatform = false;

    protected List<String> getSourceFilePaths() {
        if (sourceDirs != null && !sourceDirs.isEmpty()) return sourceDirs;
        return project.getCompileSourceRoots();
    }

    @NotNull
    private List<File> getSourceDirs() {
        List<String> sources = getSourceFilePaths();
        List<File> result = new ArrayList<>(sources.size());

        for (String source : sources) {
            addSourceRoots(result, source);
        }

        Map<String, MavenProject> projectReferences = project.getProjectReferences();
        if (projectReferences != null) {
            iterateDependencies:
            for (Dependency dependency : project.getDependencies()) {
                MavenProject sibling = projectReferences.get(dependency.getGroupId() + ":" + dependency.getArtifactId() + ":" + dependency.getVersion());
                if (sibling != null) {
                    Plugin plugin = sibling.getPlugin("org.jetbrains.kotlin:kotlin-maven-plugin");
                    if (plugin != null) {
                        for (PluginExecution pluginExecution : plugin.getExecutions()) {
                            if (pluginExecution.getGoals() != null && pluginExecution.getGoals().contains("metadata")) {
                                for (String sourceRoot : orEmpty(getRelatedSourceRoots(sibling))) {
                                    addSourceRoots(result, sourceRoot);
                                    continue iterateDependencies;
                                }
                            }
                        }
                    }
                }
            }
        }

        return result;
    }

    protected abstract List<String> getRelatedSourceRoots(MavenProject project);

    private void addSourceRoots(List<File> result, String source) {
        File f = new File(source);
        if (f.isAbsolute()) {
            result.add(f);
        } else {
            result.add(new File(project.getBasedir(), source));
        }
    }

    /**
     * Suppress all warnings.
     */
    @Parameter(defaultValue = "false")
    public boolean nowarn;

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    public MavenProject project;

    /**
     * The directory for compiled classes.
     */
    @Parameter(defaultValue = "${project.build.outputDirectory}", required = true, readonly = true)
    public String output;

    /**
     * The directory for compiled tests classes.
     */
    @Parameter(defaultValue = "${project.build.testOutputDirectory}", required = true, readonly = true)
    public String testOutput;

    /**
     * Kotlin compilation module, as alternative to source files or folders.
     */
    @Parameter(readonly = true)
    @Deprecated
    public String module;

    /**
     * Kotlin compilation module, as alternative to source files or folders (for tests).
     */
    @Parameter(readonly = true)
    @Deprecated
    public String testModule;


    @Parameter(property = "kotlin.compiler.languageVersion")
    protected String languageVersion;


    @Parameter(property = "kotlin.compiler.apiVersion")
    protected String apiVersion;

    /**
     * possible values are: enable, error, warn
     */
    @Parameter(property = "kotlin.compiler.experimental.coroutines")
    @Nullable
    protected String experimentalCoroutines;

    /**
     * Additional command line arguments for Kotlin compiler.
     */
    @Parameter
    public List<String> args;

    private final static Pattern OPTION_PATTERN = Pattern.compile("([^:]+):([^=]+)=(.*)");

    static {
        if (System.getProperty("kotlin.compiler.X.enable.idea.logger") != null) {
            Logger.setFactory(IdeaCoreLoggerFactory.class);
        }
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Kotlin version " + KotlinCompilerVersion.VERSION +
                " (JRE " + System.getProperty("java.runtime.version") + ")");

        if (!hasKotlinFilesInSources()) {
            getLog().warn("No sources found skipping Kotlin compile");
            return;
        }

        A arguments = createCompilerArguments();
        CLICompiler<A> compiler = createCompiler();

        List<File> sourceRoots = getSourceRoots();

        configureCompilerArguments(arguments, compiler, sourceRoots);
        printCompilerArgumentsIfDebugEnabled(arguments, compiler);

        MavenPluginLogMessageCollector messageCollector = new MavenPluginLogMessageCollector(getLog());

        ExitCode exitCode = execCompiler(compiler, messageCollector, arguments, sourceRoots);

        if (exitCode != ExitCode.OK) {
            messageCollector.throwKotlinCompilerException();
        }
    }

    @NotNull
    protected ExitCode execCompiler(
            CLICompiler<A> compiler,
            MessageCollector messageCollector,
            A arguments,
            List<File> sourceRoots
    ) throws MojoExecutionException {
        for (File sourceRoot : sourceRoots) {
            arguments.freeArgs.add(sourceRoot.getPath());
        }
        return compiler.exec(messageCollector, Services.EMPTY, arguments);
    }

    private boolean hasKotlinFilesInSources() throws MojoExecutionException {
        for (File root : getSourceDirs()) {
            if (root.exists()) {
                boolean sourcesExists = !FileUtil.processFilesRecursively(root, file -> !file.getName().endsWith(".kt"));
                if (sourcesExists) return true;
            }
        }

        return false;
    }

    private void printCompilerArgumentsIfDebugEnabled(@NotNull A arguments, @NotNull CLICompiler<A> compiler) {
        if (getLog().isDebugEnabled()) {
            getLog().debug("Invoking compiler " + compiler + " with arguments:");
            try {
                Field[] fields = arguments.getClass().getFields();
                for (Field f : fields) {
                    Object value = f.get(arguments);

                    String valueString;
                    if (value instanceof Object[]) {
                        valueString = Arrays.deepToString((Object[]) value);
                    }
                    else if (value != null) {
                        valueString = String.valueOf(value);
                    }
                    else {
                        valueString = "(null)";
                    }

                    getLog().debug(f.getName() + "=" + valueString);
                }
                getLog().debug("End of arguments");
            }
            catch (Exception e) {
                getLog().warn("Failed to print compiler arguments: " + e, e);
            }
        }
    }

    @NotNull
    protected abstract CLICompiler<A> createCompiler();

    @NotNull
    protected abstract A createCompilerArguments();

    protected abstract void configureSpecificCompilerArguments(@NotNull A arguments, @NotNull List<File> sourceRoots) throws MojoExecutionException;

    private List<String> getCompilerPluginClassPaths() {
        ArrayList<String> result = new ArrayList<>();

        List<File> files = new ArrayList<>();

        for (Dependency dependency : mojoExecution.getPlugin().getDependencies()) {
            Artifact artifact = system.createDependencyArtifact(dependency);
            ArtifactResolutionResult resolved = system.resolve(new ArtifactResolutionRequest().setArtifact(artifact));

            for (Artifact resolvedArtifact : resolved.getArtifacts()) {
                File file = resolvedArtifact.getFile();

                if (file != null && file.exists()) {
                    files.add(file);
                }
            }
        }

        for (File file : files) {
            result.add(file.getAbsolutePath());
        }

        return result;
    }

    @NotNull
    private Map<String, KotlinMavenPluginExtension> loadCompilerPlugins() throws PluginNotFoundException {
        if (compilerPlugins == null) return Collections.emptyMap();

        Map<String, KotlinMavenPluginExtension> loadedPlugins = new HashMap<>();
        for (String pluginName : compilerPlugins) {
            getLog().debug("Looking for plugin " + pluginName);
            try {
                KotlinMavenPluginExtension extension = container.lookup(KotlinMavenPluginExtension.class, pluginName);
                loadedPlugins.put(pluginName, extension);
                getLog().debug("Got plugin instance" + pluginName + " of type " + extension.getClass().getName());
            } catch (ComponentLookupException e) {
                getLog().debug("Unable to get plugin instance" + pluginName);
                throw new PluginNotFoundException(pluginName, e);
            }
        }
        return loadedPlugins;
    }

    @NotNull
    private List<String> renderCompilerPluginOptions(@NotNull List<PluginOption> options) {
        List<String> renderedOptions = new ArrayList<>(options.size());
        for (PluginOption option : options) {
            renderedOptions.add(option.toString());
        }
        return renderedOptions;
    }

    @NotNull
    private List<PluginOption> getCompilerPluginOptions() throws PluginNotFoundException, PluginOptionIllegalFormatException {
        if (mojoExecution == null) {
            throw new IllegalStateException("No mojoExecution injected");
        }

        List<PluginOption> pluginOptions = new ArrayList<>();

        Map<String, KotlinMavenPluginExtension> plugins = loadCompilerPlugins();

        // Get options for extension-provided compiler plugins

        for (Map.Entry<String, KotlinMavenPluginExtension> pluginEntry : plugins.entrySet()) {
            String pluginName = pluginEntry.getKey();
            KotlinMavenPluginExtension plugin = pluginEntry.getValue();

            // applied plugin (...) to info()
            if (plugin.isApplicable(project, mojoExecution)) {
                getLog().info("Applied plugin: '" + pluginName + "'");
                List<PluginOption> optionsForPlugin = plugin.getPluginOptions(project, mojoExecution);
                if (!optionsForPlugin.isEmpty()) {
                    pluginOptions.addAll(optionsForPlugin);
                }
            }
        }

        if (this.pluginOptions != null) {
            pluginOptions.addAll(parseUserProvidedPluginOptions(this.pluginOptions, plugins));
        }

        Map<String, List<PluginOption>> optionsByPluginName = new LinkedHashMap<>();
        for (PluginOption option : pluginOptions) {
            optionsByPluginName.computeIfAbsent(option.pluginName, key -> new ArrayList<>()).add(option);
        }

        for (Map.Entry<String, List<PluginOption>> entry : optionsByPluginName.entrySet()) {
            assert !entry.getValue().isEmpty();

            String pluginName = entry.getValue().get(0).pluginName;

            StringBuilder renderedOptions = new StringBuilder("[");
            for (PluginOption option : entry.getValue()) {
                if (renderedOptions.length() > 1) {
                    renderedOptions.append(", ");
                }
                renderedOptions.append(option.key).append(": ").append(option.value);
            }
            renderedOptions.append("]");

            getLog().debug("Options for plugin " + pluginName + ": " + renderedOptions);
        }

        return pluginOptions;
    }

    @NotNull
    private static List<PluginOption> parseUserProvidedPluginOptions(
            @NotNull List<String> rawOptions,
            @NotNull Map<String, KotlinMavenPluginExtension> plugins
    ) throws PluginOptionIllegalFormatException, PluginNotFoundException {
        List<PluginOption> pluginOptions = new ArrayList<>(rawOptions.size());

        for (String rawOption : rawOptions) {
            Matcher matcher = OPTION_PATTERN.matcher(rawOption);
            if (!matcher.matches()) {
                throw new PluginOptionIllegalFormatException(rawOption);
            }

            String pluginName = matcher.group(1);
            String key = matcher.group(2);
            String value = matcher.group(3);
            KotlinMavenPluginExtension plugin = plugins.get(pluginName);
            if (plugin == null) {
                throw new PluginNotFoundException(pluginName);
            }

            pluginOptions.add(new PluginOption(pluginName, plugin.getCompilerPluginId(), key, value));
        }

        return pluginOptions;
    }

    @NotNull
    private List<File> getSourceRoots() throws MojoExecutionException {
        List<File> sourceRoots = new ArrayList<>();
        for (File sourceDir : getSourceDirs()) {
            if (sourceDir.exists()) {
                sourceRoots.add(sourceDir);
            }
            // unfortunately there is no good way to detect generated sources directory so we simply keep hardcoded value
            else if (!sourceDir.getPath().contains("generated-sources")) {
                getLog().warn("Source root doesn't exist: " + sourceDir);
            }
        }
        if (sourceRoots.isEmpty()) {
            throw new MojoExecutionException("No source roots to compile");
        }
        getLog().info("Compiling Kotlin sources from " + sourceRoots);
        return sourceRoots;
    }

    private void configureCompilerArguments(@NotNull A arguments, @NotNull CLICompiler<A> compiler, @NotNull List<File> sourceRoots) throws MojoExecutionException {
        if (getLog().isDebugEnabled()) {
            arguments.verbose = true;
        }

        arguments.suppressWarnings = nowarn;
        arguments.languageVersion = languageVersion;
        arguments.apiVersion = apiVersion;
        arguments.multiPlatform = multiPlatform;

        if (experimentalCoroutines != null) {
            arguments.coroutinesState = experimentalCoroutines;
        }

        configureSpecificCompilerArguments(arguments, sourceRoots);

        try {
            compiler.parseArguments(ArrayUtil.toStringArray(args), arguments);
        }
        catch (IllegalArgumentException e) {
            throw new MojoExecutionException(e.getMessage());
        }

        if (arguments.noInline) {
            getLog().info("Method inlining is turned off");
        }

        List<String> pluginClassPaths = getCompilerPluginClassPaths();
        if (pluginClassPaths != null && !pluginClassPaths.isEmpty()) {
            if (arguments.pluginClasspaths == null || arguments.pluginClasspaths.length == 0) {
                arguments.pluginClasspaths = pluginClassPaths.toArray(new String[pluginClassPaths.size()]);
            } else {
                for (String path : pluginClassPaths) {
                    arguments.pluginClasspaths = ArrayUtil.append(arguments.pluginClasspaths, path);
                }
            }

            if (getLog().isDebugEnabled()) {
                getLog().debug("Plugin classpaths are: " + Joiner.on(", ").join(arguments.pluginClasspaths));
            }

        }

        List<String> pluginArguments;
        try {
            pluginArguments = renderCompilerPluginOptions(getCompilerPluginOptions());
        } catch (PluginNotFoundException | PluginOptionIllegalFormatException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }

        if (!pluginArguments.isEmpty()) {
            if (getLog().isDebugEnabled()) {
                getLog().debug("Plugin options are: " + Joiner.on(", ").join(pluginArguments));
            }

            arguments.pluginOptions = pluginArguments.toArray(new String[pluginArguments.size()]);
        }
    }

    public static class PluginNotFoundException extends Exception {
        PluginNotFoundException(String pluginId, Throwable cause) {
            super("Plugin not found: " + pluginId, cause);
        }

        PluginNotFoundException(String pluginId) {
            super("Plugin not found: " + pluginId);
        }
    }

    public static class PluginOptionIllegalFormatException extends Exception {
        PluginOptionIllegalFormatException(String option) {
            super("Plugin option has an illegal format: " + option);
        }
    }

    @NotNull
    private static <T> List<T> orEmpty(@Nullable List<T> in) {
        if (in == null) {
            return Collections.emptyList();
        }

        return in;
    }
}
