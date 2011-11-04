namespace kotlin

namespace modules {

import java.util.*
import jet.modules.*

class ModuleSetBuilder(): IModuleSetBuilder {
    val modules: ArrayList<IModuleBuilder?> = ArrayList<IModuleBuilder?>()

    fun module(name: String, callback: fun ModuleBuilder.()) {
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
    val sourceFiles: ArrayList<String?> = ArrayList<String?>()
    val classpathRoots: ArrayList<String?> = ArrayList<String?>()
    var _jarName: String? = null

    val source: SourcesBuilder
      get() = SourcesBuilder(this)

    val classpath: ClasspathBuilder
      get() = ClasspathBuilder(this)

    val jar: JarBuilder
      get() = JarBuilder(this)

    fun addSourceFiles(pattern: String) {
        sourceFiles.add(pattern)
    }

    fun addClasspathEntry(name: String) {
        classpathRoots.add(name)
    }

    fun setJarName(name: String) {
        _jarName = name
    }

    override fun getSourceFiles(): List<String?>? = sourceFiles
    override fun getClasspathRoots(): List<String?>? = classpathRoots
    override fun getModuleName(): String? = name
    override fun getJarName(): String? = _jarName

}

class ModuleBuilder2(name: String): ModuleBuilder(name) {


}
}

