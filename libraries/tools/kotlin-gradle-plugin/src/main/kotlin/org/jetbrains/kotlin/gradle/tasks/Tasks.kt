package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.jet.cli.jvm.K2JVMCompiler
import org.jetbrains.jet.cli.jvm.K2JVMCompilerArguments
import org.jetbrains.kotlin.gradle.plugin.KSpec
import java.io.File
import org.gradle.api.GradleException
import org.jetbrains.jet.cli.common.ExitCode
import org.gradle.api.Task
import org.gradle.api.tasks.SourceTask
import org.jetbrains.kotlin.doc.KDocArguments
import org.jetbrains.kotlin.doc.KDocConfig
import java.util.HashMap
import java.util.HashSet
import org.jetbrains.kotlin.doc.KDocCompiler
import org.gradle.api.tasks.TaskAction
import java.util.LinkedList
import com.google.common.io.Resources
import java.net.URL
import com.google.common.io.Files
import org.gradle.api.file.SourceDirectorySet
import com.google.common.base.Joiner
import java.util.ArrayList
import org.apache.commons.io.FilenameUtils
import org.jetbrains.jet.cli.common.messages.MessageCollector
import org.jetbrains.jet.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.jet.cli.common.messages.CompilerMessageLocation
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.apache.commons.io.FileUtils
import java.io.IOException
import org.apache.commons.lang.StringUtils
import org.gradle.api.initialization.dsl.ScriptHandler

public open class KotlinCompile(): AbstractCompile() {

    val srcDirsRoots = HashSet<File>()
    val compiler = K2JVMCompiler()
    val logger = Logging.getLogger(getClass())

    public val kotlinOptions: K2JVMCompilerArguments = K2JVMCompilerArguments()


    // override setSource to track source directory sets
    override fun setSource(source: Any?) {
        srcDirsRoots.clear()
        if (source is SourceDirectorySet) {
            srcDirsRoots.addAll(source.getSrcDirs())
        }
        super.setSource(source)
    }

    // override source to track source directory sets
    override fun source(vararg sources: Any?): SourceTask? {
        for (source in sources) {
            if (source is SourceDirectorySet) {
                srcDirsRoots.addAll(source.getSrcDirs())
            }
        }
        return super.source(sources)
    }

    fun findSrcDirRoot(file: File): File? {
        val absPath = file.getAbsolutePath()
        for (root in srcDirsRoots) {
            val rootAbsPath = root.getAbsolutePath()
            if (FilenameUtils.directoryContains(rootAbsPath, absPath)) {
                return root
            }
        }
        return null
    }


    override fun compile() {

        val args = kotlinOptions

        val javaSrcRoots = HashSet<File>()
        val sources = ArrayList<File>()

        // collect source directory roots for all java files to allow cross compilation
        for (file in getSource()) {
            if (FilenameUtils.getExtension(file.getName()).equalsIgnoreCase("java")) {
                val javaRoot = findSrcDirRoot(file)
                if (javaRoot != null) {
                    javaSrcRoots.add(javaRoot)
                }
            } else {
                sources.add(file)
            }
        }

        if (sources.empty) {
            getLogger().warn("No Kotlin files found, skipping Kotlin compiler task")
            return
        }

        val customSources = args.getSourceDirs();
        if (customSources == null || customSources.isEmpty()) {
            args.setSourceDirs(sources.map { it.getAbsolutePath() })
        }


        if (StringUtils.isEmpty(kotlinOptions.classpath)) {
            val gradleClasspath = getClasspath()
            val effectiveClassPath = (javaSrcRoots + gradleClasspath).makeString(File.pathSeparator)
            args.setClasspath(effectiveClassPath)
        }

        val embeddedAnnotations = getAnnotations()
        args.outputDir =if (StringUtils.isEmpty(kotlinOptions.outputDir)) { getDestinationDir()?.getPath() } else { kotlinOptions.outputDir }
        args.annotations = if (StringUtils.isEmpty(kotlinOptions.annotations)) { embeddedAnnotations.getPath() } else { kotlinOptions.annotations }

        val messageCollector = GradleMessageCollector(logger)
        val exitCode = compiler.exec(messageCollector, args)

        when (exitCode) {
            ExitCode.COMPILATION_ERROR -> throw GradleException("Compilation error. See log for more details")
            ExitCode.INTERNAL_ERROR -> throw GradleException("Internal compiler error. See log for more details")
            else -> {}
        }
    }

