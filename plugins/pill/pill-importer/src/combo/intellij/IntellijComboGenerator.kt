/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.pill.combo.intellij

import org.jetbrains.kotlin.pill.*
import org.jetbrains.kotlin.pill.combo.ComboGenerator
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File

private const val MODULES_XML_PATH = ".idea/modules.xml"
private const val MODULES_DIR_PATH = ".idea/modules"
private const val LIBRARIES_DIR_PATH = ".idea/libraries"

private const val KOTLIN_IDE_IML_NAME = "kotlinide.iml"
private const val COMBO_IML_NAME = "combo.iml"

class IntellijComboGenerator(private val kotlinProjectDir: File) : ComboGenerator {
    private val comboProjectDir = kotlinProjectDir.parentFile
    private val ideaProjectDir = File(kotlinProjectDir.parentFile, "intellij")

    private val kotlinDependenciesDir = File(System.getProperty("user.home"), ".gradle/kotlin-build-dependencies/repo/kotlin.build")

    private val substitutions: Substitutions

    init {
        require(kotlinProjectDir.name == "kotlin") { "Kotlin project must be placed in " + File(kotlinProjectDir.parentFile, "kotlin") }
        require(ideaProjectDir.exists()) { "Intellij project not found in $ideaProjectDir" }

        substitutions = SubstitutionFileReader.read(kotlinProjectDir, ideaProjectDir)
    }

    override fun generate() {
        copyIdeaSettings()
        patchIdeaVcsSettings()
        patchIdeaMiscSettings()
        patchIdeaExternalDependenciesSettings()
        addKotlinLibraries()

        File(comboProjectDir, MODULES_DIR_PATH).deleteRecursively()
        mergeModules()

        generateModule(File(comboProjectDir, "$MODULES_DIR_PATH/$KOTLIN_IDE_IML_NAME"), "\$MODULE_DIR\$/../../" + kotlinProjectDir.name)
        generateModule(File(comboProjectDir, COMBO_IML_NAME), "\$MODULE_DIR\$")

        copyKotlinModules()
    }

    private fun copyIdeaSettings() {
        val excludes = listOf("modules", "modules.xml", "workspace.xml")

        for (file in File(ideaProjectDir, ".idea").listFiles().orEmpty()) {
            if (file.name in excludes) {
                continue
            }

            val target = File(comboProjectDir, ".idea/" + file.name)
            target.deleteRecursively()
            copyTree(file, target) { patchPaths(it, ideaProjectDir, null) }
        }
    }

    private fun addKotlinLibraries() {
        fun addMavenLibrary(fileName: String, name: String, groupId: String, artifactId: String, version: String, vararg excludes: String) {
            val coordinates = "$groupId:$artifactId:$version"

            val file = File(comboProjectDir, "$LIBRARIES_DIR_PATH/$fileName")
            val contents = xml("component", "name" to "libraryTable") {
                xml("library", "name" to name, "type" to "repository") {
                    xml("properties", "maven-id" to coordinates) {
                        if (excludes.isNotEmpty()) {
                            xml("exclude") {
                                for (excludeEntry in excludes) {
                                    xml("dependency", "maven-id" to excludeEntry)
                                }
                            }
                        }
                    }
                    xml("CLASSES") {
                        val groupPath = groupId.replace('.', '/')
                        val path = "$groupPath/$artifactId/$version/$artifactId-$version.jar"
                        xml("root", "url" to "jar://\$MAVEN_REPOSITORY\$/$path!/")
                    }
                    xml("JAVADOC")
                    xml("SOURCES")
                }
            }
            file.writeText(contents.toString())
        }

        fun copyKotlinLibrary(name: String) {
            val src = File(kotlinProjectDir, "$LIBRARIES_DIR_PATH/$name.xml")
            val target = File(comboProjectDir, "$LIBRARIES_DIR_PATH/$name.xml")
            copyTree(src, target) { patchPaths(it, kotlinProjectDir, null) }
        }

        addMavenLibrary(
            "kotlin_test_junit.xml", "kotlin-test-junit",
            "org.jetbrains.kotlin", "kotlin-test-junit", "1.3.70",
            "org.jetbrains.kotlin:kotlin-stdlib",
            // Excluded
            "org.hamcrest:hamcrest-core",
            "org.jetbrains.kotlin:kotlin-stdlib-common",
            "org.jetbrains.kotlin:kotlin-test-common",
            "junit:junit",
            "org.jetbrains:annotations",
            "org.jetbrains.kotlin:kotlin-test-annotations-common",
            "org.jetbrains.kotlin:kotlin-test",
        )

        addMavenLibrary(
            "kotlin_annotations_jvm.xml", "kotlin-annotations-jvm",
            "org.jetbrains.kotlin", "kotlin-annotations-jvm", "1.3.70"
        )

        copyKotlinLibrary("kotlin_coroutines_experimental_compat")
    }

