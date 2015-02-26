package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.tasks.compile.AbstractCompile
import java.io.File
import org.gradle.api.GradleException
import org.jetbrains.kotlin.cli.common.ExitCode
import org.gradle.api.tasks.SourceTask
import org.jetbrains.kotlin.doc.KDocArguments
import java.util.HashSet
import org.jetbrains.kotlin.doc.KDocCompiler
import org.gradle.api.tasks.TaskAction
import org.gradle.api.file.SourceDirectorySet
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.apache.commons.lang.StringUtils
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.codehaus.groovy.runtime.MethodClosure
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.CLICompiler
import com.intellij.ide.highlighter.JavaFileType
import org.jetbrains.kotlin.idea.JetFileType
import org.jetbrains.kotlin.utils.LibraryUtils
import com.intellij.openapi.util.io.FileUtil
import org.apache.commons.io.FilenameUtils

val DEFAULT_ANNOTATIONS = "org.jebrains.kotlin.gradle.defaultAnnotations"

abstract class AbstractKotlinCompile<T : CommonCompilerArguments>() : AbstractCompile() {
    abstract protected val compiler: CLICompiler<T>
    abstract protected fun createBlankArgs(): T
    open protected fun afterCompileHook(args: T) {
    }
    abstract protected fun populateTargetSpecificArgs(args: T)

    public var kotlinOptions: T = createBlankArgs()
    public var kotlinDestinationDir: File? = getDestinationDir()

    private val logger = Logging.getLogger(this.javaClass)
    override fun getLogger() = logger

    [TaskAction]
    override fun compile() {
        getLogger().debug("Starting ${javaClass} task")
        val args = createBlankArgs()
        val sources = getKotlinSources()
        if (sources.isEmpty()) {
            getLogger().warn("No Kotlin files found, skipping Kotlin compiler task")
            return
        }

        populateTargetSpecificArgs(args)
        populateCommonArgs(args, sources)
        callCompiler(args)
        afterCompileHook(args)
    }

    private fun getKotlinSources(): List<File> = getSource().filter { it.isKotlinFile() }

    private fun File.isKotlinFile(): Boolean {
        return when (FilenameUtils.getExtension(getName()).toLowerCase()) {
            "kt", "kts" -> true
            else -> false
        }
    }

    private fun populateCommonArgs(args: T, sources: List<File>) {
        args.freeArgs = sources.map { it.getAbsolutePath() }
        args.suppressWarnings = kotlinOptions.suppressWarnings
        args.verbose = kotlinOptions.verbose
        args.version = kotlinOptions.version
        args.noInline = kotlinOptions.noInline
    }

    private fun callCompiler(args: T) {
        val messageCollector = GradleMessageCollector(getLogger())
        getLogger().debug("Calling compiler")
        val exitCode = compiler.exec(messageCollector, Services.EMPTY, args)

        when (exitCode) {
            ExitCode.COMPILATION_ERROR -> throw GradleException("Compilation error. See log for more details")
            ExitCode.INTERNAL_ERROR -> throw GradleException("Internal compiler error. See log for more details")
        }
    }

}


public open class KotlinCompile() : AbstractKotlinCompile<K2JVMCompilerArguments>() {
    override val compiler = K2JVMCompiler()
    override fun createBlankArgs(): K2JVMCompilerArguments = K2JVMCompilerArguments()

    val srcDirsSources = HashSet<SourceDirectorySet>()

    override fun populateTargetSpecificArgs(args: K2JVMCompilerArguments) {
        args.pluginClasspaths = property("compilerPluginClasspaths") as? Array<String>
        args.pluginOptions = property("compilerPluginArguments") as? Array<String>

        if (StringUtils.isEmpty(kotlinOptions.classpath)) {
            val existingClasspathEntries = getClasspath().filter({ it != null && it.exists() })
            val effectiveClassPath = (getJavaSourceRoots() + existingClasspathEntries).makeString(File.pathSeparator)
            args.classpath = effectiveClassPath
        }

        args.destination = if (StringUtils.isEmpty(kotlinOptions.destination)) {
            kotlinDestinationDir?.getPath()
        } else {
            kotlinOptions.destination
        }

        val embeddedAnnotations = getAnnotations(getProject(), getLogger())
        val userAnnotations = kotlinOptions.annotations?.split(File.pathSeparatorChar)?.toList() ?: emptyList()
        val allAnnotations = if (kotlinOptions.noJdkAnnotations) userAnnotations else userAnnotations.plus(embeddedAnnotations.map { it.getPath() })
        if (allAnnotations.isNotEmpty()) {
            args.annotations = allAnnotations.join(File.pathSeparator)
        }

        args.noStdlib = true
        args.noJdkAnnotations = true
        args.noInline = kotlinOptions.noInline
        args.noOptimize = kotlinOptions.noOptimize
        args.noCallAssertions = kotlinOptions.noCallAssertions
        args.noParamAssertions = kotlinOptions.noParamAssertions
    }

