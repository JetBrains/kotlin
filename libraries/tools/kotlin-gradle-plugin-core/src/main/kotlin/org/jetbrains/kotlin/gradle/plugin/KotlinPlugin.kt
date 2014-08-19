package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.jetbrains.kotlin.gradle.internal.KotlinSourceSetImpl
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.internal.HasConvention
import org.jetbrains.kotlin.gradle.internal.KotlinSourceSet
import org.gradle.api.specs.Spec
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KDoc
import java.io.File
import java.util.concurrent.Callable
import org.gradle.api.Action
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.api.logging.Logging
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import org.gradle.api.internal.DefaultDomainObjectSet
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.api.AndroidSourceSet
import org.gradle.api.plugins.ExtensionAware
import org.jetbrains.jet.cli.common.arguments.K2JVMCompilerArguments;
import java.util.ArrayList
import com.android.build.gradle.BasePlugin
import com.android.build.gradle.api.LibraryVariant
import com.android.build.gradle.api.ApkVariant
import com.android.builder.model.BuildType
import com.android.build.gradle.api.TestVariant
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.Dependency
import java.util.HashSet
import org.gradle.api.UnknownDomainObjectException
import org.gradle.api.initialization.dsl.ScriptHandler
import org.jetbrains.kotlin.gradle.plugin.android.AndroidGradleWrapper
import javax.inject.Inject

val DEFAULT_ANNOTATIONS = "org.jebrains.kotlin.gradle.defaultAnnotations"

open class KotlinPlugin [Inject] (val scriptHandler: ScriptHandler): Plugin<Project> {

    public override fun apply(project: Project) {
        val javaBasePlugin = project.getPlugins().apply(javaClass<JavaBasePlugin>())
        val javaPluginConvention = project.getConvention().getPlugin(javaClass<JavaPluginConvention>())

        project.getPlugins().apply(javaClass<JavaPlugin>())

        configureSourceSetDefaults(project as ProjectInternal, javaBasePlugin, javaPluginConvention)
        configureKDoc(project, javaPluginConvention)

        val version = project.getProperties()["kotlin.gradle.plugin.version"] as String
        project.getExtensions().add(DEFAULT_ANNOTATIONS, GradleUtils(scriptHandler).resolveDependencies("org.jetbrains.kotlin:kotlin-jdk-annotations:$version"))
    }


    private fun configureSourceSetDefaults(project: ProjectInternal,
                                           javaBasePlugin: JavaBasePlugin,
                                           javaPluginConvention: JavaPluginConvention) {
        javaPluginConvention.getSourceSets()?.all(object : Action<SourceSet> {
            override fun execute(sourceSet: SourceSet?) {
                if (sourceSet is HasConvention) {
                    val sourceSetName = sourceSet.getName()
                    val kotlinSourceSet = KotlinSourceSetImpl( sourceSetName, project.getFileResolver())
                    sourceSet.getConvention().getPlugins().put("kotlin", kotlinSourceSet)

                    val kotlinDirSet = kotlinSourceSet.getKotlin()
                    kotlinDirSet.srcDir(project.file("src/${sourceSetName}/kotlin"))

                    sourceSet.getAllJava()?.source(kotlinDirSet)
                    sourceSet.getAllSource()?.source(kotlinDirSet)
                    sourceSet.getResources()?.getFilter()?.exclude(KSpec({ elem ->
                        kotlinDirSet.contains(elem.getFile())
                    }))

                    val kotlinTaskName = sourceSet.getCompileTaskName("kotlin")
                    val kotlinTask: KotlinCompile = project.getTasks().create(kotlinTaskName, javaClass<KotlinCompile>())

                    javaBasePlugin.configureForSourceSet(sourceSet, kotlinTask)
                    // store kotlin classes in separate directory. They will serve as class-path to java compiler
                    val kotlinOutputDir = File(project.getBuildDir(), "kotlin-classes/${sourceSetName}")
                    kotlinTask.kotlinDestinationDir = kotlinOutputDir;

                    kotlinTask.setDescription("Compiles the $sourceSet.kotlin.")
                    kotlinTask.source(kotlinDirSet)

                    val javaTask = project.getTasks().findByName(sourceSet.getCompileJavaTaskName()) as AbstractCompile?

                    if (javaTask != null) {
                        javaTask.dependsOn(kotlinTaskName)
                        val javacClassPath = javaTask.getClasspath() + project.files(kotlinTask.kotlinDestinationDir);
                        javaTask.setClasspath(javacClassPath)
                    }
                }
            }
        })
    }

