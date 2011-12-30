package kotlin.modules

import java.util.*
import jet.modules.*

fun moduleSet(description: ModuleSetBuilder.() -> Unit) = description

fun module(name: String, description: ModuleBuilder.() -> Unit) = moduleSet {
    module(name, description)
}

class ModuleSetBuilder(): IModuleSetBuilder {
    private val modules = ArrayList<IModuleBuilder?>()

    fun module(name: String, callback:  ModuleBuilder.() -> Unit) {
        val builder = ModuleBuilder(name)
        builder.callback()
        modules.add(builder)
    }

    override fun getModules(): List<IModuleBuilder?>? = modules
}

class SourcesBuilder(val parent: ModuleBuilder) {
    fun files(pattern: String) {
        parent.addSourceFiles(pattern)
    }
}

class ClasspathBuilder(val parent: ModuleBuilder) {
    fun entry(name: String) {
        parent.addClasspathEntry(name)
    }
}

class JarBuilder(val parent: ModuleBuilder) {
    fun name(jarName: String) {
        parent.setJarName(jarName)
    }
}

open class ModuleBuilder(val name: String): IModuleBuilder {
    // http://youtrack.jetbrains.net/issue/KT-904
    private val sourceFiles0: ArrayList<String?> = ArrayList<String?>()
    private val classpathRoots0: ArrayList<String?> = ArrayList<String?>()
    var _jarName: String? = null

    val source: SourcesBuilder
      get() = SourcesBuilder(this)

    val classpath: ClasspathBuilder
      get() = ClasspathBuilder(this)

    val jar: JarBuilder
      get() = JarBuilder(this)

    fun addSourceFiles(pattern: String) {
        sourceFiles0.add(pattern)
    }

    fun addClasspathEntry(name: String) {
        classpathRoots0.add(name)
    }

    fun setJarName(name: String) {
        _jarName = name
    }

    override fun getSourceFiles(): List<String?>? = sourceFiles0
    override fun getClasspathRoots(): List<String?>? = classpathRoots0
    override fun getModuleName(): String? = name
    override fun getJarName(): String? = _jarName

}

class ModuleBuilder2(name: String): ModuleBuilder(name) {


}

