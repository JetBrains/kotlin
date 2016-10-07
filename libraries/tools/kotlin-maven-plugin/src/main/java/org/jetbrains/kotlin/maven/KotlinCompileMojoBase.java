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

package org.jetbrains.kotlin.maven;

import com.google.common.base.Joiner;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Processor;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.cli.common.CLICompiler;
import org.jetbrains.kotlin.cli.common.ExitCode;
import org.jetbrains.kotlin.cli.common.KotlinVersion;
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments;
import org.jetbrains.kotlin.config.Services;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class KotlinCompileMojoBase<A extends CommonCompilerArguments> extends AbstractMojo {
    @Component
    protected PlexusContainer container;

    @Component
    protected MojoExecution mojoExecution;

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
     * A classpaths required for kotlin compiler plugin(s). Useful if you don't use extensions due to some reason
     */
    @Parameter
    private List<String> compilerPluginsClassPaths;

    /**
     * A list of plugin options in format plugin:(pluginId):(parameter)=(value)
     */
    @Parameter
    private List<String> pluginArguments;

    protected List<String> getSourceFilePaths() {
        if (sourceDirs != null && !sourceDirs.isEmpty()) return sourceDirs;
        return project.getCompileSourceRoots();
    }

    public List<File> getSourceDirs() {
        List<String> sources = getSourceFilePaths();
        List<File> result = new ArrayList<File>(sources.size());
        File baseDir = project.getBasedir();

        for (String source : sources) {
            File f = new File(source);
            if (f.isAbsolute()) {
                result.add(f);
            } else {
                result.add(new File(baseDir, source));
            }
        }

        return result;
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
    @Parameter
    public String module;

    /**
     * Kotlin compilation module, as alternative to source files or folders (for tests).
     */
    @Parameter
    public String testModule;

    /**
     * Additional command line arguments for Kotlin compiler.
     */
    @Parameter
    public List<String> args;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        getLog().info("Kotlin Compiler version " + KotlinVersion.VERSION);

        if (!hasKotlinFilesInSources()) {
            getLog().warn("No sources found skipping Kotlin compile");
            return;
        }

        A arguments = createCompilerArguments();
        CLICompiler<A> compiler = createCompiler();

        configureCompilerArguments(arguments, compiler);
        printCompilerArgumentsIfDebugEnabled(arguments, compiler);

        MavenPluginLogMessageCollector messageCollector = new MavenPluginLogMessageCollector(getLog());

        ExitCode exitCode = compiler.exec(messageCollector, Services.EMPTY, arguments);

        if (exitCode != ExitCode.OK) {
            messageCollector.throwKotlinCompilerException();
        }
    }

    private boolean hasKotlinFilesInSources() throws MojoExecutionException {
        List<File> sources = getSourceDirs();
        if (sources == null || sources.isEmpty()) return false;

        for (File root : sources) {
            if (root.exists()) {
                boolean sourcesExists = !FileUtil.processFilesRecursively(root, new Processor<File>() {
                    @Override
                    public boolean process(File file) {
                        return !file.getName().endsWith(".kt");
                    }
                });
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

    protected abstract void configureSpecificCompilerArguments(@NotNull A arguments) throws MojoExecutionException;

    protected List<String> getCompilerPluginsClassPaths() {
        ArrayList<String> result = new ArrayList<String>();

        if (compilerPluginsClassPaths != null) {
            result.addAll(compilerPluginsClassPaths);
        }

        // TODO do that once we have compiler running in the separate process
//        URL[] urls = mojoExecution.getMojoDescriptor().getRealm().getURLs();
//        for (URL url : urls) {
//            if ("file".equals(url.getProtocol())) {
//                try {
//                    result.add(new File(url.toURI()).getAbsolutePath());
//                } catch (URISyntaxException ignore) {
//                }
//            }
//        }

        return result;
    }

    protected List<String> getCompilerPluginArguments() throws ComponentLookupException {
        return configureCompilerPlugins();
    }

    private List<String> configureCompilerPlugins() throws ComponentLookupException {
        if (mojoExecution == null) {
            throw new IllegalStateException("No mojoExecution injected");
        }

        List<String> pluginArguments = new ArrayList<String>();

        if (this.pluginArguments != null) {
            pluginArguments.addAll(this.pluginArguments);
        }

        if (compilerPlugins != null) {
            for (String pluginId : compilerPlugins) {
                getLog().debug("Looking for plugin " + pluginId);
                KotlinMavenPluginExtension extension = container.lookup(KotlinMavenPluginExtension.class, pluginId);
                getLog().debug("Got plugin instance" + pluginId + " of type " + extension.getClass().getName());

                if (extension.isApplicable(project, mojoExecution)) {
                    getLog().info("Applying plugin " + pluginId);
                    pluginArguments.addAll(extension.getPluginArguments(project, mojoExecution));
                    // TODO here we can use artifact resolver to build exact dependency tree

                    container.getComponentDescriptor(KotlinMavenPluginExtension.class.getName(), pluginId).getRealm();
                }
            }
        }

        return pluginArguments;
    }

    private void configureCompilerArguments(@NotNull A arguments, @NotNull CLICompiler<A> compiler) throws MojoExecutionException {
        if (getLog().isDebugEnabled()) {
            arguments.verbose = true;
        }

        List<String> sources = new ArrayList<String>();
        for (File source : getSourceDirs()) {
            if (source.exists()) {
                sources.add(source.getPath());
            }
            else {
                getLog().warn("Source root doesn't exist: " + source);
            }
        }

        if (sources.isEmpty()) {
            throw new MojoExecutionException("No source roots to compile");
        }

        arguments.suppressWarnings = nowarn;

        getLog().info("Compiling Kotlin sources from " + sources);

        configureSpecificCompilerArguments(arguments);

        try {
            compiler.parseArguments(ArrayUtil.toStringArray(args), arguments);
        }
        catch (IllegalArgumentException e) {
            throw new MojoExecutionException(e.getMessage());
        }

        arguments.freeArgs.addAll(sources);

        if (arguments.noInline) {
            getLog().info("Method inlining is turned off");
        }

        try {
            List<String> pluginArguments = getCompilerPluginArguments();
            if (pluginArguments != null && !pluginArguments.isEmpty()) {
                if (getLog().isDebugEnabled()) {
                    getLog().debug("Plugin options are: " + Joiner.on(", ").join(pluginArguments));
                }

                arguments.pluginOptions = pluginArguments.toArray(new String[pluginArguments.size()]);
            }
        } catch (ComponentLookupException e) {
            throw new MojoExecutionException("Failed to lookup kotlin compiler plugins", e);
        }

        List<String> classPaths = getCompilerPluginsClassPaths();

        if (classPaths != null && !classPaths.isEmpty()) {
            if (getLog().isDebugEnabled()) {
                getLog().debug("Plugin classpaths are: " + Joiner.on(", ").join(classPaths));
            }
            arguments.pluginClasspaths = classPaths.toArray(new String[classPaths.size()]);
        }
    }
}
