package ideacompose

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File


class Patcher(
    val src: File,
    val result: File,
    val substitutionsFile: File,
    val backup: File? = null
) {
    private val resultIdea = result[".idea"]
    private val kotlinFolder = src
    private val intellijFolder = src[".."]
    private val substitutions = GsonBuilder().create().fromJson<Map<String, List<String>>>(
        substitutionsFile.readText(),
        object : TypeToken<Map<String, List<String>>>() {}.type
    )
    val kotlinLibsToInline = mutableMapOf<String, Lib>()

    fun patch() {
        println("src project: $src")
        println("writing updated files to: $result")
        println("reading substitutions from: $substitutionsFile")

        mergeModules()
        copyLibraries()
        replaceDeps()
    }

    private fun copyLibraries() {
        kotlinFolder[".idea", "libraries"].listFiles()?.toList()?.forEach { src ->
            val relative = src.relativeTo(kotlinFolder).path
            val target = result[relative]
            target.parentFile.mkdirs()

            val text = src.readText().replace("\$PROJECT_DIR\$/", "\$PROJECT_DIR\$/kotlin/")
            target.writeText(text)
            updated(relative)
        }
    }

    private fun mergeModules() {
        val kotlinModules = kotlinFolder[".idea/modules.xml"].loadXml()
        val intellijModules = intellijFolder[".idea/modules.xml"].loadXml()

        val kotlinModulesImported = intellijModules.importNode(kotlinModules.firstChild, true)

        kotlinModulesImported.elements
            .single { it.tagName == "component" && it.getAttribute("name") == "ProjectModuleManager" }
            .elements.single { it.tagName == "modules" }
            .elements.forEach {
                it.updateKotlinModulePath("fileurl")
                it.updateKotlinModulePath("filepath")
            }

        intellijModules.firstChild.mergeWith(kotlinModulesImported)
        intellijModules.saveTo(intellijFolder[".idea/modules.xml"])
        updated("modules.xml")
    }

    private fun Element.updateKotlinModulePath(attrName: String) {
        val src = getAttribute(attrName)
        val new = src.replace("\$PROJECT_DIR\$/", "\$PROJECT_DIR\$/kotlin/")
        setAttribute(attrName, new)
    }

    fun replaceDeps() {
        loadLibs()
        val modulesDir = kotlinFolder[".idea", "modules"]
        modulesDir
            .walkTopDown()
            .filter { it.extension == "iml" }
            .forEach {
                replaceDeps(it, it.relativeTo(modulesDir).path)
            }

        unknwnown.forEach {
            warn("unknown library: $it")
        }
    }

    private fun replaceDeps(iml: File, relative: String) {
        val dir = iml.parentFile
        val xml = iml.loadXml()

        var changed = false

        if (cleanClasspathKotlinCompilerArgument(xml)) {
            changed = true
        }

        val entries = xml.getElementsByTagName("orderEntry")
            .toList()
            .filterIsInstance<Element>()

        val libs = entries.filter { it.attributes.toMap()["type"] == "library" }

        libs.forEach {
            val lib = kotlinLibsToInline[it.getAttribute("name")]
            if (lib != null) {
                changed = true
                inlineLib(it, lib)
            }
        }

        val moduleLibs = entries.filter { it.attributes.toMap()["type"] == "module-library" }

        moduleLibs.forEach {
            if (processModuleLib(it.parentNode, it, dir)) {
                changed = true
            }
        }

        if (changed) {
            updated(relative)
            xml.saveTo(kotlinFolder[".idea", "modules", relative], prettyPrint = true)
        }
    }

    private fun inlineLib(it: Element, lib: Lib) {
        val container = it.parentNode
        container.removeChild(it)

        check(lib.unknwown.isEmpty())
        lib.replacements.forEach {
            val imported = container.ownerDocument.importNode(it.toXmlElement(), true)
            container.appendChild(imported)
        }
    }

    private fun cleanClasspathKotlinCompilerArgument(xml: Document): Boolean {
        var changed = false
        xml.getElementsByTagName("compilerArguments").toList().forEach {
            it.elements.forEach {
                if (it.tagName == "option" && it.getAttribute("name") == "classpath") {
                    val src = it.getAttribute("value")
                    val new = src.split(File.pathSeparator)
                        .filter {
                            !it.contains("dependencies/repo/kotlin.build/ideaIC") &&
                                    !it.contains("dependencies/repo/kotlin.build/intellij-core") &&
                                    !it.contains("dependencies/repo/kotlin.build/sources")
                        }
                        .joinToString(File.pathSeparator)
                    it.setAttribute("value", new)
                    changed = true
                }
            }
        }
        return changed
    }

    private fun update(relative: String): File {
        if (backup != null) {
            val target = backup[relative]
            target.parentFile.mkdirs()
            src[relative].copyTo(target)
        }

        println("UPDATED: $relative")
        return result[relative]
    }

    private fun updated(relative: String) {
        println("UPDATED: $relative")
    }

    private fun loadLibs() {
        src["..", ".idea", "libraries"].listFiles()!!.toList()
            .filter { it.extension == "xml" }
            .forEach {
                loadLib(it)
            }
    }

    private fun loadLib(it: File) {
        val libElement = it.loadXml()
            .elements.single { it.tagName == "component" && it.getAttribute("name") == "libraryTable" }
            .elements.single { it.tagName == "library" }

        val lib = Lib(libElement)
        if (lib.replacements.isNotEmpty()) {
            kotlinLibsToInline[libElement.getAttribute("name")] = lib
        }
    }

    inner class Lib(
        val lib: Element,
        val moduleDir: File? = null,
        val projectDir: File = kotlinFolder
    ) {
        val unknwown = mutableSetOf<String>()
        val replacements = mutableSetOf<String>()
        val knownRoots = mutableListOf<Element>()

        init {
            load()
        }

        fun load() {
            check(lib.tagName == "library") { "single <library> element expected, but <${lib.tagName}> found" }
            lib.elements.forEach {
                val kind = it.tagName
                if (kind in setOf("CLASSES", "JAVADOC", "SOURCES")) {
                    it.elements.forEach { root ->
                        check(root.tagName == "root") {
                            "$kind should contain only <root> elements"
                        }
                        val url = root.attributes.toMap().getValue("url")

                        val substitution = getSubstitution(url, moduleDir, projectDir)
                        if (substitution == null) {
                            unknwown.add(url)
                        } else {
                            knownRoots.add(root)
                            if (substitution is Items) {
                                replacements.addAll(substitution.items)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun processModuleLib(container: Node, entry: Node, dir: File): Boolean {
        val elements = entry.elements
        val libElement = elements.singleOrNull()
            ?: error("single <library> element expected, but ${elements.map { it.tagName }} found")

        val lib = Lib(libElement, dir)

        if (lib.replacements.isNotEmpty()) {
            if (lib.unknwown.isEmpty()) {
                container.removeChild(entry)
            } else {
                warn(
                    "library with substituted dependencies has unsubstituted dependencies: \n - " + lib.unknwown.joinToString(
                        "\n - "
                    )
                )
                lib.knownRoots.forEach {
                    it.parentNode.removeChild(it)
                }
            }

            lib.replacements.forEach {
                val imported = container.ownerDocument.importNode(it.toXmlElement(), true)
                container.appendChild(imported)
            }

            return true
        } else {
            return false
        }
    }

    private fun warn(message: String) {
        println("WARNING: $message")
    }

    private fun getSubstitution(url: String, moduleDir: File? = null, projectDir: File): Substitution? {
        var path = when {
            url.startsWith("jar://") -> {
                val jarUrl = url.removePrefix("jar://")
                if (!jarUrl.endsWith("!/")) {
                    warn("JAR protocol, selecting subfolder is unsupported: $jarUrl")
                    return null
                }
                jarUrl.removeSuffix("!/")
            }
            url.startsWith("file://") -> url.removePrefix("file://")
            else -> error("unsupported protocol $url")
        }

        if (moduleDir != null) {
            path = path.replace("\$MODULE_DIR$", moduleDir.absolutePath)
        }

        path = path.replace("\$PROJECT_DIR$", projectDir.absolutePath)

        val relativeFile = File(path).relativeToOrNull(kotlinFolder)
        if (relativeFile != null) {
            val relativePath = relativeFile.path
            val dependenciesPrefix = "dependencies/repo/kotlin.build/".replace("/", File.separator)
            if (relativePath.contains(dependenciesPrefix)) {
                val path1 = relativePath.drop(relativePath.indexOf(dependenciesPrefix) + dependenciesPrefix.length).split(File.separator)
                val module = path1[0]
                val version = path1[1]
                // 2: artifacts
                val relative = path1.drop(3).joinToString("/")
                val prefix = when (module) {
                    "ideaIC" -> ""
                    "intellij-core" -> "/core/lib"
                    "jps-standalone" -> "/lib"
                    "intellij-runtime-annotations" -> return null
                    "sources" -> return Sources()
                    else -> return null
                }
                val jarId = getJarId("$prefix/$relative")
                val substitution = substitutions[jarId]

                return if (substitution == null) {
                    unknwnown.add(jarId)
                    Unknown()
                } else Items(substitution)
            }
        }

        return null
    }

    val versionSuffixes = setOf("ea", "patched", "preview", "jre", "ga", "cj", "rc", "snapshot", "sp1", "alpha")
    val versionNumberRegexp = Regex("[\\d.c]+")

    private fun getJarId(jarName: String): String {
        val path = jarName.substringBeforeLast('/')
        val fileName = jarName.substringAfterLast('/')
        val ext = fileName.substringAfterLast(".")
        val baseFileName = fileName.substringBeforeLast(".")
        val components = baseFileName.split("-").dropLastWhile {
            it.toLowerCase() in versionSuffixes
                    || versionNumberRegexp.matches(cleanVersionNumber(it))
        }

        val result = "$path/${components.joinToString("-")}.$ext"

        return result
    }

    private fun cleanVersionNumber(it: String) = it
        .removeSuffix(".Final")
        .removeSuffix(".RELEASE")
        .removeSuffix("v")
        .removeSuffix("build")

    val unknwnown = mutableSetOf<String>()
}

sealed class Substitution
class Unknown : Substitution()
class Sources : Substitution()
class Items(val items: List<String>) : Substitution()