    private fun configureKDoc(project: Project, javaPluginConvention: JavaPluginConvention) {
        val mainSourceSet = javaPluginConvention.getSourceSets()?.findByName(SourceSet.MAIN_SOURCE_SET_NAME) as HasConvention?

        if (mainSourceSet != null) {
            val kdoc = project.getTasks().create(KDOC_TASK_NAME, javaClass<KDoc>())!!

            kdoc.setDescription("Generates KDoc API documentation for the main source code.")
            kdoc.setGroup(JavaBasePlugin.DOCUMENTATION_GROUP)
            kdoc.setSource(mainSourceSet.getConvention().getExtensionsAsDynamicObject().getProperty("kotlin"))
        }

        project.getTasks().withType(javaClass<KDoc>(), object : Action<KDoc> {
            override fun execute(task: KDoc?) {
                task!!.destinationDir = File(javaPluginConvention.getDocsDir(), "kdoc")
            }
        })
    }

    public val KDOC_TASK_NAME: String = "kdoc"
}


open class KotlinAndroidPlugin [Inject] (val scriptHandler: ScriptHandler): Plugin<Project> {

    val log = Logging.getLogger(this.javaClass)

    public override fun apply(p0: Project) {

        val project = p0 as ProjectInternal
        val ext = project.getExtensions().getByName("android") as BaseExtension

        ext.getSourceSets().all(object : Action<AndroidSourceSet> {
            override fun execute(sourceSet: AndroidSourceSet?) {
                if (sourceSet is HasConvention) {
                    val sourceSetName = sourceSet.getName()
                    val kotlinSourceSet = KotlinSourceSetImpl( sourceSetName, project.getFileResolver())
                    sourceSet.getConvention().getPlugins().put("kotlin", kotlinSourceSet)
                    val kotlinDirSet = kotlinSourceSet.getKotlin()
                    kotlinDirSet.srcDir(project.file("src/${sourceSetName}/kotlin"))


                    /*TODO: before 0.11 gradle android plugin there was:
                      sourceSet.getAllJava().source(kotlinDirSet)
                      sourceSet.getAllSource().source(kotlinDirSet)
                      AndroidGradleWrapper.getResourceFilter(sourceSet)?.exclude(KSpec({ elem ->
                        kotlinDirSet.contains(elem.getFile())
                      }))
                     but those methods were removed so commented as temporary hack*/

                    project.getLogger().debug("Created kotlin sourceDirectorySet at ${kotlinDirSet.getSrcDirs()}")
                }
            }
        })

        (ext as ExtensionAware).getExtensions().add("kotlinOptions", K2JVMCompilerArguments())

        project.afterEvaluate( { (project: Project?): Unit ->
            if (project != null) {
                val testVariants = ext.getTestVariants()!!
                processVariants(testVariants, project, ext)
                if (ext is AppExtension) {
                    val appVariants = ext.getApplicationVariants()!!
                    processVariants(appVariants, project, ext)
                }

                if (ext is LibraryExtension) {
                    val libVariants = ext.getLibraryVariants()!!;
                    processVariants(libVariants, project, ext)
                }
            }

        })
        val version = project.getProperties()["kotlin.gradle.plugin.version"] as String
        project.getExtensions().add(DEFAULT_ANNOTATIONS, GradleUtils(scriptHandler!!).resolveDependencies("org.jetbrains.kotlin:kotlin-android-sdk-annotations:$version"));
    }