    fun getAnnotations(): File {
        val configuration = getProject()
                .getBuildscript()
                .getConfigurations()
                .getByName(ScriptHandler.CLASSPATH_CONFIGURATION)!!

        val jdkAnnotationsFromClasspath = configuration.find { it.name.startsWith("kotlin-jdk-annotations") }

        if (jdkAnnotationsFromClasspath != null) {
            logger.info("using jdk annontations from [${jdkAnnotationsFromClasspath.getAbsolutePath()}]")
            return jdkAnnotationsFromClasspath
        } else {
            throw GradleException("kotlin-jdk-annotations not found in Kotlin gradle plugin classpath")
        }
    }
}

public open class KDoc(): SourceTask() {

    val logger = Logging.getLogger(getClass())

    /**
     * Returns the directory to use to output the API docs
     */
    public var destinationDir: String = ""

    /**
     * Returns the name of the documentation set
     */
    public var title: String = ""

    /**
     * Returns the version name of the documentation set
     */
    public var version: String = ""

    /**
     * Returns a map of the package prefix to the HTML URL for the root of the apidoc using javadoc/kdoc style
     * directory layouts so that this API doc report can link to external packages
     */
    public var packagePrefixToUrls: Map<String, String> = HashMap()

    /**
     * Returns a Set of the package name prefixes to ignore from the KDoc report
     */
    public var ignorePackages: Set<String> = HashSet()

    /**
     * Returns true if a warning should be generated if there are no comments
     * on documented function or property
     */
    public var warnNoComments: Boolean = true

    /**
     * Returns the HTTP URL of the root directory of source code that we should link to
     */
    public var sourceRootHref: String = ""

    /**
     * The root project directory used to deduce relative file names when linking to source code
     */
    public var projectRootDir: String = ""

    /**
     * A map of package name to html or markdown files used to describe the package. If none is
     * specified we will look for a package.html or package.md file in the source tree
     */
    public var packageDescriptionFiles: Map<String, String> = HashMap()

    /**
     * A map of package name to summary text used in the package overviews
     */
    public var packageSummaryText: Map<String, String> = HashMap()


    TaskAction fun generateDocs() {
        val args = KDocArguments()
        val cfg = args.docConfig

        cfg.docOutputDir = destinationDir
        cfg.title = title
        cfg.sourceRootHref = sourceRootHref
        cfg.projectRootDir = projectRootDir
        cfg.warnNoComments = warnNoComments

        cfg.packagePrefixToUrls.putAll(packagePrefixToUrls)
        cfg.ignorePackages.addAll(ignorePackages)
        cfg.packageDescriptionFiles.putAll(packageDescriptionFiles)
        cfg.packageSummaryText.putAll(packageSummaryText)

        val compiler = KDocCompiler()

        val messageCollector = GradleMessageCollector(logger)
        val exitCode = compiler.exec(messageCollector, args);

        when (exitCode) {
            ExitCode.COMPILATION_ERROR -> throw GradleException("Failed to generate kdoc. See log for more details")
            ExitCode.INTERNAL_ERROR -> throw GradleException("Internal generation error. See log for more details")
            else -> {}
        }

    }
}

class GradleMessageCollector(val logger : Logger): MessageCollector {
    public override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageLocation) {
        val path = location.getPath()
        val position = if (path != null) (path + ": (" + location.getLine() + ", " + location.getColumn() + ") ") else ""

        val text = position + message

        when (severity) {
            in CompilerMessageSeverity.VERBOSE -> logger.debug(text)
            in CompilerMessageSeverity.ERRORS -> logger.error(text)
            CompilerMessageSeverity.INFO -> logger.info(text)
            else -> logger.warn(text)
        }
    }
}
