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

/**
 * Created by Nikita.Skvortsov
 * Date: 2/22/13, 2:56 PM
 */

open class KotlinCompile() : AbstractCompile() {

    val srcDirsRoots = HashSet<File>()
    val compiler = K2JVMCompiler()
    val logger = getLogger()

    // override setSource to track source directory sets
    override fun setSource(source : Any?) {
        srcDirsRoots.clear()
        if (source is SourceDirectorySet) {
            srcDirsRoots.addAll(source.getSrcDirs())
        }
        super.setSource(source)
    }

    // override source to track source directory sets
    override fun source(vararg  sources : Any?) : SourceTask? {
        for (source in sources) {
            if (source is SourceDirectorySet) {
                srcDirsRoots.addAll(source.getSrcDirs())
            }
        }
        return super.source(sources)
    }

    fun findSrcDirRoot(file : File) : String {
        val absPath = file.getAbsolutePath()
        for (root in srcDirsRoots) {
            val rootAbsPath = root.getAbsolutePath()
            if (absPath.contains(rootAbsPath)) {
                return rootAbsPath
            }
        }
        return ""
    }


    override fun compile() {

        val args = K2JVMCompilerArguments();
        val javaSrcRoots = HashSet<String>()

        val sources : MutableList<String> = LinkedList<String>()

        // collect source directory roots for all java files to allow cross compilation
        getSource().mapTo(sources, { (f : File) : String ->
            if (f.getName().endsWith(".java")) {
                val javaRoot = findSrcDirRoot(f)
                if (javaRoot.length() > 0) {
                    javaSrcRoots.add(javaRoot)
                }
                ""
            } else {
                f.getAbsolutePath()
            }
        })

        args.setSourceDirs(sources.filter { it.length() > 0 })

        val joiner = Joiner.on(File.pathSeparator)!!
        val gradleClasspath = getClasspath()?.filter(KSpec<File?>({ it != null && it.isAbsolute() }))?.getAsPath()
        val javaSrcClasspath = joiner.join(javaSrcRoots)
        val effectiveClassPath = joiner.join(javaSrcClasspath, gradleClasspath)

        if (effectiveClassPath != null && effectiveClassPath.length() > 0) {
            args.setClasspath(effectiveClassPath)
        }

        args.outputDir = (getDestinationDir()?.getPath())
        args.noJdkAnnotations = true
        args.annotations = getAnnotations()
        args.noStdlib = true

        val exitCode = compiler.exec(System.err, args)

        when (exitCode) {
            ExitCode.COMPILATION_ERROR -> throw GradleException("Compilation error. See log for more details")
            ExitCode.INTERNAL_ERROR -> throw GradleException("Internal compiler error. See log for more details")
            else -> ""
        }

    }

    fun getAnnotations() : String? {
        val jdkAnnotations : String = "kotlin-jdk-annotations.jar"
        val jdkAnnotationsResource : URL? = Resources.getResource(jdkAnnotations)
        if (jdkAnnotationsResource == null) {
            return null
        }

        // todo fix leak of deleteOnExit handlers
        val jdkAnnotationsTempDir : File? = Files.createTempDir()
        jdkAnnotationsTempDir?.deleteOnExit()
        val jdkAnnotationsFile : File = File(jdkAnnotationsTempDir, jdkAnnotations)

        Files.copy(Resources.newInputStreamSupplier(jdkAnnotationsResource), jdkAnnotationsFile)
        return jdkAnnotationsFile.getPath()
    }
}



open class KDoc() : SourceTask() {

    /**
     * Returns the directory to use to output the API docs
     */
    public var destinationDir : String = ""

    /**
     * Returns the name of the documentation set
     */
    public var title : String = ""

    /**
     * Returns the version name of the documentation set
     */
    public var version : String = ""

    /**
     * Returns a map of the package prefix to the HTML URL for the root of the apidoc using javadoc/kdoc style
     * directory layouts so that this API doc report can link to external packages
     */
    public var packagePrefixToUrls : Map<String, String> = HashMap()

    /**
     * Returns a Set of the package name prefixes to ignore from the KDoc report
     */
    public var ignorePackages : Set<String> = HashSet()

    /**
     * Returns true if a warning should be generated if there are no comments
     * on documented function or property
     */
    public var warnNoComments : Boolean = true

    /**
     * Returns the HTTP URL of the root directory of source code that we should link to
     */
    public var sourceRootHref : String = ""

    /**
     * The root project directory used to deduce relative file names when linking to source code
     */
    public var projectRootDir : String = ""

    /**
     * A map of package name to html or markdown files used to describe the package. If none is
     * speciied we will look for a package.html or package.md file in the source tree
     */
    public var packageDescriptionFiles : Map<String,String> = HashMap()

    /**
     * A map of package name to summary text used in the package overviews
     */
    public var packageSummaryText : Map<String,String> = HashMap()


    TaskAction fun generateDocs() {
        val args : KDocArguments  = KDocArguments()
        val cfg : KDocConfig = args.docConfig

        cfg.docOutputDir = destinationDir
        cfg.title = title
        cfg.sourceRootHref = sourceRootHref
        cfg.projectRootDir = projectRootDir
        cfg.warnNoComments = warnNoComments

        cfg.packagePrefixToUrls.putAll(packagePrefixToUrls)
        cfg.ignorePackages.addAll(ignorePackages)
        cfg.packageDescriptionFiles.putAll(packageDescriptionFiles)
        cfg.packageSummaryText.putAll(packageSummaryText)

        val compiler : KDocCompiler = KDocCompiler()

        val exitCode = compiler.exec(System.err, args);

        when (exitCode) {
            ExitCode.COMPILATION_ERROR -> throw GradleException("Failed to generate kdoc. See log for more details")
            ExitCode.INTERNAL_ERROR -> throw GradleException("Internal generation error. See log for more details")
            else -> ""
        }

    }
}

