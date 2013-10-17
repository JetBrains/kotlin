package kotlin.modules

import java.util.*
import jet.modules.*

public fun module(name: String, outputDir: String, callback:  ModuleBuilder.() -> Unit) {
    val builder = ModuleBuilder(name, outputDir)
    builder.callback()
    AllModules.modules.get()?.add(builder)
}

class SourcesBuilder(val parent: ModuleBuilder) {
    public fun plusAssign(pattern: String) {
        parent.addSourceFiles(pattern)
    }
}

class ClasspathBuilder(val parent: ModuleBuilder) {
    public fun plusAssign(name: String) {
        parent.addClasspathEntry(name)
    }
}

class AnnotationsPathBuilder(val parent: ModuleBuilder) {
    public fun plusAssign(name: String) {
        parent.addAnnotationsPathEntry(name)
    }
}

open class ModuleBuilder(val name: String, val outputDir: String): Module {
    // http://youtrack.jetbrains.net/issue/KT-904
    private val sourceFiles0 = ArrayList<String>()
    private val classpathRoots0 = ArrayList<String>()
    private val annotationsRoots0 = ArrayList<String>()

    val sources: SourcesBuilder
      get() = SourcesBuilder(this)

    val classpath: ClasspathBuilder
      get() = ClasspathBuilder(this)

    val annotationsPath: AnnotationsPathBuilder
      get() = AnnotationsPathBuilder(this)

    public fun addSourceFiles(pattern: String) {
        sourceFiles0.add(pattern)
    }

    public fun addClasspathEntry(name: String) {
        classpathRoots0.add(name)
    }

    public fun addAnnotationsPathEntry(name: String) {
        annotationsRoots0.add(name)
    }

    public override fun getOutputDirectory(): String = outputDir
    public override fun getSourceFiles(): List<String> = sourceFiles0
    public override fun getClasspathRoots(): List<String> = classpathRoots0
    public override fun getAnnotationsRoots(): List<String> = annotationsRoots0
    public override fun getModuleName(): String = name
}

