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
import org.jetbrains.jet.cli.jvm.K2JVMCompilerArguments
import java.util.ArrayList
import com.android.build.gradle.BasePlugin
import com.android.build.gradle.api.LibraryVariant
import com.android.build.gradle.api.ApkVariant
import com.android.builder.model.BuildType
import com.android.builder.BuilderConstants
import com.android.build.gradle.api.TestVariant

open class KotlinPlugin: Plugin<Project> {

    public override fun apply(project: Project) {
        val javaBasePlugin = project.getPlugins().apply(javaClass<JavaBasePlugin>())
        val javaPluginConvention = project.getConvention().getPlugin(javaClass<JavaPluginConvention>())

        //project.getPlugins().apply(javaClass<JavaPlugin>())

        configureSourceSetDefaults(project as ProjectInternal, javaBasePlugin, javaPluginConvention)
        configureKDoc(project, javaPluginConvention)
    }


    private fun configureSourceSetDefaults(project: ProjectInternal,
                                           javaBasePlugin: JavaBasePlugin,
                                           javaPluginConvention: JavaPluginConvention) {
        javaPluginConvention.getSourceSets()?.all(object : Action<SourceSet> {
            override fun execute(sourceSet: SourceSet?) {
                if (sourceSet is ExtensionAware) {
                    val sourceSetName = sourceSet.getName()
                    val kotlinSourceSet = sourceSet.getExtensions().create("kotlin", javaClass<KotlinSourceSetImpl>(), sourceSetName, project.getFileResolver())!!

                    val kotlinDirSet = kotlinSourceSet.getKotlin()
                    kotlinDirSet.srcDir(project.file("src/${sourceSetName}/kotlin"))

                    sourceSet.getAllJava()?.source(kotlinDirSet)
                    sourceSet.getAllSource()?.source(kotlinDirSet)
                    sourceSet.getResources()?.getFilter()?.exclude(KSpec({ elem ->
                        kotlinDirSet.contains(elem.getFile())
                    }))

                    val kotlinTaskName = sourceSet.getCompileTaskName("kotlin")
                    val kotlinTask: KotlinCompile = project.getTasks().add(kotlinTaskName, javaClass<KotlinCompile>())!!

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
            val kdoc = project.getTasks()?.add(KDOC_TASK_NAME, javaClass<KDoc>())!!

            kdoc.setDescription("Generates KDoc API documentation for the main source code.")
            kdoc.setGroup(JavaBasePlugin.DOCUMENTATION_GROUP)
            kdoc.setSource(mainSourceSet.getConvention().getExtensionsAsDynamicObject().getProperty("kotlin"))
        }

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


open class KotlinAndroidPlugin: Plugin<Project> {

    val log = Logging.getLogger(getClass())

    public override fun apply(p0: Project) {

        val project = p0 as ProjectInternal
        val ext = project.getExtensions().getByName("android") as BaseExtension

        ext.getSourceSets()?.all(object : Action<AndroidSourceSet> {
            override fun execute(sourceSet: AndroidSourceSet?) {
                if (sourceSet is ExtensionAware) {
                    val sourceSetName = sourceSet.getName()
                    val kotlinSourceSet = sourceSet.getExtensions().create("kotlin", javaClass<KotlinSourceSetImpl>(), sourceSetName, project.getFileResolver())!!
                    val kotlinDirSet = kotlinSourceSet.getKotlin()
                    kotlinDirSet.srcDir(project.file("src/${sourceSetName}/kotlin"))

                    sourceSet.getAllJava().source(kotlinDirSet)
                    sourceSet.getAllSource().source(kotlinDirSet)
                    sourceSet.getResources()?.getFilter()?.exclude(KSpec({ elem ->
                        kotlinDirSet.contains(elem.getFile())
                    }))
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
    }

    private fun processVariants(variants: DefaultDomainObjectSet<out BaseVariant>, project: Project, androidExt: BaseExtension): Unit {
        val logger = project.getLogger()
        logger.debug("Kotlin plugin processing variants")
        val kotlinOptions = (androidExt as ExtensionAware).getExtensions().getByName("kotlinOptions") as K2JVMCompilerArguments
        val sourceSets = androidExt.getSourceSets()
        val mainSourceSet = sourceSets.getByName(BuilderConstants.MAIN)
        val testSourceSet = sourceSets.getByName(BuilderConstants.INSTRUMENT_TEST)
        logger.debug("Loaded ${BuilderConstants.MAIN} and ${BuilderConstants.INSTRUMENT_TEST} source sets")

        for (variant in variants) {
            logger.debug("Processing variant [${variant}]")

            if (variant is LibraryVariant || variant is ApkVariant) {
                val buildType: BuildType = if (variant is LibraryVariant) {
                    variant.getBuildType()
                } else {
                    (variant as ApkVariant).getBuildType()
                }

                val buildTypeSourceSetName = buildType.getName()
                logger.debug("Variant build type is [$buildTypeSourceSetName]")
                val buildTypeSourceSet : AndroidSourceSet? = sourceSets.findByName(buildTypeSourceSetName)

                val javaTask = variant.getJavaCompile()!!
                val variantName = variant.getName()

                val kotlinTaskName = "compile${variantName}Kotlin"
                val kotlinTask: KotlinCompile = project.getTasks()!!.add(kotlinTaskName, javaClass<KotlinCompile>())!!
                kotlinTask.kotlinOptions = kotlinOptions


                // store kotlin classes in separate directory. They will serve as class-path to java compiler
                val kotlinOutputDir = File(project.getBuildDir(), "kotlin-classes/${variantName}")
                kotlinTask.kotlinDestinationDir = kotlinOutputDir;
                kotlinTask.setDestinationDir(javaTask.getDestinationDir())
                kotlinTask.setDescription("Compiles the ${variantName} kotlin.")
                kotlinTask.setClasspath(javaTask.getClasspath())
                kotlinTask.setDependsOn(javaTask.getDependsOn())

                val javaSourceList = ArrayList<Any?>()

                if (variant is TestVariant) {
                    javaSourceList.addAll(testSourceSet.getJava()!!.getSrcDirs()!!)
                    val testKotlinSource = (testSourceSet as ExtensionAware).getExtensions().getByName("kotlin") as KotlinSourceSet
                    logger.debug("Attaching ${testKotlinSource.getKotlin().getSrcDirs()} to variant ${variantName}")
                    kotlinTask.source(testKotlinSource.getKotlin())
                } else {
                    javaSourceList.addAll(mainSourceSet.getJava()!!.getSrcDirs()!!)
                    val mainKotlinSource = (mainSourceSet as ExtensionAware).getExtensions().getByName("kotlin") as KotlinSourceSet
                    logger.debug("Attaching ${mainKotlinSource.getKotlin().getSrcDirs()} to variant ${variantName}")
                    kotlinTask.source(mainKotlinSource.getKotlin())
                }

                if (null != buildTypeSourceSet) {
                    javaSourceList.add(buildTypeSourceSet.getJava()!!.getSrcDirs()!!)
                    kotlinTask.source(((buildTypeSourceSet as ExtensionAware).getExtensions().getByName("kotlin") as KotlinSourceSet).getKotlin())
                }
                javaSourceList.add(callable<File>{ variant.getProcessResources()!!.getSourceOutputDir()!! })
                javaSourceList.add(callable<File>{ variant.getGenerateBuildConfig()!!.getSourceOutputDir()!! })
                javaSourceList.add(callable<File>{ variant.getAidlCompile()!!.getSourceOutputDir()!! })
                javaSourceList.add(callable<File>{ variant.getRenderscriptCompile()!!.getSourceOutputDir()!! })

                if (variant is ApkVariant) {
                    for (flavour in variant.getProductFlavors().iterator()) {
                       val flavourSourceSetName = buildTypeSourceSetName + flavour.getName()
                        val flavourSourceSet : AndroidSourceSet? = sourceSets.findByName(flavourSourceSetName)
                        if (flavourSourceSet != null) {
                            javaSourceList.add(flavourSourceSet.getJava())
                            kotlinTask.source((buildTypeSourceSet as ExtensionAware).getExtensions().getByName("kotlin"))
                        }
                    }
                }

                logger.debug("Kotlin Source list for [${variantName}]")
                kotlinTask.getSource().forEach { f -> logger.debug(f.getPath()) }
                logger.debug("Kotlin Source list end")

                val basePlugin : BasePlugin = project.getPlugins().getPlugin("android") as BasePlugin

                kotlinTask.doFirst({ task  ->
                    val javaSources = project.files(javaSourceList)
                    logger.debug("Java Source list for [${variantName}]")
                    javaSources?.forEach { f -> logger.debug(f.getPath()) }
                    logger.debug("Java Source list end")

                    val cp = javaTask.getClasspath() + javaSources
                    val cpWithRt = project.files(basePlugin.getRuntimeJarList())!! + cp
                    (task as AbstractCompile).setClasspath(cpWithRt)
                    logger.debug("Final Classpath for variant [${variantName}]")
                    cpWithRt?.forEach { f -> logger.debug(f.getPath()) }
                    logger.debug("Final Classpath end")
                })

                javaTask.dependsOn(kotlinTaskName)
                val javacClassPath = javaTask.getClasspath() + project.files(kotlinTask.kotlinDestinationDir);
                javaTask.setClasspath(javacClassPath)
            }
        }
    }
}

open class KSpec<T: Any?>(val predicate: (T) -> Boolean): Spec<T> {
    public override fun isSatisfiedBy(p0: T?): Boolean {
        return p0 != null && predicate(p0)
    }
}
