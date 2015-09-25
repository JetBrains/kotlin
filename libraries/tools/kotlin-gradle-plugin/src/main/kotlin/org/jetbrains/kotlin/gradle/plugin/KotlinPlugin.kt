package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import org.jetbrains.kotlin.gradle.internal.KotlinSourceSetImpl
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.internal.HasConvention
import org.jetbrains.kotlin.gradle.internal.KotlinSourceSet
import java.io.File
import org.gradle.api.Action
import org.gradle.api.tasks.compile.AbstractCompile
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.api.AndroidSourceSet
import com.android.build.gradle.BasePlugin
import com.android.build.gradle.internal.variant.BaseVariantData
import com.android.build.gradle.internal.variant.BaseVariantOutputData
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.initialization.dsl.ScriptHandler
import org.jetbrains.kotlin.gradle.plugin.android.AndroidGradleWrapper
import javax.inject.Inject
import org.gradle.api.file.SourceDirectorySet
import kotlin.properties.Delegates
import org.gradle.api.tasks.Delete
import groovy.lang.Closure
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.gradle.tasks.KotlinTasksProvider
import java.util.ServiceLoader
import org.gradle.api.logging.*
import org.gradle.api.plugins.*
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.internal.AnnotationProcessingManager
import org.jetbrains.kotlin.gradle.internal.initKapt
import java.net.URL
import java.util.jar.Manifest

val DEFAULT_ANNOTATIONS = "org.jebrains.kotlin.gradle.defaultAnnotations"

val KOTLIN_AFTER_JAVA_TASK_SUFFIX = "AfterJava"

abstract class KotlinSourceSetProcessor<T : AbstractCompile>(
        val project: ProjectInternal,
        val javaBasePlugin: JavaBasePlugin,
        val sourceSet: SourceSet,
        val pluginName: String,
        val compileTaskNameSuffix: String,
        val taskDescription: String,
        val compilerClass: Class<T>
) {
    abstract protected fun doTargetSpecificProcessing()
    val logger = Logging.getLogger(this.javaClass)

    protected val sourceSetName: String = sourceSet.getName()
    protected val sourceRootDir: String = "src/${sourceSetName}/kotlin"
    protected val absoluteSourceRootDir: String = project.getProjectDir().getPath() + "/" + sourceRootDir
    protected val kotlinSourceSet: KotlinSourceSet? by Delegates.lazy { createKotlinSourceSet() }
    protected val kotlinDirSet: SourceDirectorySet? by Delegates.lazy { createKotlinDirSet() }
    protected val kotlinTask: T by Delegates.lazy { createKotlinCompileTask() }
    protected val kotlinTaskName: String by Delegates.lazy { kotlinTask.getName() }

    public fun run() {
        if (kotlinSourceSet == null || kotlinDirSet == null) {
            return
        }
        addSourcesToKotlinDirSet()
        commonTaskConfiguration()
        doTargetSpecificProcessing()
    }

    open protected fun createKotlinSourceSet(): KotlinSourceSet? =
            if (sourceSet is HasConvention) {
                logger.kotlinDebug("Creating KotlinSourceSet for source set ${sourceSet}")
                val kotlinSourceSet = KotlinSourceSetImpl(sourceSet.getName(), project.getFileResolver())
                sourceSet.getConvention().getPlugins().put(pluginName, kotlinSourceSet)
                kotlinSourceSet
            } else {
                null
            }

    open protected fun createKotlinDirSet(): SourceDirectorySet? {
        val srcDir = project.file(sourceRootDir)
        logger.kotlinDebug("Creating Kotlin SourceDirectorySet for source set ${kotlinSourceSet} with src dir ${srcDir}")
        val kotlinDirSet = kotlinSourceSet?.getKotlin()
        kotlinDirSet?.srcDir(srcDir)
        return kotlinDirSet
    }

    open protected fun addSourcesToKotlinDirSet() {
        logger.kotlinDebug("Adding Kotlin SourceDirectorySet ${kotlinDirSet} to source set ${sourceSet}")
        sourceSet.getAllJava()?.source(kotlinDirSet)
        sourceSet.getAllSource()?.source(kotlinDirSet)
        sourceSet.getResources()?.getFilter()?.exclude { kotlinDirSet!!.contains(it.getFile()) }
    }

    open protected fun createKotlinCompileTask(suffix: String = ""): T {
        val name = sourceSet.getCompileTaskName(compileTaskNameSuffix) + suffix
        logger.kotlinDebug("Creating kotlin compile task $name with class $compilerClass")
        val compile = project.getTasks().create(name, compilerClass)
        compile.extensions.extraProperties.set("defaultModuleName", "${project.name}-$name")
        return compile
    }

    open protected fun commonTaskConfiguration() {
        javaBasePlugin.configureForSourceSet(sourceSet, kotlinTask)
        kotlinTask.setDescription(taskDescription)
        kotlinTask.source(kotlinDirSet)
    }
}

