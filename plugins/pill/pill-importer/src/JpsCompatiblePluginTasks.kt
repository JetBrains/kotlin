/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.pill

import org.gradle.api.Project
import org.gradle.api.plugins.BasePluginConvention
import org.gradle.kotlin.dsl.extra
import shadow.org.jdom2.input.SAXBuilder
import shadow.org.jdom2.*
import shadow.org.jdom2.output.Format
import shadow.org.jdom2.output.XMLOutputter
import java.io.File
import java.util.*
import kotlin.collections.HashMap

const val EMBEDDED_CONFIGURATION_NAME = "embedded"

class JpsCompatiblePluginTasks(private val rootProject: Project, private val platformDir: File, private val resourcesDir: File) {
    companion object {
        private val DIST_LIBRARIES = listOf(
            ":kotlin-annotations-jvm",
            ":kotlin-stdlib",
            ":kotlin-stdlib-jdk7",
            ":kotlin-stdlib-jdk8",
            ":kotlin-reflect",
            ":kotlin-test:kotlin-test-jvm",
            ":kotlin-test:kotlin-test-junit",
            ":kotlin-script-runtime",
            ":kotlin-serialization"
        )

        private val IGNORED_LIBRARIES = listOf(
            // Libraries
            ":kotlin-stdlib-common",
            ":kotlin-reflect-api",
            ":kotlin-test:kotlin-test-common",
            ":kotlin-test:kotlin-test-annotations-common",
            // Other
            ":kotlin-compiler",
            ":kotlin-daemon-embeddable",
            ":kotlin-compiler-embeddable",
            ":kotlin-android-extensions",
            ":kotlin-scripting-compiler",
            ":kotlin-scripting-compiler-impl",
            ":kotlin-scripting-jvm-host"
        )

        private val MAPPED_LIBRARIES = mapOf(
            ":kotlin-reflect-api/java9" to ":kotlin-reflect/main"
        )

        private val LIB_DIRECTORIES = listOf("dependencies", "dist")
    }

    private lateinit var projectDir: File
    private lateinit var platformVersion: String
    private lateinit var platformBaseNumber: String
    private lateinit var intellijCoreDir: File
    private var isAndroidStudioPlatform: Boolean = false

    private fun initEnvironment(project: Project) {
        projectDir = project.projectDir
        platformVersion = project.extensions.extraProperties.get("versions.intellijSdk").toString()
        platformBaseNumber = platformVersion.substringBefore(".", "").takeIf { it.isNotEmpty() }
            ?: platformVersion.substringBefore("-", "").takeIf { it.isNotEmpty() }
                ?: error("Invalid platform version: $platformVersion")
        intellijCoreDir = File(platformDir.parentFile.parentFile.parentFile, "intellij-core")
        isAndroidStudioPlatform = project.extensions.extraProperties.has("versions.androidStudioRelease")
    }

    fun pill() {
        initEnvironment(rootProject)

        val variantOptionValue = System.getProperty("pill.variant", "base").toUpperCase()
        val variant = PillExtensionMirror.Variant.values().firstOrNull { it.name == variantOptionValue }
            ?: run {
                rootProject.logger.error("Invalid variant name: $variantOptionValue")
                return
            }

        rootProject.logger.lifecycle("Pill: Setting up project for the '${variant.name.toLowerCase()}' variant...")

        if (variant == PillExtensionMirror.Variant.NONE || variant == PillExtensionMirror.Variant.DEFAULT) {
            rootProject.logger.error("'none' and 'default' should not be passed as a Pill variant property value")
            return
        }

        val parserContext = ParserContext(variant)

        val dependencyPatcher = DependencyPatcher(rootProject)
        val dependencyMappers = listOf(dependencyPatcher, ::attachPlatformSources, ::attachAsmSources)

        val jpsProject = parse(rootProject, parserContext)
            .mapDependencies(dependencyMappers)
            .copy(libraries = dependencyPatcher.libraries)

        val files = render(jpsProject)

        removeExistingIdeaLibrariesAndModules()
        removeJpsAndPillRunConfigurations()
        removeAllArtifactConfigurations()

        val artifactDependencyMapper = object : ArtifactDependencyMapper {
            override fun map(dependency: PDependency): List<PDependency> {
                val result = mutableListOf<PDependency>()

                for (mappedDependency in jpsProject.mapDependency(dependency, dependencyMappers)) {
                    result += mappedDependency

                    if (mappedDependency is PDependency.Module) {
                        val module = jpsProject.modules.find { it.name == mappedDependency.name }
                        if (module != null) {
                            result += module.embeddedDependencies
                        }
                    }
                }

                return result
            }
        }

        generateKotlinPluginArtifactFile(rootProject, artifactDependencyMapper).write()

        copyRunConfigurations()
        setOptionsForDefaultJunitRunConfiguration(rootProject)

        files.forEach { it.write() }
    }

