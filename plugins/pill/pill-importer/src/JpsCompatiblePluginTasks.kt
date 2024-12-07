/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.pill

import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import org.gradle.kotlin.dsl.extra
import org.jdom2.Element
import org.jdom2.Verifier
import org.jdom2.input.SAXBuilder
import org.jdom2.output.Format
import org.jdom2.output.XMLOutputter
import org.jetbrains.kotlin.pill.model.PDependency
import org.jetbrains.kotlin.pill.model.PLibrary
import org.jetbrains.kotlin.pill.model.POrderRoot
import org.jetbrains.kotlin.pill.model.PProject
import java.io.File
import java.util.*

const val EMBEDDED_CONFIGURATION_NAME = "embedded"

class JpsCompatiblePluginTasks(
    private val rootProject: Project,
    private val platformDir: File,
    private val resourcesDir: File
) {
    companion object {
        private val DIST_LIBRARIES = listOf(
            ":kotlin-annotations-jvm",
            ":kotlin-stdlib",
            ":kotlin-stdlib-jdk7",
            ":kotlin-stdlib-jdk8",
            ":kotlin-reflect",
            ":kotlin-test",
            ":kotlin-script-runtime"
        )

        private val IGNORED_LIBRARIES = listOf(
            // Libraries
            ":kotlin-stdlib-common",
            ":kotlin-serialization",
            ":kotlin-test:kotlin-test-common",
            ":kotlin-test:kotlin-test-annotations-common",
            // Other
            ":kotlin-compiler",
            ":kotlin-daemon-embeddable",
            ":kotlin-compiler-embeddable",
            ":kotlin-android-extensions",
            ":kotlin-scripting-compiler-embeddable",
            ":kotlin-scripting-compiler-impl-embeddable",
            ":kotlin-scripting-jvm-host"
        )

        private val MAPPED_LIBRARIES = mapOf(
            ":kotlin-reflect-api/main" to ":kotlin-reflect/main",
            ":kotlin-reflect-api/java9" to ":kotlin-reflect/main"
        )

        private val LIB_DIRECTORIES = listOf("dependencies", "dist")

        private val ALLOWED_ARTIFACT_PATTERNS = listOf(
            Regex("kotlinx_cli_jvm_[\\d_]+_SNAPSHOT\\.xml"),
            Regex("kotlin_test_wasm_js_[\\d_]+_SNAPSHOT\\.xml"),
            Regex("kotlin_dom_api_compat_[\\d_]+_SNAPSHOT\\.xml")
        )

        private val EXCLUDED_DIRECTORY_PATHS = listOf("out")
    }

    private lateinit var projectDir: File
    private lateinit var platformVersion: String
    private lateinit var platformBaseNumber: String
    private lateinit var intellijCoreDir: File

    private fun initEnvironment(project: Project) {
        projectDir = project.projectDir
        platformVersion = project.extensions.extraProperties.get("versions.intellijSdk").toString()
        platformBaseNumber = platformVersion.substringBefore(".", "").takeIf { it.isNotEmpty() }
            ?: platformVersion.substringBefore("-", "").takeIf { it.isNotEmpty() }
                    ?: error("Invalid platform version: $platformVersion")
        intellijCoreDir = File(platformDir.parentFile.parentFile.parentFile, "intellij-core")
    }

    fun pill() {
        initEnvironment(rootProject)

        rootProject.logger.lifecycle("Pill: Setting up project...")

        val modulePrefix = System.getProperty("pill.module.prefix", "")
        val globalExcludedDirectories = EXCLUDED_DIRECTORY_PATHS.map { File(rootProject.projectDir, it) }
        val modelParser = ModelParser(modulePrefix, globalExcludedDirectories)

        val dependencyPatcher = DependencyPatcher(rootProject)
        val dependencyMappers = listOf(dependencyPatcher, ::attachPlatformSources, ::attachAsmSources)

        val jpsProject = modelParser.parse(rootProject)
            .mapDependencies(dependencyMappers)
            .copy(libraries = dependencyPatcher.libraries)

        val files = render(jpsProject)

        removeExistingIdeaLibrariesAndModules()
        removeJpsAndPillRunConfigurations()
        removeArtifactConfigurations()

        copyRunConfigurations()
        setOptionsForDefaultJunitRunConfiguration(rootProject)

        files.forEach { it.write() }
    }

    fun unpill() {
        initEnvironment(rootProject)

        removeExistingIdeaLibrariesAndModules()
        removeJpsAndPillRunConfigurations()
        removeArtifactConfigurations()
    }

    private fun removeExistingIdeaLibrariesAndModules() {
        File(projectDir, ".idea/libraries").deleteRecursively()
        File(projectDir, ".idea/modules").deleteRecursively()
    }

    private fun removeJpsAndPillRunConfigurations() {
        File(projectDir, ".idea/runConfigurations")
            .walk()
            .filter { (it.name.startsWith("JPS_") || it.name.startsWith("Pill_")) && it.extension.lowercase(Locale.US) == "xml" }
            .forEach { it.delete() }
    }

    private fun removeArtifactConfigurations() {
        File(projectDir, ".idea/artifacts")
            .walk()
            .filter { it.extension.lowercase(Locale.US) == "xml" && ALLOWED_ARTIFACT_PATTERNS.none { p -> p.matches(it.name) } }
            .forEach { it.delete() }
    }

    private fun copyRunConfigurations() {
        val runConfigurationsDir = File(resourcesDir, "runConfigurations")
        val targetDir = File(projectDir, ".idea/runConfigurations")
        val platformDirProjectRelative = "\$PROJECT_DIR\$/" + platformDir.toRelativeString(projectDir)

        targetDir.mkdirs()

        fun substitute(text: String): String {
            return text.replace("\$IDEA_HOME_PATH\$", platformDirProjectRelative)
        }

        (runConfigurationsDir.listFiles() ?: emptyArray())
            .filter { it.extension == "xml" }
            .map { it.name to substitute(it.readText()) }
            .forEach { File(targetDir, it.first).writeText(it.second) }
    }

    /*
        This sets a proper (project root) working directory and a "idea.home.path" property to the default JUnit configuration,
        so one does not need to make these changes manually.
     */
    private fun setOptionsForDefaultJunitRunConfiguration(project: Project) {
        val workspaceFile = File(projectDir, ".idea/workspace.xml")
        if (!workspaceFile.exists()) {
            project.logger.warn("${workspaceFile.name} does not exist, JUnit default run configuration was not modified")
            return
        }

        val document = SAXBuilder().build(workspaceFile)
        val rootElement = document.rootElement

        fun Element.getOrCreateChild(name: String, vararg attributes: Pair<String, String>): Element {
            for (child in getChildren(name)) {
                if (attributes.all { (attribute, value) -> child.getAttributeValue(attribute) == value }) {
                    return child
                }
            }

            return Element(name).apply {
                for ((attributeName, value) in attributes) {
                    setAttribute(attributeName, value)
                }

                this@getOrCreateChild.addContent(this@apply)
            }
        }

        val platformDirProjectRelative = "\$PROJECT_DIR\$/" + platformDir.toRelativeString(projectDir)

        val runManagerComponent = rootElement.getOrCreateChild("component", "name" to "RunManager")

        val junitConfiguration = runManagerComponent.getOrCreateChild(
            "configuration",
            "default" to "true",
            "type" to "JUnit",
            "factoryName" to "JUnit"
        )

        val kotlinJunitConfiguration = runManagerComponent.getOrCreateChild(
            "configuration",
            "default" to "true",
            "type" to "KotlinJUnit",
            "factoryName" to "Kotlin JUnit"
        )

        fun Element.applyJUnitTemplate() {
            getOrCreateChild("option", "name" to "WORKING_DIRECTORY").setAttribute("value", "file://\$PROJECT_DIR\$")
            getOrCreateChild("option", "name" to "VM_PARAMETERS").also { vmParams ->
                var options = vmParams.getAttributeValue("value", "")
                    .split(' ')
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }

                fun addOptionIfAbsent(name: String) {
                    if (options.none { it == name }) {
                        options = options + name
                    }
                }

                fun addOrReplaceOptionValue(name: String, value: Any?, prefix: String = "-D") {
                    val optionsWithoutNewValue = options.filter { !it.startsWith("$prefix$name=") }
                    options = if (value == null) optionsWithoutNewValue else (optionsWithoutNewValue + listOf("$prefix$name=$value"))
                }

                addOptionIfAbsent("-ea")
                addOptionIfAbsent("-XX:+HeapDumpOnOutOfMemoryError")
                addOptionIfAbsent("-Xmx1600m")
                addOptionIfAbsent("-XX:+UseCodeCacheFlushing")

                addOrReplaceOptionValue("ReservedCodeCacheSize", "128m", prefix = "-XX:")
                addOrReplaceOptionValue("jna.nosys", "true")
                addOrReplaceOptionValue("idea.is.unit.test", "true")
                addOrReplaceOptionValue("idea.ignore.disabled.plugins", "true")
                addOrReplaceOptionValue("idea.home.path", platformDirProjectRelative)
                addOrReplaceOptionValue("idea.use.native.fs.for.win", "false")
                addOrReplaceOptionValue("use.jps", "true")
                addOrReplaceOptionValue("kotlinVersion", project.rootProject.extra["kotlinVersion"].toString())
                addOrReplaceOptionValue("java.awt.headless", "true")

                val isAndroidStudioBunch = project.findProperty("versions.androidStudioRelease") != null
                addOrReplaceOptionValue("idea.platform.prefix", if (isAndroidStudioBunch) "AndroidStudio" else "Idea")

                val androidJarPath = project.configurations.findByName("androidJar")?.singleFile
                val androidSdkPath = project.configurations.findByName("androidSdk")?.singleFile

                if (androidJarPath != null && androidSdkPath != null) {
                    addOrReplaceOptionValue("android.sdk", "\$PROJECT_DIR\$/" + androidSdkPath.toRelativeString(projectDir))
                    addOrReplaceOptionValue("android.jar", "\$PROJECT_DIR\$/" + androidJarPath.toRelativeString(projectDir))
                }

                vmParams.setAttribute("value", options.joinToString(" "))
            }
        }

        junitConfiguration.applyJUnitTemplate()
        kotlinJunitConfiguration.applyJUnitTemplate()

        val output = XMLOutputter().also {
            it.format = Format.getPrettyFormat().apply {
                setEscapeStrategy { c -> Verifier.isHighSurrogate(c) || c == '"' }
                setIndent("  ")
                setTextMode(Format.TextMode.TRIM)
                setOmitEncoding(false)
                setOmitDeclaration(false)
            }
        }

        val postProcessedXml = output.outputString(document)
            .replace("&#x22;", "&quot;")
            .replace("&#xA;", "&#10;")
            .replace("&#xC;", "&#13;")

        workspaceFile.writeText(postProcessedXml)
    }

    private class DependencyPatcher(private val rootProject: Project) : Function2<PProject, PDependency, List<PDependency>> {
        private val mappings: Map<String, Optional<PLibrary>> = run {
            val distLibDir = File(rootProject.extra["distLibDir"].toString())
            val result = HashMap<String, Optional<PLibrary>>()

            fun List<File>.filterExisting() = filter { it.exists() }

            for (path in DIST_LIBRARIES) {
                val project = rootProject.findProject(path) ?: error("Project '$path' not found")

                for (sourceSet in computeAllSourceSets(project)) {
                    if (sourceSet.isTest) {
                        continue
                    }

                    val jarTask = sourceSet.findJarTask(project) ?: continue

                    val archiveName = listOfNotNull(jarTask.archiveBaseName.get(), jarTask.archiveAppendix.orNull).joinToString("-")
                    val classesJars = listOf(File(distLibDir, "$archiveName.jar")).filterExisting()
                    val sourcesJars = listOf(File(distLibDir, "$archiveName-sources.jar")).filterExisting()
                    val optLibrary = Optional.of(PLibrary(archiveName, classesJars, sourcesJars, originalName = path))

                    result[path + "/" + sourceSet.name] = optLibrary

                    val java9SourceSetName = when (val sourceSetBaseName = sourceSet.baseName) {
                        SourceSet.MAIN_SOURCE_SET_NAME -> "java9"
                        else -> sourceSetBaseName + "Java9"
                    }

                    result["$path/$java9SourceSetName"] = optLibrary
                }
            }

            for (path in IGNORED_LIBRARIES) {
                result["$path/main"] = Optional.empty<PLibrary>()
            }

            for ((old, new) in MAPPED_LIBRARIES) {
                result[old] = result[new] ?: error("Mapped library $old -> $new not found")
            }

            return@run result
        }

        val libraries: List<PLibrary> = mappings.values.filter { it.isPresent }.map { it.get() }

        override fun invoke(project: PProject, dependency: PDependency): List<PDependency> {
            if (dependency !is PDependency.ModuleLibrary) {
                return listOf(dependency)
            }

            val root = dependency.library.classes.singleOrNull() ?: return listOf(dependency)
            val paths = project.artifacts[root.absolutePath]

            if (paths == null) {
                val projectDir = rootProject.projectDir
                if (projectDir.isParent(root) && LIB_DIRECTORIES.none { File(projectDir, it).isParent(root) }) {
                    rootProject.logger.warn("Paths not found for root: ${root.absolutePath}")
                    return emptyList()
                }
                return listOf(dependency)
            }

            val result = mutableListOf<PDependency>()
            for (path in paths) {
                val module = project.modules.find { it.path == path }
                if (module != null) {
                    result += PDependency.Module(module)
                    result += module.embeddedDependencies.flatMap { invoke(project, it) }
                    continue
                }

                val maybeLibrary = mappings[path]
                if (maybeLibrary == null) {
                    rootProject.logger.warn("Library not found for root: ${root.absolutePath} ($path)")
                    continue
                }

                if (maybeLibrary.isPresent) {
                    result += PDependency.Library(maybeLibrary.get().name)
                }
            }

            return result
        }

        private fun File.isParent(child: File): Boolean {
            var parent = child.parentFile ?: return false
            while (true) {
                if (parent == this) {
                    return true
                }
                parent = parent.parentFile ?: return false
            }
        }
    }

    private fun attachPlatformSources(@Suppress("UNUSED_PARAMETER") project: PProject, dependency: PDependency): List<PDependency> {
        if (dependency is PDependency.ModuleLibrary) {
            val library = dependency.library
            val platformSourcesJar = File(platformDir, "../../../sources/intellij-$platformVersion-sources.jar")

            if (library.classes.any { it.startsWith(platformDir) || it.startsWith(intellijCoreDir) }) {
                return listOf(dependency.copy(library = library.attachSource(platformSourcesJar)))
            }
        }

        return listOf(dependency)
    }

    private fun attachAsmSources(@Suppress("UNUSED_PARAMETER") project: PProject, dependency: PDependency): List<PDependency> {
        if (dependency is PDependency.ModuleLibrary) {
            val library = dependency.library
            val asmSourcesJar = File(platformDir, "../asm-shaded-sources/asm-src-$platformBaseNumber.jar")
            val asmAllJar = File(platformDir, "lib/asm-all.jar")

            if (library.classes.any { it == asmAllJar }) {
                return listOf(dependency.copy(library = library.attachSource(asmSourcesJar)))
            }
        }

        return listOf(dependency)
    }

    private fun PProject.mapDependencies(mappers: List<(PProject, PDependency) -> List<PDependency>>): PProject {
        fun mapRoot(root: POrderRoot): List<POrderRoot> {
            val dependencies = mapDependency(root.dependency, mappers)
            return dependencies.map { root.copy(dependency = it) }
        }

        val modules = this.modules.map { module ->
            val newOrderRoots = module.orderRoots.flatMap(::mapRoot).distinct()
            module.copy(orderRoots = newOrderRoots)
        }

        return this.copy(modules = modules)
    }

    private fun PProject.mapDependency(
        dependency: PDependency,
        mappers: List<(PProject, PDependency) -> List<PDependency>>
    ): List<PDependency> {
        var dependencies = listOf(dependency)
        for (mapper in mappers) {
            val newDependencies = mutableListOf<PDependency>()
            for (dep in dependencies) {
                newDependencies += mapper(this, dep)
            }
            dependencies = newDependencies
        }

        return dependencies
    }
}
