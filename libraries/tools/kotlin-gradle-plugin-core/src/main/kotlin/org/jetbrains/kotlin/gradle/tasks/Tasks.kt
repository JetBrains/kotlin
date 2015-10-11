package org.jetbrains.kotlin.gradle.tasks

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.util.io.FileUtil
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang.StringUtils
import org.codehaus.groovy.runtime.MethodClosure
import org.gradle.api.GradleException
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.SourceTask
import java.util.HashSet
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.utils.LibraryUtils
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.org.objectweb.asm.ClassWriter
import java.io.IOException
import java.lang.ref.WeakReference
import java.io.File

val DEFAULT_ANNOTATIONS = "org.jebrains.kotlin.gradle.defaultAnnotations"

val ANNOTATIONS_PLUGIN_NAME = "org.jetbrains.kotlin.kapt"

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

    @TaskAction
    override fun compile() {
        getLogger().debug("Starting ${javaClass} task")
        val args = createBlankArgs()
        val sources = getKotlinSources()
        if (sources.isEmpty()) {
            getLogger().warn("No Kotlin files found, skipping Kotlin compiler task")
            return
        }

        populateCommonArgs(args, sources)
        populateTargetSpecificArgs(args)
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
        // show kotlin compiler where to look for java source files
        args.freeArgs = (args.freeArgs + getJavaSourceRoots().map { it.getAbsolutePath() }).toSet().toList()
        getLogger().kotlinDebug("args.freeArgs = ${args.freeArgs}")

        if (StringUtils.isEmpty(kotlinOptions.classpath)) {
            args.classpath = getClasspath().filter({ it != null && it.exists() }).joinToString(File.pathSeparator)
            getLogger().kotlinDebug("args.classpath = ${args.classpath}")
        }

        args.destination = if (StringUtils.isEmpty(kotlinOptions.destination)) {
            kotlinDestinationDir?.getPath()
        } else {
            kotlinOptions.destination
        }
        getLogger().kotlinDebug("args.destination = ${args.destination}")

        val extraProperties = getExtensions().getExtraProperties()
        args.pluginClasspaths = extraProperties.getOrNull<Array<String>>("compilerPluginClasspaths") ?: arrayOf()
        getLogger().kotlinDebug("args.pluginClasspaths = ${args.pluginClasspaths.joinToString(File.pathSeparator)}")
        val basePluginOptions = extraProperties.getOrNull<Array<String>>("compilerPluginArguments") ?: arrayOf()

        val pluginOptions = arrayListOf(*basePluginOptions)
        handleKaptProperties(extraProperties, pluginOptions)

        args.pluginOptions = pluginOptions.toTypedArray()
        getLogger().kotlinDebug("args.pluginOptions = ${args.pluginOptions.joinToString(File.pathSeparator)}")

        args.noStdlib = true
        args.noInline = kotlinOptions.noInline
        args.noOptimize = kotlinOptions.noOptimize
        args.noCallAssertions = kotlinOptions.noCallAssertions
        args.noParamAssertions = kotlinOptions.noParamAssertions
        args.moduleName = kotlinOptions.moduleName ?: extraProperties.getOrNull<String>("defaultModuleName")
        getLogger().kotlinDebug("args.moduleName = ${args.moduleName}")
    }

    private fun handleKaptProperties(extraProperties: ExtraPropertiesExtension, pluginOptions: MutableList<String>) {
        val kaptAnnotationsFile = extraProperties.getOrNull<File>("kaptAnnotationsFile")
        if (kaptAnnotationsFile != null) {
            if (kaptAnnotationsFile.exists()) kaptAnnotationsFile.delete()
            pluginOptions.add("plugin:$ANNOTATIONS_PLUGIN_NAME:output=" + kaptAnnotationsFile)
        }

        val kaptClassFileStubsDir = extraProperties.getOrNull<File>("kaptStubsDir")
        if (kaptClassFileStubsDir != null) {
            pluginOptions.add("plugin:$ANNOTATIONS_PLUGIN_NAME:stubs=" + kaptClassFileStubsDir)
        }

        val supportInheritedAnnotations = extraProperties.getOrNull<Boolean>("kaptInheritedAnnotations")
        if (supportInheritedAnnotations != null && supportInheritedAnnotations) {
            pluginOptions.add("plugin:$ANNOTATIONS_PLUGIN_NAME:inherited=true")
        }
    }

    private fun getJavaSourceRoots(): Set<File> =
            getSource()
            .filter { it.isJavaFile() }
            .map { findSrcDirRoot(it) }
            .filterNotNull()
            .toSet()

    private fun File.isJavaFile() = extension.equals(JavaFileType.INSTANCE.getDefaultExtension(), ignoreCase = true)

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
        args.libraryFiles = arrayOf<String>()  // defaults to null
        return args
    }

    public fun addLibraryFiles(vararg fs: String) {
        kotlinOptions.libraryFiles += fs
    }

    public fun addLibraryFiles(vararg fs: File) {
        val strs = fs.map { it.getPath() }.toTypedArray()
        addLibraryFiles(*strs)
    }

    public val outputFile: String?
            get() = kotlinOptions.outputFile

    public val sourceMapDestinationDir: File
            get() = File(outputFile).directory

    public val sourceMap: Boolean
            get() = kotlinOptions.sourceMap

    init {
        getOutputs().file(MethodClosure(this, "getOutputFile"))
    }

    override fun populateTargetSpecificArgs(args: K2JSCompilerArguments) {
        args.noStdlib = true
        args.outputFile = outputFile
        args.outputPrefix = kotlinOptions.outputPrefix
        args.outputPostfix = kotlinOptions.outputPostfix
        args.metaInfo = kotlinOptions.metaInfo

        val kotlinJsLibsFromDependencies =
                getProject().getConfigurations().getByName("compile")
                        .filter { LibraryUtils.isKotlinJavascriptLibrary(it) }
                        .map { it.getAbsolutePath() }

        args.libraryFiles = kotlinOptions.libraryFiles + kotlinJsLibsFromDependencies
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

        getLogger().debug("${getName()} set libraryFiles to ${args.libraryFiles.joinToString(",")}")
        getLogger().debug("${getName()} set outputFile to ${args.outputFile}")

    }
}

private fun <T: Any> ExtraPropertiesExtension.getOrNull(id: String): T? {
    try {
        @Suppress("UNCHECKED_CAST")
        return get(id) as? T
    }
    catch (e: ExtraPropertiesExtension.UnknownPropertyException) {
        return null
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

            val (path, line, column) = location
            if (path != null) {
                append(path)
                append(": ")
                if (line > 0 && column > 0) {
                    append("($line, $column): ")
                }
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

fun Logger.kotlinDebug(message: String) {
    this.debug("[KOTLIN] $message")
}