    fun unpill() {
        initEnvironment(rootProject)

        removeExistingIdeaLibrariesAndModules()
        removeJpsAndPillRunConfigurations()
        removeAllArtifactConfigurations()
    }

    private fun removeExistingIdeaLibrariesAndModules() {
        File(projectDir, ".idea/libraries").deleteRecursively()
        File(projectDir, ".idea/modules").deleteRecursively()
    }

    private fun removeJpsAndPillRunConfigurations() {
        File(projectDir, ".idea/runConfigurations")
            .walk()
            .filter { (it.name.startsWith("JPS_") || it.name.startsWith("Pill_")) && it.extension.toLowerCase() == "xml" }
            .forEach { it.delete() }
    }

    private fun removeAllArtifactConfigurations() {
        File(projectDir, ".idea/artifacts")
            .walk()
            .filter { it.extension.toLowerCase() == "xml" }
            .forEach { it.delete() }
    }

    private fun copyRunConfigurations() {
        val runConfigurationsDir = File(resourcesDir, "runConfigurations")
        val targetDir = File(projectDir, ".idea/runConfigurations")
        val platformDirProjectRelative = "\$PROJECT_DIR\$/" + platformDir.toRelativeString(projectDir)
        val additionalIdeaArgs = if (isAndroidStudioPlatform) "-Didea.platform.prefix=AndroidStudio" else ""

        targetDir.mkdirs()

        fun substitute(text: String): String {
            return text
                .replace("\$IDEA_HOME_PATH\$", platformDirProjectRelative)
                .replace("\$ADDITIONAL_IDEA_ARGS\$", additionalIdeaArgs)
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

                fun addOrReplaceOptionValue(name: String, value: Any?) {
                    val optionsWithoutNewValue = options.filter { !it.startsWith("-D$name=") }
                    options = if (value == null) optionsWithoutNewValue else (optionsWithoutNewValue + listOf("-D$name=$value"))
                }

                if (options.none { it == "-ea" }) {
                    options = options + "-ea"
                }

                addOrReplaceOptionValue("idea.home.path", platformDirProjectRelative)
                addOrReplaceOptionValue("ideaSdk.androidPlugin.path", "$platformDirProjectRelative/plugins/android/lib")
                addOrReplaceOptionValue("use.jps", "true")
                addOrReplaceOptionValue("kotlinVersion", project.rootProject.extra["kotlinVersion"].toString())

                val isAndroidStudioBunch = project.findProperty("versions.androidStudioRelease") != null
                addOrReplaceOptionValue("idea.platform.prefix", if (isAndroidStudioBunch) "AndroidStudio" else null)

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
            @Suppress("UsePropertyAccessSyntax")
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
                val project = rootProject.findProject(path) ?: error("Project not found")
                val archiveName = project.convention.findPlugin(BasePluginConvention::class.java)!!.archivesBaseName
                val classesJars = listOf(File(distLibDir, "$archiveName.jar")).filterExisting()
                val sourcesJars = listOf(File(distLibDir, "$archiveName-sources.jar")).filterExisting()
                result["$path/main"] = Optional.of(PLibrary(archiveName, classesJars, sourcesJars, originalName = path))
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
                    result += PDependency.Module(module.name)
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