class Kotlin2JvmSourceSetProcessor(
        project: ProjectInternal,
        javaBasePlugin: JavaBasePlugin,
        sourceSet: SourceSet,
        val scriptHandler: ScriptHandler,
        val tasksProvider: KotlinTasksProvider
) : KotlinSourceSetProcessor<AbstractCompile>(
        project, javaBasePlugin, sourceSet,
        pluginName = "kotlin",
        compileTaskNameSuffix = "kotlin",
        taskDescription = "Compiles the $sourceSet.kotlin.",
        compilerClass = tasksProvider.kotlinJVMCompileTaskClass
) {

    private companion object {
        private var cachedKotlinAnnotationProcessingDep: String? = null
    }

    override fun doTargetSpecificProcessing() {
        // store kotlin classes in separate directory. They will serve as class-path to java compiler
        val kotlinDestinationDir = File(project.getBuildDir(), "kotlin-classes/${sourceSetName}")
        kotlinTask.setProperty("kotlinDestinationDir", kotlinDestinationDir)

        val javaTask = project.getTasks().findByName(sourceSet.getCompileJavaTaskName()) as AbstractCompile?

        if (javaTask != null) {
            javaTask.dependsOn(kotlinTaskName)
            val javacClassPath = javaTask.getClasspath() + project.files(kotlinDestinationDir);
            javaTask.setClasspath(javacClassPath)
        }

        val kotlinAnnotationProcessingDep = cachedKotlinAnnotationProcessingDep ?: run {
            val projectVersion = loadKotlinVersionFromResource(project.getLogger())
            val dep = "org.jetbrains.kotlin:kotlin-annotation-processing:$projectVersion"
            cachedKotlinAnnotationProcessingDep = dep
            dep
        }

        val aptConfiguration = project.createAptConfiguration(sourceSet.getName(), kotlinAnnotationProcessingDep)

        project afterEvaluate { project ->
            if (project != null) {
                for (dir in sourceSet.getJava().getSrcDirs()) {
                    kotlinDirSet?.srcDir(dir)
                }

                val subpluginEnvironment = loadSubplugins(project)
                subpluginEnvironment.addSubpluginArguments(project, kotlinTask)

                if (aptConfiguration.getDependencies().size() > 1 && javaTask is JavaCompile) {
                    val (aptOutputDir, aptWorkingDir) = project.getAptDirsForSourceSet(sourceSetName)

                    val kaptManager = AnnotationProcessingManager(kotlinTask, javaTask, sourceSetName,
                            aptConfiguration.resolve(), aptOutputDir, aptWorkingDir, tasksProvider.tasksLoader)

                    val kotlinAfterJavaTask = project.initKapt(kotlinTask, javaTask, kaptManager,
                            sourceSetName, kotlinDestinationDir, subpluginEnvironment) {
                        createKotlinCompileTask(it)
                    }

                    if (kotlinAfterJavaTask != null) {
                        javaTask.doFirst {
                            kotlinAfterJavaTask.setClasspath(project.files(kotlinTask.getClasspath(), javaTask.getDestinationDir()))
                        }
                    }
                }
            }
        }
    }
}