    private fun getJavaSourceRoots(): Set<File> =
            getSource()
            .filter { it.isJavaFile() }
            .map { findSrcDirRoot(it) }
            .filterNotNull()
            .toSet()

    private fun File.isJavaFile() = extension.equalsIgnoreCase(JavaFileType.INSTANCE.getDefaultExtension())

    override fun afterCompileHook(args: K2JVMCompilerArguments) {
        getLogger().debug("Copying resulting files to classes")

        // Copy kotlin classes to all classes directory
        val outputDirFile = File(args.destination!!)
        if (outputDirFile.exists()) {
            FileUtils.copyDirectory(outputDirFile, getDestinationDir())
        }
    }

    // override setSource to track source directory sets
    override fun setSource(source: Any?) {
        srcDirsSources.clear()
        if (source is SourceDirectorySet) {
            srcDirsSources.add(source)
        }
        super.setSource(source)
    }

    // override source to track source directory sets
    override fun source(vararg sources: Any?): SourceTask? {
        for (source in sources) {
            if (source is SourceDirectorySet) {
                srcDirsSources.add(source)
            }
        }
        return super.source(sources)
    }

    fun findSrcDirRoot(file: File): File? {
        for (source in srcDirsSources) {
            for (root in source.getSrcDirs()) {
                if (FileUtil.isAncestor(root, file, false)) {
                    return root
                }
            }
        }
        return null
    }
}

public open class Kotlin2JsCompile() : AbstractKotlinCompile<K2JSCompilerArguments>() {
    override val compiler = K2JSCompiler()

    override fun createBlankArgs(): K2JSCompilerArguments {
        val args = K2JSCompilerArguments()
        args.libraryFiles = array<String>()  // defaults to null
        return args
    }

    public fun addLibraryFiles(vararg fs: String) {
        kotlinOptions.libraryFiles = (kotlinOptions.libraryFiles + (fs as Array<String>)).copyToArray()
    }

    public fun addLibraryFiles(vararg fs: File) {
        val strs = fs.map { it.getPath() }.copyToArray()
        addLibraryFiles(*strs)
    }

    public val outputFile: String?
            get() = kotlinOptions.outputFile

    public val sourceMapDestinationDir: File
            get() = File(outputFile).directory

    public val sourceMap: Boolean
            get() = kotlinOptions.sourceMap

    {
        getOutputs().file(MethodClosure(this, "getOutputFile"))
    }

    override fun populateTargetSpecificArgs(args: K2JSCompilerArguments) {
        args.noStdlib = true
        args.outputFile = outputFile
        args.outputPrefix = kotlinOptions.outputPrefix
        args.outputPostfix = kotlinOptions.outputPostfix

        val kotlinJsLibsFromDependencies =
                getProject().getConfigurations().getByName("compile")
                        .filter { LibraryUtils.isKotlinJavascriptLibrary(it) }
                        .map { it.getAbsolutePath() }

        args.libraryFiles = (kotlinOptions.libraryFiles + kotlinJsLibsFromDependencies).copyToArray()
        args.target = kotlinOptions.target
        args.sourceMap = kotlinOptions.sourceMap

        if (args.outputFile == null) {
            throw GradleException("${getName()}.kotlinOptions.outputFile should be specified.")
        }

        val outputDir = File(args.outputFile).directory
        if (!outputDir.exists()) {
            if (!outputDir.mkdirs()) {
                throw GradleException("Failed to create output directory ${outputDir} or one of its ancestors")
            }
        }

        getLogger().debug("${getName()} set libraryFiles to ${args.libraryFiles.join(",")}")
        getLogger().debug("${getName()} set outputFile to ${args.outputFile}")

    }
}

