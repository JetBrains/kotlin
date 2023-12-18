import com.sun.org.apache.xml.internal.serializer.OutputPropertiesFactory
import org.gradle.api.Project
import org.w3c.dom.Element
import java.io.FileNotFoundException
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/**
 * Unused declarations annotated with annotations from this list will not be reported by the "Unused symbols" inspection.
 */
private val entryPointAnnotations = listOf(
    "org.jetbrains.kotlin.utils.IDEAPluginsCompatibilityAPI",
)

/** A workaround for [IDEA-84055](https://youtrack.jetbrains.com/issue/IDEA-84055).
 * This is needed because we don't store `.idea/misc.xml` in the VCS.
 *
 * Configures IDEA to use [entryPointAnnotations] as the list of entry points for the "Unused symbol" inspection.
 *
 * Suppose the `misc.xml` file already exists and has the following contents:
 * ```xml
 * <?xml version="1.0" encoding="UTF-8"?>
 * <project version="4">
 *   <component name="EntryPointsManager">
 *     // Some previous data
 *   </component>
 *   <component name="OtherComponent">
 *     // Some other data
 *   </component>
 * </project>
 * ```
 *
 * If [entryPointAnnotations] contains items `["com.example.Annotation1", "com.example.Annotation2", "com.example.Annotation3"]`,
 * the updated misc.xml content after a run of this function would look like this:
 * ```xml
 * <?xml version="1.0" encoding="UTF-8"?>
 * <project version="4">
 *   <component name="OtherComponent">
 *     // Some other data
 *   </component>
 *   <component name="EntryPointsManager">
 *     <list size="3">
 *       <item index="0" class="java.lang.String" itemvalue="com.example.Annotation1" />
 *       <item index="1" class="java.lang.String" itemvalue="com.example.Annotation2" />
 *       <item index="2" class="java.lang.String" itemvalue="com.example.Annotation3" />
 *     </list>
 *   </component>
 * </project>
 * ```
 */
fun Project.patchIdeaMiscXml() {
    val miscXml = rootDir.resolve(".idea/misc.xml")
    val documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
    val document = try {
        documentBuilder.parse(miscXml)
    } catch (e: FileNotFoundException) {
        documentBuilder.newDocument()
    }
    val project = (document.getElementsByTagName("project").item(0) as? Element)
        ?: document.createElement("project").apply { setAttribute("version", "4") }
    val components = project.getElementsByTagName("component")
    for (i in 0 until components.length) {
        val component = components.item(i) as? Element
        if (component?.getAttribute("name") == "EntryPointsManager") {
            project.removeChild(component)
        }
    }
    val entryPointsManager = document.createElement("component").also(project::appendChild)
    entryPointsManager.setAttribute("name", "EntryPointsManager")
    val componentList = document.createElement("list").also(entryPointsManager::appendChild)
    componentList.setAttribute("size", entryPointAnnotations.size.toString())
    for ((i, annotation) in entryPointAnnotations.withIndex()) {
        document.createElement("item").also(componentList::appendChild).run {
            setAttribute("index", i.toString())
            setAttribute("class", "java.lang.String")
            setAttribute("itemvalue", annotation)
        }
    }
    TransformerFactory.newInstance().newTransformer().run {
        setOutputProperty(OutputKeys.INDENT, "yes")
        setOutputProperty(OutputPropertiesFactory.S_KEY_INDENT_AMOUNT, "2")
        setOutputProperty(OutputKeys.ENCODING, "UTF-8")
        setOutputProperty(OutputKeys.STANDALONE, "no")
        transform(DOMSource(document), StreamResult(miscXml))
    }
}