class Kotlin2JsSourceSetProcessor(
        project: ProjectInternal,
        javaBasePlugin: JavaBasePlugin,
        sourceSet: SourceSet,
        val scriptHandler: ScriptHandler,
        val tasksProvider: KotlinTasksProvider
) : KotlinSourceSetProcessor<AbstractCompile>(
        project, javaBasePlugin, sourceSet,
        pluginName = "kotlin2js",
        taskDescription = "Compiles the kotlin sources in $sourceSet to JavaScript.",
        compileTaskNameSuffix = "kotlin2Js",
        compilerClass = tasksProvider.kotlinJSCompileTaskClass
) {

    val copyKotlinJsTaskName = sourceSet.getTaskName("copy", "kotlinJs")
    val clean = project.getTasks().findByName("clean")
    val build = project.getTasks().findByName("build")

    val defaultKotlinDestinationDir = File(project.getBuildDir(), "kotlin2js/${sourceSetName}")
    private fun kotlinTaskDestinationDir(): File? = kotlinTask.property("kotlinDestinationDir") as File?
    private fun kotlinJsDestinationDir(): File? = (kotlinTask.property("outputFile") as String).let { File(it).directory }

    private fun kotlinSourcePathsForSourceMap() = sourceSet.getAllSource()
            .map { it.path }
            .filter { it.endsWith(".kt") }
            .map { it.replace(absoluteSourceRootDir, (kotlinTask.property("sourceMapDestinationDir") as File).getPath()) }

    private fun shouldGenerateSourceMap() = kotlinTask.property("sourceMap")

    override fun doTargetSpecificProcessing() {
        kotlinTask.setProperty("kotlinDestinationDir", defaultKotlinDestinationDir)
        build?.dependsOn(kotlinTaskName)
        clean?.dependsOn("clean" + kotlinTaskName.capitalize())

        createCleanSourceMapTask()
    }

    private fun createCleanSourceMapTask() {
        val taskName = sourceSet.getTaskName("clean", "sourceMap")
        val task = project.getTasks().create(taskName, javaClass<Delete>())
        task.onlyIf { kotlinTask.property("sourceMap") as Boolean }
        task.delete(object : Closure<String>(this) {
            override fun call(): String? = (kotlinTask.property("outputFile") as String) + ".map"
        })
        clean?.dependsOn(taskName)
    }
}


abstract class AbstractKotlinPlugin @Inject constructor(val scriptHandler: ScriptHandler, val tasksProvider: KotlinTasksProvider) : Plugin<Project> {
    abstract fun buildSourceSetProcessor(project: ProjectInternal, javaBasePlugin: JavaBasePlugin, sourceSet: SourceSet): KotlinSourceSetProcessor<*>

    public override fun apply(project: Project) {
        val javaBasePlugin = project.getPlugins().apply(javaClass<JavaBasePlugin>())
        val javaPluginConvention = project.getConvention().getPlugin(javaClass<JavaPluginConvention>())

        project.getPlugins().apply(javaClass<JavaPlugin>())

        configureSourceSetDefaults(project as ProjectInternal, javaBasePlugin, javaPluginConvention)

        val gradleUtils = GradleUtils(scriptHandler, project)
        project.getExtensions().add(DEFAULT_ANNOTATIONS, gradleUtils.resolveKotlinPluginDependency("kotlin-jdk-annotations"))
    }

    open protected fun configureSourceSetDefaults(project: ProjectInternal,
                                                  javaBasePlugin: JavaBasePlugin,
                                                  javaPluginConvention: JavaPluginConvention) {
        javaPluginConvention.getSourceSets()?.all(Action<SourceSet> { sourceSet ->
            if (sourceSet != null) {
                buildSourceSetProcessor(project, javaBasePlugin, sourceSet).run()
            }
        })
    }
}


open class KotlinPlugin @Inject constructor(scriptHandler: ScriptHandler, tasksProvider: KotlinTasksProvider) : AbstractKotlinPlugin(scriptHandler, tasksProvider) {
    override fun buildSourceSetProcessor(project: ProjectInternal, javaBasePlugin: JavaBasePlugin, sourceSet: SourceSet) =
            Kotlin2JvmSourceSetProcessor(project, javaBasePlugin, sourceSet, scriptHandler, tasksProvider)

    override fun apply(project: Project) {
        project.createKaptExtension()
        super.apply(project)
    }
}


