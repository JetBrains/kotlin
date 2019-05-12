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

import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import kotlin.script.experimental.jvm.JvmScriptingHostConfigurationKt;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.repository.ComponentDependency;
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys;
import org.jetbrains.kotlin.cli.common.config.KotlinSourceRoot;
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinToJVMBytecodeCompiler;
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot;
import org.jetbrains.kotlin.codegen.GeneratedClassLoader;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar;
import org.jetbrains.kotlin.config.CommonConfigurationKeys;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.KtScript;
import org.jetbrains.kotlin.scripting.compiler.plugin.ScriptingCompilerConfigurationComponentRegistrar;
import org.jetbrains.kotlin.scripting.configuration.ConfigurationKt;
import org.jetbrains.kotlin.utils.ParametersMapKt;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Allows to execute kotlin script files during the build process.
 * You can specify script file or inline script to be executed.
 * <br/>
 * Scripts have access to the build information.
 * When compiling, kotlin maven plugin jar and it's dependencies
 * (including core maven libraries) are added to classpath.
 * Before execution this mojo is exposed to the script via static variable
 * and maven project is stored in mojo's project field for script access.
 * <br/>
 * <pre><code>
 *     import org.jetbrains.kotlin.maven.ExecuteKotlinScriptMojo
 *     val mojo = ExecuteKotlinScriptMojo.INSTANCE
 *     mojo.getLog().info("kotlin build script accessing build info of ${mojo.project.artifactId} project")
 * </code></pre>
 */
@Mojo(name = "script", requiresDependencyResolution = ResolutionScope.COMPILE, threadSafe = true)
public class ExecuteKotlinScriptMojo extends AbstractMojo {
    /**
     * The Kotlin script file to be executed.
     * Either this or {@code script} parameter must be specified.
     */
    @Parameter
    private File scriptFile;

    /**
     * The inline Kotlin script to be executed.
     * Either this or {@code scriptFile} parameter must be specified.
     */
    @Parameter
    private String script;

    /**
     * The content of inline scripts is temporarily stored here.
     */
    @Parameter(defaultValue = "${project.build.directory}/kotlin-build-scripts", required = true)
    private File buildDirectory;

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    public MavenProject project;

    @Parameter(defaultValue = "${plugin}", required = true, readonly = true)
    private PluginDescriptor plugin;

    @Parameter(defaultValue = "${localRepository}", required = true, readonly = true)
    private ArtifactRepository localRepository;

    @Parameter(property = "kotlin.compiler.scriptTemplates")
    protected List<String> scriptTemplates;

    @Parameter(property = "kotlin.compiler.scriptArguments")
    protected List<String> scriptArguments;

    @Parameter(property = "kotlin.compiler.scriptClasspath")
    protected List<String> scriptClasspath;

    @Component
    private ArtifactHandlerManager artifactHandlerManager;

