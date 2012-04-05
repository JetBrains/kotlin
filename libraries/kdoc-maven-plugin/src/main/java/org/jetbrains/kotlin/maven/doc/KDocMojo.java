/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.kotlin.maven.doc;

import org.apache.maven.plugin.MojoExecutionException;
import org.jetbrains.jet.cli.CompilerArguments;
import org.jetbrains.jet.cli.KotlinCompiler;
import org.jetbrains.kotlin.doc.KDocArguments;
import org.jetbrains.kotlin.doc.KDocCompiler;
import org.jetbrains.kotlin.doc.KDocConfig;
import org.jetbrains.kotlin.maven.KotlinCompileMojoBase;

import java.util.List;

/**
 * Generates API docs documentation for kotlin sources
 *
 * @goal apidoc
 * @phase site
 * @requiresDependencyResolution test
 * @noinspection UnusedDeclaration
 */
public class KDocMojo extends KotlinCompileMojoBase {

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

    /**
     * The directory for compiled apidoc classes.
     *
     * @parameter default-value="${project.build.outputDirectory}"
     * @required
     * @readonly
     */
    public String output;


    // TODO not sure why default is stopping us passing this value in via a config
    // default-value="${project.compileSourceRoots}"

    /**
     * The sources used to compile
     *
     * @parameter expression="${sources}"
     * @required
     */
    private List<String> sources;

    /**
     * The directory for the generated KDocs
     *
     * @parameter expression="${docOutputDir}" default-value="${basedir}/target/site/apidocs"
     * @required
     */
    private String docOutputDir;

    /**
     * The module to use for documentation
     *
     * @parameter expression="${docModule}"
     */
    private String docModule;

    /**
     * Package prefixes which are excluded from the kdoc
     *
     * @parameter expression="${ignorePackages}"
     */
    protected List<String> ignorePackages;

    /**
     * Whether protected classes, functions and properties should be included
     *
     * @parameter expression="${includeProtected}" default-value="true"
     */
    private boolean includeProtected;

    /**
     * The title of the documentation
     *
     * @parameter expression="${title}" default-value="KDoc for ${project.artifactId}"
     */
    private String title;

    /**
     * The version of the documentation
     *
     * @parameter expression="${version}" default-value="KDoc for ${project.version"
     */
    private String version;

    /**
     * Whether warnings should be generated if no comments could be found for classes, functions and properties being documented
     *
     * @parameter expression="${warnNoComments}" default-value="true"
     */
    private boolean warnNoComments;

    @Override
    protected KotlinCompiler createCompiler() {
        return new KDocCompiler();
    }

    @Override
    protected CompilerArguments createCompilerArguments() {
        return new KDocArguments();
    }

    @Override
    protected void configureCompilerArguments(CompilerArguments arguments) throws MojoExecutionException {
        configureBaseCompilerArguments(getLog(), arguments, docModule, sources, classpath, output);

        if (arguments instanceof KDocArguments) {
            KDocArguments kdoc = (KDocArguments) arguments;
            KDocConfig docConfig = kdoc.getDocConfig();
            docConfig.setDocOutputDir(docOutputDir);
            if (ignorePackages != null) {
                docConfig.getIgnorePackages().addAll(ignorePackages);
            }
            docConfig.setIncludeProtected(includeProtected);
            docConfig.setTitle(title);
            docConfig.setVersion(version);
            docConfig.setWarnNoComments(warnNoComments);
            getLog().info("API docs output to: " + docConfig.getDocOutputDir());
            getLog().info("classpath: " + classpath);
            getLog().info("title: " + title);
            getLog().info("sources: " + sources);
            getLog().info("API docs ignore packages: " + ignorePackages);
        } else {
            getLog().warn("No KDocArguments available!");
        }

    }
}