open class Kotlin2JsPlugin @Inject constructor(scriptHandler: ScriptHandler, tasksProvider: KotlinTasksProvider) : AbstractKotlinPlugin(scriptHandler, tasksProvider) {
    override fun buildSourceSetProcessor(project: ProjectInternal, javaBasePlugin: JavaBasePlugin, sourceSet: SourceSet) =
            Kotlin2JsSourceSetProcessor(project, javaBasePlugin, sourceSet, scriptHandler, tasksProvider)
}


open class KotlinAndroidPlugin @Inject constructor(val scriptHandler: ScriptHandler, val tasksProvider: KotlinTasksProvider) : Plugin<Project> {

    val log = Logging.getLogger(this.javaClass)

    public override fun apply(p0: Project) {

        val project = p0 as ProjectInternal
        val ext = project.getExtensions().getByName("android") as BaseExtension

        val version = loadAndroidPluginVersion()
        if (version != null) {
            val minimalVersion = "1.1.0"
            if (compareVersionNumbers(version, minimalVersion) < 0) {
                throw IllegalStateException("Kotlin: Unsupported version of com.android.tools.build:gradle plugin: version $minimalVersion or higher should be used with kotlin-android plugin")
            }
        }

        val aptConfigurations = hashMapOf<String, Configuration>()

        val projectVersion = loadKotlinVersionFromResource(log)
        val kotlinAnnotationProcessingDep = "org.jetbrains.kotlin:kotlin-annotation-processing:$projectVersion"

        ext.getSourceSets().all(Action<AndroidSourceSet> { sourceSet ->
            if (sourceSet is HasConvention) {
                val sourceSetName = sourceSet.getName()
                val kotlinSourceSet = KotlinSourceSetImpl(sourceSetName, project.getFileResolver())
                sourceSet.getConvention().getPlugins().put("kotlin", kotlinSourceSet)
                val kotlinDirSet = kotlinSourceSet.getKotlin()
                kotlinDirSet.srcDir(project.file("src/${sourceSetName}/kotlin"))

                aptConfigurations.put(sourceSet.getName(),
                        project.createAptConfiguration(sourceSet.getName(), kotlinAnnotationProcessingDep))

                /*TODO: before 0.11 gradle android plugin there was:
                  sourceSet.getAllJava().source(kotlinDirSet)
                  sourceSet.getAllSource().source(kotlinDirSet)
                  AndroidGradleWrapper.getResourceFilter(sourceSet)?.exclude(KSpec({ elem ->
                    kotlinDirSet.contains(elem.getFile())
                  }))
                 but those methods were removed so commented as temporary hack*/

                project.getLogger().kotlinDebug("Created kotlin sourceDirectorySet at ${kotlinDirSet.getSrcDirs()}")
            }
        })

        (ext as ExtensionAware).getExtensions().add("kotlinOptions", tasksProvider.kotlinJVMOptionsClass)

        project.createKaptExtension()

        project afterEvaluate { project ->
            if (project != null) {
                val plugin = (project.getPlugins().findPlugin("android")
                                ?: project.getPlugins().findPlugin("android-library")) as BasePlugin

                val variantManager = AndroidGradleWrapper.getVariantDataManager(plugin)
                processVariantData(variantManager.getVariantDataList(), project,
                        ext, plugin, aptConfigurations)
            }
        }

        project.getExtensions().add(DEFAULT_ANNOTATIONS, GradleUtils(scriptHandler, project)
                .resolveKotlinPluginDependency("kotlin-android-sdk-annotations"))
    }

