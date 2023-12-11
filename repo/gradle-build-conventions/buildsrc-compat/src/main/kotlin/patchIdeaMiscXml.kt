import com.sun.org.apache.xml.internal.serializer.OutputPropertiesFactory
import org.gradle.api.Project
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.provideDelegate
import org.w3c.dom.Element
import java.io.FileNotFoundException
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

// A workaround for IDEA-84055
fun Project.patchIdeaMiscXml() {
    val entryPointAnnotations: List<String> by rootProject.extra
    val miscXml = rootProject.rootDir.resolve(".idea/misc.xml")
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