public open class KDoc() : SourceTask() {

    private val logger = Logging.getLogger(this.javaClass)
    override fun getLogger() = logger

    public var kdocArgs: KDocArguments = KDocArguments()

    public var destinationDir: File? = null

    {
        // by default, output dir is not defined in options
        kdocArgs.docConfig.docOutputDir = ""
    }

    TaskAction fun generateDocs() {
        val args = KDocArguments()
        val cfg = args.docConfig

        val kdocOptions = kdocArgs.docConfig

        cfg.docOutputDir = if ((kdocOptions.docOutputDir.length() == 0) && (destinationDir != null)) {
            destinationDir!!.path
        } else {
            kdocOptions.docOutputDir
        }
        cfg.title = kdocOptions.title
        cfg.sourceRootHref = kdocOptions.sourceRootHref
        cfg.projectRootDir = kdocOptions.projectRootDir
        cfg.warnNoComments = kdocOptions.warnNoComments

        cfg.packagePrefixToUrls.putAll(kdocOptions.packagePrefixToUrls)
        cfg.ignorePackages.addAll(kdocOptions.ignorePackages)
        cfg.packageDescriptionFiles.putAll(kdocOptions.packageDescriptionFiles)
        cfg.packageSummaryText.putAll(kdocOptions.packageSummaryText)

        // KDoc compiler does not accept list of files as input. Try to pass directories instead.
        args.freeArgs = getSource().map { it.getParentFile()!!.getAbsolutePath() }
        // Drop compiled sources to temp. Why KDoc compiles anything after all?!
        args.destination = getTemporaryDir()?.getAbsolutePath()

        getLogger().warn(args.freeArgs.toString())
        val embeddedAnnotations = getAnnotations(getProject(), getLogger())
        val userAnnotations = (kdocArgs.annotations ?: "").split(File.pathSeparatorChar).toList()
        val allAnnotations = if (kdocArgs.noJdkAnnotations) userAnnotations else userAnnotations.plus(embeddedAnnotations.map { it.getPath() })
        args.annotations = allAnnotations.joinToString(File.pathSeparator)

        args.noStdlib = true
        args.noJdkAnnotations = true


        val compiler = KDocCompiler()

        val messageCollector = GradleMessageCollector(getLogger())
        val exitCode = compiler.exec(messageCollector, Services.EMPTY, args)

        when (exitCode) {
            ExitCode.COMPILATION_ERROR -> throw GradleException("Failed to generate kdoc. See log for more details")
            ExitCode.INTERNAL_ERROR -> throw GradleException("Internal generation error. See log for more details")
        }

    }
}

fun getAnnotations(project: Project, logger: Logger): Collection<File> {
    [suppress("UNCHECKED_CAST")]
    val annotations = project.getExtensions().getByName(DEFAULT_ANNOTATIONS) as Collection<File>

    if (!annotations.isEmpty()) {
        logger.info("using default annontations from [${annotations.map { it.getPath() }}]")
        return annotations
    } else {
        throw GradleException("Default annotations not found in Kotlin gradle plugin classpath")
    }
}

class GradleMessageCollector(val logger: Logger) : MessageCollector {
    public override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageLocation) {
        val text = with(StringBuilder()) {
            append(when (severity) {
                in CompilerMessageSeverity.VERBOSE -> "v"
                in CompilerMessageSeverity.ERRORS -> "e"
                CompilerMessageSeverity.INFO -> "i"
                CompilerMessageSeverity.WARNING -> "w"
                else -> throw IllegalArgumentException("Unknown CompilerMessageSeverity: $severity")
            })
            append(": ")

            val path = location.getPath()
            if (path != null) {
                append(path)
                append(": ")
                append("(")
                append(location.getLine())
                append(", ")
                append(location.getColumn())
                append("): ")
            }

            append(message)

            toString()
        }
        when (severity) {
            in CompilerMessageSeverity.VERBOSE -> logger.debug(text)
            in CompilerMessageSeverity.ERRORS -> logger.error(text)
            CompilerMessageSeverity.INFO -> logger.info(text)
            CompilerMessageSeverity.WARNING -> logger.warn(text)
            else -> throw IllegalArgumentException("Unknown CompilerMessageSeverity: $severity")
        }
    }
}