    private fun processVariantData(
            variantDataList: List<BaseVariantData<out BaseVariantOutputData>>,
            project: Project,
            androidExt: BaseExtension,
            androidPlugin: BasePlugin,
            aptConfigurations: Map<String, Configuration>
    ) {
        val logger = project.getLogger()
        val kotlinOptions = getExtension<Any?>(androidExt, "kotlinOptions")

        val subpluginEnvironment = loadSubplugins(project)

        for (variantData in variantDataList) {
            val variantDataName = variantData.getName()
            logger.kotlinDebug("Process variant [$variantDataName]")

            val javaTask = AndroidGradleWrapper.getJavaCompile(variantData)
            if (javaTask == null) {
                logger.info("KOTLIN: javaTask is missing for $variantDataName, so Kotlin files won't be compiled for it")
                continue
            }

            val kotlinTaskName = "compile${variantDataName.capitalize()}Kotlin"
            val kotlinTask = tasksProvider.createKotlinJVMTask(project, kotlinTaskName)

            kotlinTask.extensions.extraProperties.set("defaultModuleName", "${project.name}-$kotlinTaskName")
            if (kotlinOptions != null) {
                kotlinTask.setProperty("kotlinOptions", kotlinOptions)
            }

            // store kotlin classes in separate directory. They will serve as class-path to java compiler
            val kotlinOutputDir = File(project.getBuildDir(), "tmp/kotlin-classes/${variantDataName}")
            kotlinTask.setProperty("kotlinDestinationDir", kotlinOutputDir)
            kotlinTask.setDestinationDir(javaTask.getDestinationDir())
            kotlinTask.setDescription("Compiles the ${variantDataName} kotlin.")
            kotlinTask.setClasspath(javaTask.getClasspath())
            kotlinTask.setDependsOn(javaTask.getDependsOn())

            fun SourceDirectorySet.addSourceDirectories(additionalSourceFiles: Collection<File>) {
                for (dir in additionalSourceFiles) {
                    this.srcDir(dir)
                    logger.kotlinDebug("Source directory ${dir.getAbsolutePath()} was added to kotlin source for $kotlinTaskName")
                }
            }

            fun configureDefaultSourceSet() {
                val defaultSourceSet = variantData.getVariantConfiguration().getDefaultSourceSet()
                val kotlinSourceDirectorySet = getExtension<KotlinSourceSet>(defaultSourceSet, "kotlin").getKotlin()
                // getJavaSources should return the Java sources used for compilation
                // We want to collect only generated files, like R-class output dir
                // Actual java sources will be collected later
                val additionalSourceFiles = variantData.getJavaSources().filterIsInstance(javaClass<File>())
                kotlinSourceDirectorySet.addSourceDirectories(additionalSourceFiles)
            }

            configureDefaultSourceSet()

            val aptFiles = arrayListOf<File>()

            // getSortedSourceProviders should return only actual java sources, generated sources should be collected earlier
            val providers = variantData.getVariantConfiguration().getSortedSourceProviders()
            for (provider in providers) {
                val javaSrcDirs = AndroidGradleWrapper.getJavaSrcDirs(provider as AndroidSourceSet)
                val kotlinSourceSet = getExtension<KotlinSourceSet>(provider, "kotlin")
                val kotlinSourceDirectorySet = kotlinSourceSet.getKotlin()
                kotlinTask.source(kotlinSourceDirectorySet)

                kotlinSourceDirectorySet.addSourceDirectories(javaSrcDirs)

                val aptConfiguration = aptConfigurations[(provider as AndroidSourceSet).getName()]
                // Ignore if there's only an annotation processor wrapper in dependencies (added by default)
                if (aptConfiguration != null && aptConfiguration.getDependencies().size() > 1) {
                    aptFiles.addAll(aptConfiguration.resolve())
                }
            }

            subpluginEnvironment.addSubpluginArguments(project, kotlinTask)

            kotlinTask doFirst {
                val androidRT = project.files(AndroidGradleWrapper.getRuntimeJars(androidPlugin, androidExt))
                val fullClasspath = (javaTask.getClasspath() + androidRT) - project.files(kotlinTask.property("kotlinDestinationDir"))
                (it as AbstractCompile).setClasspath(fullClasspath)

                for (task in project.getTasksByName(kotlinTaskName + KOTLIN_AFTER_JAVA_TASK_SUFFIX, false)) {
                    (task as AbstractCompile).setClasspath(project.files(fullClasspath, javaTask.getDestinationDir()))
                }
            }

            javaTask.dependsOn(kotlinTaskName)

            val (aptOutputDir, aptWorkingDir) = project.getAptDirsForSourceSet(variantDataName)
            variantData.addJavaSourceFoldersToModel(aptOutputDir)

            if (javaTask is JavaCompile && aptFiles.isNotEmpty()) {
                val kaptManager = AnnotationProcessingManager(kotlinTask, javaTask, variantDataName,
                        aptFiles.toSet(), aptOutputDir, aptWorkingDir, tasksProvider.tasksLoader, variantData)

                kotlinTask.storeKaptAnnotationsFile(kaptManager)

                project.initKapt(kotlinTask, javaTask, kaptManager, variantDataName, kotlinOutputDir, subpluginEnvironment) {
                    tasksProvider.createKotlinJVMTask(project, kotlinTaskName + KOTLIN_AFTER_JAVA_TASK_SUFFIX)
                }
            }

            javaTask doFirst {
                javaTask.setClasspath(javaTask.getClasspath() + project.files(kotlinTask.property("kotlinDestinationDir")))
            }
        }
    }

