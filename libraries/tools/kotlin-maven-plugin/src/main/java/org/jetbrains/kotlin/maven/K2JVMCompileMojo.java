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

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ArrayUtil;
import com.sampullara.cli.Args;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments;
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static com.intellij.openapi.util.text.StringUtil.join;

/**
 * Compiles kotlin sources
 *
 * @goal compile
 * @phase compile
 * @requiresDependencyResolution compile
 * @noinspection UnusedDeclaration
 */
public class K2JVMCompileMojo extends KotlinCompileMojoBase<K2JVMCompilerArguments> {
    /**
     * The directories used to scan for annotation.xml files for Kotlin annotations
     *
     * @parameter
     */
    public List<String> annotationPaths;

    /**
     * @parameter default-value="true"
     */
    public boolean scanForAnnotations;

    /**
     * Project classpath.
     *
     * @parameter default-value="${project.compileClasspathElements}"
     * @required
     * @readonly
     */
    public List<String> classpath;

    /**
     * Project test classpath.
     *
     * @parameter default-value="${project.testClasspathElements}"
     * @required
     * @readonly
     */
    protected List<String> testClasspath;

    @NotNull
    @Override
    protected K2JVMCompiler createCompiler() {
        return new K2JVMCompiler();
    }

    @NotNull
    @Override
    protected K2JVMCompilerArguments createCompilerArguments() {
        return new K2JVMCompilerArguments();
    }

    @Override
    protected void configureSpecificCompilerArguments(@NotNull K2JVMCompilerArguments arguments) throws MojoExecutionException {
        LOG.info("Classes directory is " + output);
        arguments.destination = output;

        // don't include runtime, it should be in maven dependencies
        arguments.noStdlib = true;

        if (module != null) {
            LOG.info("Compiling Kotlin module " + module);
            arguments.module = module;
        }

        ArrayList<String> classpathList = new ArrayList<String>(classpath);

        if (!classpathList.isEmpty()) {
            String classPathString = join(classpathList, File.pathSeparator);
            LOG.info("Classpath: " + classPathString);
            arguments.classpath = classPathString;
        }

        LOG.info("Classes directory is " + output);
        arguments.destination = output;

        arguments.noJdkAnnotations = true;
        arguments.annotations = getFullAnnotationsPath(LOG, annotationPaths);
        LOG.info("Using kotlin annotations from " + arguments.annotations);

        try {
            Args.parse(arguments, ArrayUtil.toStringArray(args));
        }
        catch (IllegalArgumentException e) {
            throw new MojoExecutionException(e.getMessage());
        }

        if (arguments.noOptimize) {
            LOG.info("Optimization is turned off");
        }
    }

    protected String getFullAnnotationsPath(Log log, List<String> annotations) {
        String jdkAnnotation = getJdkAnnotations().getPath();

        List<String> list = new ArrayList<String>();
        list.add(jdkAnnotation);

        if (annotations != null) {
            for (String annotationPath : annotations) {
                if (new File(annotationPath).exists()) {
                    list.add(annotationPath);
                } else {
                    log.info("annotation path " + annotationPath + " does not exist");
                }
            }
        }

        if (scanForAnnotations) {
            for (String path : scanAnnotations(log)) {
                if (!list.contains(path)) {
                    list.add(path);
                }
            }
        }

        return join(list, File.pathSeparator);
    }

    @NotNull
    private static File getJdkAnnotations() {
        ClassLoader classLoader = KotlinCompileMojoBase.class.getClassLoader();
        if (!(classLoader instanceof URLClassLoader)) {
            throw new RuntimeException("Kotlin plugin`s class loader is not URLClassLoader");
        }

        for (URL url : ((URLClassLoader) classLoader).getURLs()) {
            String path = url.getPath();
            if (StringUtil.isEmpty(path)) {
                continue;
            }

            File file = new File(path);
            if (file.getName().startsWith("kotlin-jdk-annotations")) {
                return file;
            }
        }

        throw new RuntimeException("Could not get jdk annotations from Kotlin plugin`s classpath");
    }

    private List<String> scanAnnotations(Log log) {
        List<String> annotations = new ArrayList<String>();

        Set<Artifact> artifacts = project.getArtifacts();
        for (Artifact artifact : artifacts) {
            File file = artifact.getFile();
            if (file.isFile() && containsAnnotations(file, log)) {
                log.info("Discovered kotlin annotations in: " + file);
                try {
                    annotations.add(file.getCanonicalPath());
                }
                catch (IOException e) {
                    log.warn("Error extracting canonical path from: " + file, e);
                }
            }
        }

        return annotations;
    }

    private static boolean containsAnnotations(File file, Log log) {
        log.debug("Scanning for kotlin annotations in " + file);

        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(file);

            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (name.endsWith("/annotations.xml")) {
                    return true;
                }
            }
        }
        catch (IOException e) {
            log.warn("Error reading contents of jar: " + file, e);
        }
        finally {
            if (zipFile != null) {
                try {
                    zipFile.close();
                }
                catch (IOException e) {
                    log.warn("Error closing: " + zipFile, e);
                }
            }
        }
        return false;
    }
}

