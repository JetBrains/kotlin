/**
 * JVM specific API implementations using JAXB and so forth which would not be used when compiling to JS
 */
package kotlin.dom

import java.io.File
import java.io.InputStream
import java.io.StringWriter
import java.io.Writer
import java.util.*
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.Source
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import org.w3c.dom.*
import org.xml.sax.InputSource

/** Converts the node list to an XML String */
fun NodeList?.toXmlString(xmlDeclaration: Boolean = false): String {
    return if (this == null)
        "" else {
        nodesToXmlString(this.toList(), xmlDeclaration)
    }
}

inline fun NodeList?.toList(): List<Node> {
    return if (this == null) {
        Collections.EMPTY_LIST as List<Node>
    }
    else {
        NodeListAsList(this)
    }
}

inline fun NodeList?.toElementList(): List<Element> {
    return if (this == null) {
        Collections.EMPTY_LIST as List<Element>
    }
    else {
        ElementListAsList(this)
    }
}

class NodeListAsList(val nodeList: NodeList): AbstractList<Node>() {
    override fun get(index: Int): Node {
        val node = nodeList.item(index)
        if (node == null) {
            throw IndexOutOfBoundsException("NodeList does not contain a node at index: " + index)
        } else {
            return node
        }
    }

    override fun size(): Int = nodeList.getLength()
}

class ElementListAsList(val nodeList: NodeList): AbstractList<Element>() {
    override fun get(index: Int): Element {
        val node = nodeList.item(index)
        if (node is Element) {
            return node
        } else {
            if (node == null) {
                throw IndexOutOfBoundsException("NodeList does not contain a node at index: " + index)
            } else {
                throw IllegalArgumentException("Node is not an Element as expected but is $node")
            }
        }
    }

    override fun size(): Int = nodeList.getLength()

}

/** Creates a new document with the given document builder*/
public fun createDocument(builder: DocumentBuilder): Document {
    return builder.newDocument().sure()
}

/** Creates a new document with an optional DocumentBuilderFactory */
public fun createDocument(builderFactory: DocumentBuilderFactory = defaultDocumentBuilderFactory()): Document {
    return createDocument(builderFactory.newDocumentBuilder()!!)
}

/**
 * Returns the default [[DocumentBuilderFactory]]
 */
public fun defaultDocumentBuilderFactory(): DocumentBuilderFactory {
    return DocumentBuilderFactory.newInstance()!!
}

/**
 * Returns the default [[DocumentBuilder]]
 */
public fun defaultDocumentBuilder(builderFactory: DocumentBuilderFactory = defaultDocumentBuilderFactory()): DocumentBuilder {
    return builderFactory.newDocumentBuilder()!!
}

/**
 * Parses the XML document using the given *file*
 */
public fun parseXml(file: File, builder: DocumentBuilder = defaultDocumentBuilder()): Document {
    return builder.parse(file)!!
}

/**
 * Parses the XML document using the given *uri*
 */
public fun parseXml(uri: String, builder: DocumentBuilder = defaultDocumentBuilder()): Document {
    return builder.parse(uri)!!
}

/**
 * Parses the XML document using the given *inputStream*
 */
public fun parseXml(inputStream: InputStream, builder: DocumentBuilder = defaultDocumentBuilder()): Document {
    return builder.parse(inputStream)!!
}

/**
 * Parses the XML document using the given *inputSource*
 */
public fun parseXml(inputSource: InputSource, builder: DocumentBuilder = defaultDocumentBuilder()): Document {
    return builder.parse(inputSource)!!
}


/** Creates a new TrAX transformer */
public fun createTransformer(source: Source? = null, factory: TransformerFactory = TransformerFactory.newInstance().sure()): Transformer {
    val transformer = if (source != null) {
        factory.newTransformer(source)
    } else {
        factory.newTransformer()
    }
    return transformer.sure()
}

/** Converts the node to an XML String */
public fun Node.toXmlString(xmlDeclaration: Boolean = false): String {
    val writer = StringWriter()
    writeXmlString(writer, xmlDeclaration)
    return writer.toString().sure()
}

/** Converts the node to an XML String and writes it to the given [[Writer]] */
public fun Node.writeXmlString(writer: Writer, xmlDeclaration: Boolean): Unit {
    val transformer = createTransformer()
    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, if (xmlDeclaration) "no" else "yes")
    transformer.transform(DOMSource(this), StreamResult(writer))
}

/** Converts the collection of nodes to an XML String */
public fun nodesToXmlString(nodes: java.lang.Iterable<Node>, xmlDeclaration: Boolean = false): String {
    // TODO this should work...
    // return this.map<Node,String>{it.toXmlString()}.makeString("")
    val builder = StringBuilder()
    for (n in nodes) {
        builder.append(n.toXmlString(xmlDeclaration))
    }
    return builder.toString().sure()
}