    private fun patchIdeaVcsSettings() {
        val vcsFile = File(comboProjectDir, ".idea/vcs.xml")
        val vcs = vcsFile.loadXml()

        val vcsDirectoryMappingsComponent = findComponent(vcs, "VcsDirectoryMappings")

        val newVcsDirectoryMappings = vcs.createElement("component").apply {
            setAttribute("name", "VcsDirectoryMappings")

            fun addMapping(path: String) {
                val mappingElement = vcs.createElement("mapping").apply {
                    setAttribute("directory", path)
                    setAttribute("vcs", "Git")
                }

                appendChild(mappingElement)
            }

            addMapping("\$PROJECT_DIR\$/" + ideaProjectDir.name)
            addMapping("\$PROJECT_DIR\$/" + kotlinProjectDir.name)
        }

        vcsDirectoryMappingsComponent.parentNode.replaceChild(newVcsDirectoryMappings, vcsDirectoryMappingsComponent)
        vcs.saveXml(vcsFile)
    }

    private fun patchIdeaMiscSettings() {
        val miscFile = File(comboProjectDir, ".idea/misc.xml")
        val misc = miscFile.loadXml()

        val projectRootManagerComponent = findComponent(misc, "ProjectRootManager")

        projectRootManagerComponent.getElementsByTagName("output").elements.forEach { it.parentNode.removeChild(it) }

        val newOutput = misc.createElement("output").apply {
            setAttribute("url", "file://\$PROJECT_DIR\$/out/classes")
        }

        projectRootManagerComponent.appendChild(newOutput)

        misc.saveXml(miscFile)
    }

    private fun patchIdeaExternalDependenciesSettings() {
        val externalDependenciesFile = File(comboProjectDir, ".idea/externalDependencies.xml")
        val externalDependencies = externalDependenciesFile.loadXml()

        val externalDependenciesComponent = findComponent(externalDependencies, "ExternalDependencies")

        for (element in externalDependenciesComponent.getElementsByTagName("plugin").elements) {
            if (element.getAttribute("id") == "org.jetbrains.kotlin") {
                element.setAttribute("min-version", "1.4.0")
                element.setAttribute("max-version", "1.4.99")
            }
        }

        externalDependencies.saveXml(externalDependenciesFile)
    }

    private fun copyTree(src: File, target: File, processor: (String) -> String) {
        for (srcFile in src.walkTopDown()) {
            val relativePath = srcFile.toRelativeString(src)
            val targetFile = File(target, relativePath)

            if (srcFile.isFile) {
                targetFile.parentFile.mkdirs()

                val contents = processor(srcFile.readText())
                targetFile.writeText(contents)
            }
        }
    }

    private fun generateModule(file: File, path: String) {
        file.parentFile.mkdirs()

        file.writeText(
            """
            <module type="JAVA_MODULE" version="4">
              <component name="NewModuleRootManager" inherit-compiler-output="true">
                <exclude-output />
                <content url="file://$path"/>
                <orderEntry type="inheritedJdk" />
              </component>
            </module>
            """.trimIndent()
        )
    }