    fun <T> getExtension(obj: Any, extensionName: String): T {
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

private fun loadSubplugins(project: Project): SubpluginEnvironment {
    try {
        val subplugins = ServiceLoader.load(
            javaClass<KotlinGradleSubplugin>(), project.getBuildscript().getClassLoader()).toList()
        val subpluginDependencyNames =
            subplugins.mapTo(hashSetOf<String>()) { it.getGroupName() + ":" + it.getArtifactName() }

        val classpath = project.getBuildscript().getConfigurations().getByName("classpath")
        val subpluginClasspaths = hashMapOf<KotlinGradleSubplugin, List<String>>()

        for (subplugin in subplugins) {
            val files = classpath.getDependencies()
                    .filter { subpluginDependencyNames.contains(it.getGroup() + ":" + it.getName()) }
                    .flatMap { classpath.files(it).map { it.getAbsolutePath() } }
            subpluginClasspaths.put(subplugin, files)
        }

        return SubpluginEnvironment(subpluginClasspaths, subplugins)
    } catch (e: NoClassDefFoundError) {
        // Skip plugin loading if KotlinGradleSubplugin is not defined.
        // It is true now for tests in kotlin-gradle-plugin-core.
        return SubpluginEnvironment(mapOf(), listOf())
    }
}

class SubpluginEnvironment(
    val subpluginClasspaths: Map<KotlinGradleSubplugin, List<String>>,
    val subplugins: List<KotlinGradleSubplugin>
) {

    fun addSubpluginArguments(project: Project, compileTask: AbstractCompile) {
        val realPluginClasspaths = arrayListOf<String>()
        val pluginArguments = arrayListOf<String>()
        fun getPluginOptionString(pluginId: String, key: String, value: String) = "plugin:$pluginId:$key=$value"

        subplugins.forEach { subplugin ->
            val args = subplugin.getExtraArguments(project, compileTask)

            with (subplugin) {
                project.getLogger().kotlinDebug("Subplugin ${getPluginName()} (${getGroupName()}:${getArtifactName()}) loaded.")
            }

            val subpluginClasspath = subpluginClasspaths[subplugin]
            if (args != null && subpluginClasspath != null) {
                realPluginClasspaths.addAll(subpluginClasspath)
                for (arg in args) {
                    val option = getPluginOptionString(subplugin.getPluginName(), arg.key, arg.value)
                    pluginArguments.add(option)
                }
            }
        }

        val extraProperties = compileTask.extensions.extraProperties
        extraProperties.set("compilerPluginClasspaths", realPluginClasspaths.toTypedArray())
        extraProperties.set("compilerPluginArguments", pluginArguments.toTypedArray())
    }
}

open class GradleUtils(val scriptHandler: ScriptHandler, val project: ProjectInternal) {
    public fun resolveDependencies(vararg coordinates: String): Collection<File> {
        val dependencyHandler: DependencyHandler = scriptHandler.getDependencies()
        val configurationsContainer: ConfigurationContainer = scriptHandler.getConfigurations()

        val deps = coordinates map { dependencyHandler.create(it) }
        val configuration = configurationsContainer.detachedConfiguration(*deps.toTypedArray())

        return configuration.getResolvedConfiguration().getFiles { true }
    }

    public fun kotlinPluginVersion(): String = project.getProperties()["kotlin.gradle.plugin.version"] as String
    public fun kotlinPluginArtifactCoordinates(artifact: String): String = "org.jetbrains.kotlin:${artifact}:${kotlinPluginVersion()}"
    public fun kotlinJsLibraryCoordinates(): String = kotlinPluginArtifactCoordinates("kotlin-js-library")

    public fun resolveKotlinPluginDependency(artifact: String): Collection<File> =
            resolveDependencies(kotlinPluginArtifactCoordinates(artifact))
    public fun resolveJsLibrary(): File = resolveDependencies(kotlinJsLibraryCoordinates()).first()
}

internal operator fun FileCollection.plus(other: FileCollection) = this.plus(other)
internal operator fun FileCollection.minus(other: FileCollection) = this.minus(other)

fun AbstractCompile.storeKaptAnnotationsFile(kapt: AnnotationProcessingManager) {
    extensions.extraProperties.set("kaptAnnotationsFile", kapt.getAnnotationFile())
}

private fun Project.getAptDirsForSourceSet(sourceSetName: String): Pair<File, File> {
    val aptOutputDir = File(getBuildDir(), "generated/source/kapt")
    val aptOutputDirForVariant = File(aptOutputDir, sourceSetName)

    val aptWorkingDir = File(getBuildDir(), "tmp/kapt")
    val aptWorkingDirForVariant = File(aptWorkingDir, sourceSetName)

    return aptOutputDirForVariant to aptWorkingDirForVariant
}

private fun Project.createAptConfiguration(sourceSetName: String, kotlinAnnotationProcessingDep: String): Configuration {
    val aptConfigurationName = if (sourceSetName != "main") "kapt${sourceSetName.capitalize()}" else "kapt"

    val aptConfiguration = getConfigurations().create(aptConfigurationName)
    aptConfiguration.getDependencies().add(getDependencies().create(kotlinAnnotationProcessingDep))

    return aptConfiguration
}

private fun Project.createKaptExtension() {
    getExtensions().create("kapt", javaClass<KaptExtension>())
}

//copied from BasePlugin.getLocalVersion
private fun loadAndroidPluginVersion(): String? {
    try {
        val clazz = javaClass<BasePlugin>()
        val className = clazz.getSimpleName() + ".class"
        val classPath = clazz.getResource(className).toString()
        if (!classPath.startsWith("jar")) {
            // Class not from JAR, unlikely
            return null
        }
        val manifestPath = classPath.substring(0, classPath.lastIndexOf("!") + 1) + "/META-INF/MANIFEST.MF";

        val jarConnection = URL(manifestPath).openConnection();
        jarConnection.setUseCaches(false);
        val jarInputStream = jarConnection.getInputStream();
        val attr = Manifest(jarInputStream).getMainAttributes();
        jarInputStream.close();
        return attr.getValue("Plugin-Version");
    } catch (t: Throwable) {
        return null;
    }
}

//Copied from StringUtil.compareVersionNumbers
private fun compareVersionNumbers(v1: String?, v2: String?): Int {
    if (v1 == null && v2 == null) {
        return 0
    }
    if (v1 == null) {
        return -1
    }
    if (v2 == null) {
        return 1
    }

    val pattern = "[\\.\\_\\-]".toRegex()
    val digitsPattern = "\\d+".toRegex()
    val part1 = v1.split(pattern)
    val part2 = v2.split(pattern)

    var idx = 0
    while (idx < part1.size() && idx < part2.size()) {
        val p1 = part1[idx]
        val p2 = part2[idx]

        val cmp: Int
        if (p1.matches(digitsPattern) && p2.matches(digitsPattern)) {
            cmp = p1.toInt().compareTo(p2.toInt())
        } else {
            cmp = part1[idx].compareTo(part2[idx])
        }
        if (cmp != 0) return cmp
        idx++
    }

    if (part1.size() == part2.size()) {
        return 0
    } else {
        val left = part1.size() > idx
        val parts = if (left) part1 else part2

        while (idx < parts.size()) {
            val p = parts[idx]
            val cmp: Int
            if (p.matches(digitsPattern)) {
                cmp = Integer(p).compareTo(0)
            } else {
                cmp = 1
            }
            if (cmp != 0) return if (left) cmp else -cmp
            idx++
        }
        return 0
    }
}