    public static ExecuteKotlinScriptMojo INSTANCE;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (scriptFile != null && script == null) {
            executeScriptFile(scriptFile);
        } else if (scriptFile == null && script != null) {
            executeScriptInline();
        } else {
            throw new MojoExecutionException("Either scriptFile or script parameter must be specified");
        }
    }

    private void executeScriptInline() throws MojoExecutionException {
        try {
            if (!buildDirectory.exists()) {
                buildDirectory.mkdirs();
            }

            File scriptFile = File.createTempFile("kotlin-maven-plugin-inline-script-", ".tmp.kts", buildDirectory);
            FileOutputStream stream = new FileOutputStream(scriptFile);
            stream.write(script.getBytes("UTF-8"));
            stream.close();

            try {
                executeScriptFile(scriptFile);
            } finally {
                boolean deleted = scriptFile.delete();
                if (!deleted) {
                    getLog().warn("Error deleting " + scriptFile.getAbsolutePath());
                }
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Error executing inline script", e);
        }
    }

    private void executeScriptFile(File scriptFile) throws MojoExecutionException {
        initCompiler();

        Disposable rootDisposable = Disposer.newDisposable();

        try {
            MavenPluginLogMessageCollector messageCollector = new MavenPluginLogMessageCollector(getLog());

            CompilerConfiguration configuration = new CompilerConfiguration();

            configuration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector);

            configuration.add(ComponentRegistrar.Companion.getPLUGIN_COMPONENT_REGISTRARS(),
                              new ScriptingCompilerConfigurationComponentRegistrar());

            List<File> deps = new ArrayList<>();

            deps.addAll(getDependenciesForScript());

            for (File item: deps) {
                if (item.exists()) {
                    configuration.add(CLIConfigurationKeys.CONTENT_ROOTS, new JvmClasspathRoot(item));
                    getLog().debug("Adding to classpath: " + item.getAbsolutePath());
                } else {
                    getLog().debug("Skipping non-existing dependency: " + item.getAbsolutePath());
                }
            }

            configuration.add(CLIConfigurationKeys.CONTENT_ROOTS, new KotlinSourceRoot(scriptFile.getAbsolutePath(), false));
            configuration.put(CommonConfigurationKeys.MODULE_NAME, JvmProtoBufUtil.DEFAULT_MODULE_NAME);

            ConfigurationKt.configureScriptDefinitions(
                    scriptTemplates, configuration, this.getClass().getClassLoader(), messageCollector,
                    JvmScriptingHostConfigurationKt.getDefaultJvmScriptingHostConfiguration()
            );

            KotlinCoreEnvironment environment = KotlinCoreEnvironment.createForProduction(rootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES);

            GenerationState state = KotlinToJVMBytecodeCompiler.INSTANCE.analyzeAndGenerate(environment);

            if (state == null) {
                throw new ScriptExecutionException(scriptFile, "compile error");
            }

            GeneratedClassLoader classLoader = new GeneratedClassLoader(state.getFactory(), getClass().getClassLoader());
            KtScript script = environment.getSourceFiles().get(0).getScript();
            FqName nameForScript = script.getFqName();

            try {
                Class<?> klass = classLoader.loadClass(nameForScript.asString());
                ExecuteKotlinScriptMojo.INSTANCE = this;
                if (ParametersMapKt.tryConstructClassFromStringArgs(klass, scriptArguments) == null)
                    throw new ScriptExecutionException(scriptFile, "unable to construct script");
            } catch (ClassNotFoundException e) {
                throw new ScriptExecutionException(scriptFile, "internal error", e);
            }
        }
        finally {
            rootDisposable.dispose();
            ExecuteKotlinScriptMojo.INSTANCE = null;
        }
    }

    private List<File> getDependenciesForScript() throws MojoExecutionException {
        List<File> deps = new ArrayList<>();

        deps.addAll(getKotlinRuntimeDependencies());
        deps.add(getThisPluginAsDependency());

        deps.addAll(getThisPluginDependencies());

        for (String cp: scriptClasspath) {
            deps.add(new File(cp));
        }

        return deps;
    }

    private File getDependencyFile(ComponentDependency dep) {
        ArtifactHandler artifactHandler = artifactHandlerManager.getArtifactHandler(dep.getType());
        Artifact artifact = new DefaultArtifact(dep.getGroupId(), dep.getArtifactId(), dep.getVersion(), null, dep.getType(), null, artifactHandler);
        return getArtifactFile(artifact);
    }

    private File getArtifactFile(Artifact artifact) {
        localRepository.find(artifact);
        return artifact.getFile();
    }

    private List<File> getThisPluginDependencies() {
        return plugin.getDependencies().stream().map(this::getDependencyFile).collect(Collectors.toList());
    }

    private File getThisPluginAsDependency() {
        ComponentDependency dep = new ComponentDependency();

        dep.setGroupId(plugin.getGroupId());
        dep.setArtifactId(plugin.getArtifactId());
        dep.setVersion(plugin.getVersion());

        return getDependencyFile(dep);
    }

    private List<File> getKotlinRuntimeDependencies() throws MojoExecutionException {
        Artifact stdlibDep = null;
        Artifact runtimeDep = null;

        ArrayList<File> files = new ArrayList<>(2);

        for (Artifact dep: project.getArtifacts()) {
            if (dep.getArtifactId().equals("kotlin-stdlib")) {
                files.add(getArtifactFile(dep));
                stdlibDep = dep;
            }
            if (dep.getArtifactId().equals("kotlin-runtime")) {
                files.add(getArtifactFile(dep));
                runtimeDep = dep;
            }
            if (stdlibDep != null && runtimeDep != null) break;
        }

        if (stdlibDep == null) {
            throw new MojoExecutionException("Unable to find kotlin-stdlib artifacts among project dependencies");
        }

        return files;
    }

    private void initCompiler() {
        // execute static init of CLICompiler, had warnings without it
        // WARN: Failed to initialize native filesystem for Windows
        // java.lang.RuntimeException: Could not find installation home path. Please make sure bin/idea.properties is present in the installation directory.
        //     at com.intellij.openapi.application.PathManager.getHomePath(PathManager.java:96)
        new K2JVMCompiler();
    }
}
