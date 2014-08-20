package kotlin.modules

import java.util.ArrayList

public fun module(name: String, outputDir: String, callback: ModuleBuilder.() -> Unit) {
    val builder = ModuleBuilder(name, outputDir)
    builder.callback()
    AllModules.get()?.add(builder)
}

public class SourcesBuilder(private val parent: ModuleBuilder) {
    public fun plusAssign(pattern: String) {
        parent.addSourceFiles(pattern)
    }
}

public class ClasspathBuilder(private val parent: ModuleBuilder) {
    public fun plusAssign(name: String) {
        parent.addClasspathEntry(name)
    }
}

public class AnnotationsPathBuilder(private val parent: ModuleBuilder) {
    public fun plusAssign(name: String) {
        parent.addAnnotationsPathEntry(name)
    }
}

public open class ModuleBuilder(private val name: String, private val outputDir: String) : Module {
    // http://youtrack.jetbrains.net/issue/KT-904
    private val sourceFiles0 = ArrayList<String>()
    private val classpathRoots0 = ArrayList<String>()
    private val annotationsRoots0 = ArrayList<String>()

    public val sources: SourcesBuilder
        get() = SourcesBuilder(this)

    public val classpath: ClasspathBuilder
        get() = ClasspathBuilder(this)

    public val annotationsPath: AnnotationsPathBuilder
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

