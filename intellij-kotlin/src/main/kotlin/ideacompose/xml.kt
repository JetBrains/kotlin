package ideacompose

import org.w3c.dom.*
import java.io.ByteArrayInputStream
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

fun File.loadXml(): Document {
    val factory = DocumentBuilderFactory.newInstance()
    val builder = factory.newDocumentBuilder()
    val doc = builder.parse(this)
    return doc
}

fun Document.saveTo(file: File, prettyPrint: Boolean = false) {
    file.parentFile.mkdirs()
    file.writer().use {
        val tr = TransformerFactory.newInstance().newTransformer()
        if (prettyPrint) {
            tr.setOutputProperty(OutputKeys.INDENT, "yes");
            tr.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
//            tr.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
        }
        tr.transform(DOMSource(this), StreamResult(it))
    }
}

fun String.toXmlElement(): Element =
    DocumentBuilderFactory
        .newInstance()
        .newDocumentBuilder()
        .parse(ByteArrayInputStream(toByteArray()))
        .documentElement

val Node.elements: Collection<Element>
    get() = childNodes.toList().filterIsInstance<Element>()

fun NodeList?.toList(): Collection<Node> {
    if (this == null) return listOf()

    return ArrayList<Node>(length).also {
        for (i in 0 until length) {
            it.add(item(i))
        }
    }
}

fun NamedNodeMap?.toMap(): Map<String, String> {
    if (this == null) return mapOf()

    return mutableMapOf<String, String>().also {
        for (i in 0 until length) {
            val item = item(i)
            it[item.nodeName] = item.nodeValue
        }
    }
}

val Node.data: NodeData?
    get() = if (nodeType == Node.ELEMENT_NODE) NodeData(nodeName, attributes.toMap()) else null

data class NodeData(val name: String, val values: Map<String, String>)

fun Node.mergeWith(src: Node) {
    val existed = childNodes.toList()
        .filter { it.nodeType == Node.ELEMENT_NODE }
        .associateBy { it.data }

    src.childNodes.toList().forEach {
        val data = it.data
        val e = existed[data]
        if (e != null) {
            e.mergeWith(it)
        } else {
            appendChild(it)
        }
    }
}