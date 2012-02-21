/**
 * JVM specific API implementations using JAXB and so forth which would not be used when compiling to JS
 */
package std.dom

import org.w3c.dom.*
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.Source
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

import java.io.StringWriter
import javax.xml.transform.OutputKeys

fun createDocument(builder: DocumentBuilder): Document {
    return builder.newDocument().sure()
}

fun createDocument(builderFactory: DocumentBuilderFactory = DocumentBuilderFactory.newInstance().sure()): Document {
    return createDocument(builderFactory.newDocumentBuilder().sure())
}

fun createTransformer(source: Source? = null, factory: TransformerFactory = TransformerFactory.newInstance().sure()): Transformer {
    val transformer = if (source != null) {
        factory.newTransformer(source)
    } else {
        factory.newTransformer()
    }
    return transformer.sure()
}

fun Node.toXmlString(xmlDeclaration: Boolean = this is Document): String {
    return nodeToXmlString(this, xmlDeclaration)
}

/*
fun Document.toXmlString(xmlDeclaration: Boolean = true): String {
    return nodeToXmlString(this, xmlDeclaration)
}
*/

fun nodeToXmlString(node: Node, xmlDeclaration: Boolean): String {
    val transformer = createTransformer()
    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, if (xmlDeclaration) "no" else "yes")
    val buffer = StringWriter()
    transformer.transform(DOMSource(node), StreamResult(buffer))
    return buffer.toString().sure()

}