    private fun mergeModules() {
        val kotlinModulesFile = File(kotlinProjectDir, MODULES_XML_PATH)
        val kotlinModules = kotlinModulesFile.loadXml()

        val ideaModulesFile = File(ideaProjectDir, MODULES_XML_PATH)
        val ideaModules = ideaModulesFile.loadXml()

        val ideaPathContext = ProjectContext(ideaProjectDir)
        val comboPathContext = ProjectContext(comboProjectDir)

        fun createModuleElement(url: String, path: String): Element {
            return ideaModules.createElement("module").apply {
                setAttribute("fileurl", url)
                setAttribute("filepath", path)
            }
        }

        val ideaProjectModuleManager = findComponent(ideaModules, "ProjectModuleManager")
        for (module in ideaProjectModuleManager.getElementsByTagName("module").elements) {
            val filePath = module.getAttribute("filepath") ?: continue
            val file = File(ideaPathContext.substituteWithValues(filePath)).canonicalFile
            val newModule = createModuleElement(comboPathContext.substituteWithVariables(file), comboPathContext.getUrlWithVariables(file))
            module.parentNode.replaceChild(newModule, module)
        }

        val ideaProjectModuleManagerModules = ideaProjectModuleManager.getElementsByTagName("modules").asList().single()

        val kotlinProjectModuleManager = findComponent(kotlinModules, "ProjectModuleManager")
        for (module in kotlinProjectModuleManager.getElementsByTagName("module").elements) {
            val fileUrl = module.getAttribute("fileurl") ?: continue
            val filePath = module.getAttribute("filepath") ?: continue

            if (filePath == "\$PROJECT_DIR\$/kotlin.iml") {
                continue
            }

            val moduleElement = createModuleElement(fileUrl, filePath)
            ideaProjectModuleManagerModules.appendChild(moduleElement)
        }

        fun addBulkModule(path: String) {
            val moduleElement = createModuleElement("file://$path", path)
            ideaProjectModuleManagerModules.appendChild(moduleElement)
        }

        addBulkModule("\$PROJECT_DIR\$/$MODULES_DIR_PATH/$KOTLIN_IDE_IML_NAME")
        addBulkModule("\$PROJECT_DIR\$/$COMBO_IML_NAME")

        val targetFile = File(comboProjectDir, MODULES_XML_PATH)
        ideaModules.saveXml(targetFile)
    }

    private fun copyKotlinModules() {
        val kotlinModulesDir = File(kotlinProjectDir, MODULES_DIR_PATH)

        kotlinModulesDir
            .walkTopDown()
            .filter { it.extension == "iml" }
            .forEach { moduleFile -> patchModule(moduleFile) }
    }

