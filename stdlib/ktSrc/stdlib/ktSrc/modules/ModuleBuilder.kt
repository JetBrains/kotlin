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

class SourcesBuilder() {
    protected val sourceFiles: ArrayList<String?> = ArrayList<String?>()

    fun files(vararg pattern: String) {
        for(p in pattern) {
            sourceFiles.add(p)
        }
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
    val source     = SourcesBuilder()
    val testSource = SourcesBuilder()

    val classpathRoots: ArrayList<String?> = ArrayList<String?>()
    var _jarName: String? = null

    val classpath: ClasspathBuilder
      get() = ClasspathBuilder(this)

    val jar: JarBuilder
      get() = JarBuilder(this)

    fun addClasspathEntry(name: String) {
        classpathRoots.add(name)
    }

    fun setJarName(name: String) {
        _jarName = name
    }

    override fun getSourceFiles(): List<String?>? = source.sourceFiles
    override fun getTestSourceFiles(): List<String?>? = testSource.sourceFiles
    override fun getClasspathRoots(): List<String?>? = classpathRoots
    override fun getModuleName(): String? = name
    override fun getJarName(): String? = _jarName

}

class ModuleBuilder2(name: String): ModuleBuilder(name) {


}

