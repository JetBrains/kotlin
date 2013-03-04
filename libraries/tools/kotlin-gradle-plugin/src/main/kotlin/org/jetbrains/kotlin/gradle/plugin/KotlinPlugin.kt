package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.plugins.JavaPlugin
import groovy.lang.Closure
import org.gradle.api.tasks.SourceSet
import org.jetbrains.kotlin.gradle.internal.KotlinSourceSetImpl
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.internal.IConventionAware
import org.gradle.api.internal.HasConvention
import org.gradle.api.internal.file.collections.SimpleFileCollection
import org.gradle.api.file.FileTreeElement
import org.jetbrains.kotlin.gradle.internal.KotlinSourceSet
import org.gradle.api.file.SourceDirectorySet
import org.codehaus.groovy.runtime.MethodClosure
import org.gradle.api.specs.Spec
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KDoc
import java.io.File
import java.util.concurrent.Callable
import org.gradle.api.Action
import org.gradle.api.tasks.compile.AbstractCompile
import java.util.Arrays

open class KotlinPlugin: Plugin<Project> {

    public override fun apply(project: Project) {
        val javaBasePlugin = project.getPlugins().apply(javaClass<JavaBasePlugin>())
        val javaPluginConvention = project.getConvention().getPlugin(javaClass<JavaPluginConvention>())

        project.getPlugins().apply(javaClass<JavaPlugin>())

        configureSourceSetDefaults(project as ProjectInternal, javaBasePlugin, javaPluginConvention)
        configureKDoc(project, javaPluginConvention)
    }


    private fun configureSourceSetDefaults(project: ProjectInternal,
                                           javaBasePlugin: JavaBasePlugin,
                                           javaPluginConvention: JavaPluginConvention) {
        javaPluginConvention.getSourceSets()?.all(object : Action<SourceSet> {
            override fun execute(sourceSet: SourceSet?) {
                if (sourceSet is HasConvention) {
                    val sourceSetName = sourceSet.getName()
                    val kotlinSourceSet = KotlinSourceSetImpl(sourceSetName, project.getFileResolver())

                    val kotlinDirSet = kotlinSourceSet.getKotlin()
                    kotlinDirSet.srcDir(project.file("src/${sourceSetName}/kotlin"))

                    sourceSet.getAllJava()?.source(kotlinDirSet)
                    sourceSet.getAllSource()?.source(kotlinDirSet)
                    sourceSet.getConvention().getPlugins().put("kotlin", kotlinSourceSet)

                    sourceSet.getResources()?.getFilter()?.exclude(KSpec({ elem ->
                        kotlinDirSet.contains(elem.getFile())
                    }))

                    val kotlinTaskName = sourceSet.getCompileTaskName("kotlin")
                    val kotlinTask: KotlinCompile = project.getTasks().add(kotlinTaskName, javaClass<KotlinCompile>())!!

                    javaBasePlugin.configureForSourceSet(sourceSet, kotlinTask)

                    kotlinTask.setDescription("Compiles the $sourceSet.kotlin.")
                    kotlinTask.source(kotlinDirSet)

                    val javaTask = project.getTasks().findByName(sourceSet.getCompileJavaTaskName()) as AbstractCompile?
                    javaTask?.dependsOn(kotlinTaskName)

                    val sourceSetCompileConfigurationName = if (sourceSetName == SourceSet.MAIN_SOURCE_SET_NAME) {
                        "compile"
                    } else {
                        "${sourceSetName}Compile"
                    }

                    project.getDependencies()?.add(sourceSetCompileConfigurationName, project.files(kotlinTask.getDestinationDir()))
                }
            }
        })
    }

    private fun configureKDoc(project: Project, javaPluginConvention: JavaPluginConvention) {
        val mainSourceSet = javaPluginConvention.getSourceSets()?.getByName(SourceSet.MAIN_SOURCE_SET_NAME)!! as HasConvention

        val kdoc = project.getTasks()?.add(KDOC_TASK_NAME, javaClass<KDoc>())!!
        kdoc.setDescription("Generates KDoc API documentation for the main source code.")
        kdoc.setGroup(JavaBasePlugin.DOCUMENTATION_GROUP)
        kdoc.setSource(mainSourceSet.getConvention().getExtensionsAsDynamicObject().getProperty("kotlin"))

        project.getTasks()?.withType(javaClass<KDoc>(), object : Action<KDoc> {
            override fun execute(param: KDoc?) {
                param?.getConventionMapping()?.map("destinationDir", object : Callable<Any> {
                    override fun call(): Any {
                        return File(javaPluginConvention.getDocsDir(), "kdoc");
                    }
                })
            }
        })
    }

    public val KDOC_TASK_NAME: String = "kdoc"
}


open class KSpec<T: Any?>(val predicate: (T) -> Boolean): Spec<T> {
    public override fun isSatisfiedBy(p0: T?): Boolean {
        return p0 != null && predicate(p0)
    }
}
