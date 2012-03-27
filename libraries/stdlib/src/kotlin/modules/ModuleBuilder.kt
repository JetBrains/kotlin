package kotlin.modules

import java.util.*
import jet.modules.*

fun module(name: String, callback:  ModuleBuilder.() -> Unit) {
    val builder = ModuleBuilder(name)
    builder.callback()
    AllModules.modules.sure().get()?.add(builder)
}

class SourcesBuilder(val parent: ModuleBuilder) {
    fun plusAssign(pattern: String) {
        parent.addSourceFiles(pattern)
    }
}

class ClasspathBuilder(val parent: ModuleBuilder) {
    fun plusAssign(name: String) {
        parent.addClasspathEntry(name)
    }
}

open class ModuleBuilder(val name: String): Module {
    // http://youtrack.jetbrains.net/issue/KT-904
    private val sourceFiles0: ArrayList<String?> = ArrayList<String?>()
    private val classpathRoots0: ArrayList<String?> = ArrayList<String?>()

    val sources: SourcesBuilder
      get() = SourcesBuilder(this)

    val classpath: ClasspathBuilder
      get() = ClasspathBuilder(this)

    fun addSourceFiles(pattern: String) {
        sourceFiles0.add(pattern)
    }

    fun addClasspathEntry(name: String) {
        classpathRoots0.add(name)
    }

    override fun getSourceFiles(): List<String?>? = sourceFiles0
    override fun getClasspathRoots(): List<String?>? = classpathRoots0
    override fun getModuleName(): String? = name
}