    private fun patchModule(moduleFile: File) {
        val relativePath = moduleFile.toRelativeString(kotlinProjectDir)
        val comboModuleFile = File(comboProjectDir, relativePath)

        val module = moduleFile.loadXml()
        val moduleRootManager = findComponent(module, "NewModuleRootManager")

        val kotlinPathContext = ModuleContext(kotlinProjectDir, moduleFile)
        val comboPathContext = ModuleContext(comboProjectDir, comboModuleFile)

        // Patch source paths
        for (contentRoot in moduleRootManager.getElementsByTagName("content").elements) {
            fun patchUrlEntity(entity: Element) {
                val url = entity.getAttribute("url") ?: return
                val path = kotlinPathContext.substituteWithValues(getUrlPath(url))
                entity.setAttribute("url", comboPathContext.getUrlWithVariables(File(path).canonicalFile))
            }

            patchUrlEntity(contentRoot)
            contentRoot.getElementsByTagName("sourceFolder").elements.forEach { patchUrlEntity(it) }
        }

        val oldOrderEntryElements = moduleRootManager.getElementsByTagName("orderEntry").elements
        val oldOrderEntries = oldOrderEntryElements.map { OrderEntryInfo.parse(it, kotlinPathContext) }

        val newOrderEntries = mutableListOf<ScopedOrderEntryInfo>()

        for (scopedEntry in oldOrderEntries) {
            when (val entry = scopedEntry.entry) {
                is OrderEntryInfo.Library -> {
                    val libraryName = mapKotlinProjectLibrary(entry.name) ?: entry.name
                    newOrderEntries += scopedEntry.copy(entry = OrderEntryInfo.Library(libraryName))
                }
                is OrderEntryInfo.ModuleLibrary -> {
                    val javadoc = entry.library.javadoc.filter { !it.startsWith(kotlinDependenciesDir) }
                    val sources = entry.library.sources.filter { !it.startsWith(kotlinDependenciesDir) }

                    for (classpathFile in entry.library.classes) {
                        if (classpathFile.startsWith(kotlinProjectDir)) {
                            val mappedLibrary = LibraryInfo(listOf(classpathFile), javadoc, sources)
                            newOrderEntries += scopedEntry.copy(entry = OrderEntryInfo.ModuleLibrary(mappedLibrary))
                        } else if (classpathFile.startsWith(kotlinDependenciesDir)) {
                            val relativeClasspathFile = classpathFile.relativeToOrNull(kotlinDependenciesDir) ?: continue
                            val relativeClasspathPath = relativeClasspathFile.path
                            val artifactName = relativeClasspathPath.substringBefore(File.separatorChar, "")

                            val substitutionsForArtifact = this.substitutions.getForArtifact(artifactName)
                            if (substitutionsForArtifact == null) {
                                println("[ERR] Substitutions not found for artifact $classpathFile")
                                continue
                            }

                            val pathInsideIdea = getJarPathInsideIdeaPlatform(relativeClasspathPath) ?: continue

                            val substitutionsForFile = substitutionsForArtifact[pathInsideIdea]
                            if (substitutionsForFile == null) {
                                println("[ERR] Substitutions not found for path $pathInsideIdea")
                                continue
                            }

                            for (substitution in substitutionsForFile) {
                                when (substitution) {
                                    is OrderEntryInfo.Library, is OrderEntryInfo.ModuleOutput -> {
                                        newOrderEntries += scopedEntry.copy(entry = substitution)
                                    }
                                    is OrderEntryInfo.ModuleLibrary -> {
                                        for (substitutionClasspathFile in substitution.library.classes) {
                                            val libraryForFile = substitution.library.copy(classes = listOf(substitutionClasspathFile))
                                            newOrderEntries += scopedEntry.copy(entry = OrderEntryInfo.ModuleLibrary(libraryForFile))
                                        }
                                    }
                                    else -> error("Unexpected substitution entry $substitution")
                                }
                            }
                        } else {
                            newOrderEntries += scopedEntry
                        }
                    }
                }
                else -> newOrderEntries += scopedEntry
            }
        }

        oldOrderEntryElements.forEach { it.parentNode.removeChild(it) }

        for (scopedEntry in newOrderEntries.distinct()) {
            val element = scopedEntry.render(module, comboPathContext)
            moduleRootManager.appendChild(element)
        }

        module.saveXml(comboModuleFile)
    }

    private fun mapKotlinProjectLibrary(name: String): String? {
        return when (name) {
            "kotlin-stdlib", "kotlin-stdlib-jdk7" -> "kotlin-stdlib-jdk8"
            else -> null
        }
    }

    private fun getJarPathInsideIdeaPlatform(relativePath: String): String? {
        val pathComponents = relativePath.split(File.separatorChar)
        val countToDrop = if (pathComponents.firstOrNull() == "intellij-runtime-annotations") 2 else 3
        return pathComponents.drop(countToDrop).joinToString("/")
    }

    private fun findComponent(configurationFile: Document, componentName: String): Element {
        return configurationFile
            .childElements.first() // <project>
            .childElements.first { it.tagName == "component" && it.getAttribute("name") == componentName }
    }
}