    private fun processVariants(variants: DefaultDomainObjectSet<out BaseVariant>, project: Project, androidExt: BaseExtension): Unit {
        val logger = project.getLogger()
        val kotlinOptions = getExtention<K2JVMCompilerArguments>(androidExt, "kotlinOptions")
        val sourceSets = androidExt.getSourceSets()
        //TODO: change to BuilderConstants.MAIN - it was relocated in 0.11 plugin
        val mainSourceSet = sourceSets.getByName("main")
        val testSourceSet = try {
            sourceSets.getByName("instrumentTest")
        } catch (e: UnknownDomainObjectException) {
            sourceSets.getByName("androidTest")
        }

        for (variant in variants) {
            if (variant is LibraryVariant || variant is ApkVariant) {
                val buildTypeSourceSetName = AndroidGradleWrapper.getVariantName(variant)

                logger.debug("Variant build type is [$buildTypeSourceSetName]")
                val buildTypeSourceSet : AndroidSourceSet? = sourceSets.findByName(buildTypeSourceSetName)

                val javaTask = variant.getJavaCompile()!!
                val variantName = variant.getName()

                val kotlinTaskName = "compile${variantName}Kotlin"
                val kotlinTask: KotlinCompile = project.getTasks().create(kotlinTaskName, javaClass<KotlinCompile>())
                kotlinTask.kotlinOptions = kotlinOptions


                // store kotlin classes in separate directory. They will serve as class-path to java compiler
                val kotlinOutputDir = File(project.getBuildDir(), "tmp/kotlin-classes/${variantName}")
                kotlinTask.kotlinDestinationDir = kotlinOutputDir;
                kotlinTask.setDestinationDir(javaTask.getDestinationDir())
                kotlinTask.setDescription("Compiles the ${variantName} kotlin.")
                kotlinTask.setClasspath(javaTask.getClasspath())
                kotlinTask.setDependsOn(javaTask.getDependsOn())

                kotlinTask.resPath = File(variant.getMergeResources()?.getOutputDir()!!.canonicalPath + "/layout").canonicalPath
                kotlinTask.manifestPath = variant.getProcessResources().getManifestFile()!!.canonicalPath

                val javaSourceList = ArrayList<Any?>()

                if (variant is TestVariant) {
                    javaSourceList.addAll(AndroidGradleWrapper.getJavaSrcDirs(testSourceSet))
                    val testKotlinSource = getExtention<KotlinSourceSet>(testSourceSet, "kotlin")
                    kotlinTask.source(testKotlinSource.getKotlin())
                } else {
                    javaSourceList.addAll(AndroidGradleWrapper.getJavaSrcDirs(mainSourceSet))
                    val mainKotlinSource = getExtention<KotlinSourceSet>(mainSourceSet, "kotlin")
                    kotlinTask.source(mainKotlinSource.getKotlin())
                }

                if (null != buildTypeSourceSet) {
                    javaSourceList.add(AndroidGradleWrapper.getJavaSrcDirs(buildTypeSourceSet))
                    kotlinTask.source(getExtention<KotlinSourceSet>(buildTypeSourceSet, "kotlin").getKotlin())
                }
                javaSourceList.add(Callable<File?>{ variant.getProcessResources().getSourceOutputDir() })
                javaSourceList.add(Callable<File?>{ variant.getGenerateBuildConfig()?.getSourceOutputDir() })
                javaSourceList.add(Callable<File?>{ variant.getAidlCompile().getSourceOutputDir() })
                javaSourceList.add(Callable<File?>{ variant.getRenderscriptCompile().getSourceOutputDir() })

                if (variant is ApkVariant) {
                    for (flavourName in AndroidGradleWrapper.getProductFlavorsNames(variant)) {
                       val flavourSourceSetName = buildTypeSourceSetName + flavourName
                        val flavourSourceSet : AndroidSourceSet? = sourceSets.findByName(flavourSourceSetName)
                        if (flavourSourceSet != null) {
                            javaSourceList.add(AndroidGradleWrapper.getJavaSrcDirs(flavourSourceSet))
                            kotlinTask.source((buildTypeSourceSet as ExtensionAware).getExtensions().getByName("kotlin"))
                        }
                    }
                }

                kotlinTask.doFirst({ task  ->
                    var plugin = project.getPlugins().findPlugin("android")
                    if (null == plugin) {
                        plugin = project.getPlugins().findPlugin("android-library")
                    }
                    val basePlugin : BasePlugin = plugin as BasePlugin
                    val javaSources = project.files(javaSourceList)
                    val androidRT = project.files(AndroidGradleWrapper.getRuntimeJars(basePlugin))
                    val fullClasspath = (javaTask.getClasspath() + (javaSources + androidRT)) - project.files(kotlinTask.kotlinDestinationDir)
                    (task as AbstractCompile).setClasspath(fullClasspath)
                })

                javaTask.dependsOn(kotlinTaskName)
                val javacClassPath = javaTask.getClasspath() + project.files(kotlinTask.kotlinDestinationDir);
                javaTask.setClasspath(javacClassPath)
            }
        }
    }

    fun <T> getExtention(obj: Any, extensionName: String): T {
        if (obj is ExtensionAware) {
            val result = obj.getExtensions().findByName(extensionName)
            if (result != null) {
                return result as T
            }
        }
        val result = (obj as HasConvention).getConvention().getPlugins()[extensionName]
        return result as T
    }

}

open class KSpec<T: Any?>(val predicate: (T) -> Boolean): Spec<T> {
    public override fun isSatisfiedBy(p0: T?): Boolean {
        return p0 != null && predicate(p0)
    }
}

open class GradleUtils(val scriptHandler: ScriptHandler) {
    public fun resolveDependencies(vararg coordinates: String): Collection<File> {
        val dependencyHandler : DependencyHandler = scriptHandler.getDependencies()
        val configurationsContainer : ConfigurationContainer = scriptHandler.getConfigurations()

        val deps = coordinates.map { dependencyHandler.create(it) }
        val configuration = configurationsContainer.detachedConfiguration(*deps.copyToArray())

        return configuration.getResolvedConfiguration().getFiles(KSpec({ dep -> true }))!!
    }
}

