/**
 * JVM specific API implementations using JAXP and so forth which would not be used when compiling to JS
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

// JavaScript style properties - TODO could auto-generate these
public val Node.nodeName: String
    get() = getNodeName() ?: ""

public val Node.nodeValue: String
    get() = getNodeValue() ?: ""

public val Node.nodeType: Short
    get() = getNodeType()

public val Node.parentNode: Node?
    get() = getParentNode()

public val Node.childNodes: NodeList
    get() = getChildNodes()!!

public val Node.firstChild: Node?
    get() = getFirstChild()

public val Node.lastChild: Node?
    get() = getLastChild()

public val Node.nextSibling: Node?
    get() = getNextSibling()

public val Node.previousSibling: Node?
    get() = getPreviousSibling()

public val Node.attributes: NamedNodeMap?
    get() = getAttributes()

public val Node.ownerDocument: Document?
    get() = getOwnerDocument()

public val Document.documentElement: Element?
    get() = this.getDocumentElement()

public val Node.namespaceURI: String
    get() = getNamespaceURI() ?: ""

public val Node.prefix: String
    get() = getPrefix() ?: ""

public val Node.localName: String
    get() = getLocalName() ?: ""

public val Node.baseURI: String
    get() = getBaseURI() ?: ""

public var Node.textContent: String
    get() = getTextContent() ?: ""
    set(value) {
        setTextContent(value)
    }

public val DOMStringList.length: Int
    get() = this.getLength()

public val NameList.length: Int
    get() = this.getLength()

public val DOMImplementationList.length: Int
    get() = this.getLength()

public val NodeList.length: Int
    get() = this.getLength()

public val CharacterData.length: Int
    get() = this.getLength()

public val NamedNodeMap.length: Int
    get() = this.getLength()


/**
 * Returns the HTML representation of the node
 */
public val Node.outerHTML: String
    get() = toXmlString()

/**
 * Returns the HTML representation of the node
 */
public val Node.innerHTML: String
    get() = childNodes.outerHTML

/**
 * Returns the HTML representation of the nodes
 */
public val NodeList.outerHTML: String
    get() = toList().map { it.innerHTML }.join("")

/** Returns an [[Iterator]] of all the next [[Element]] siblings */
public fun Node.nextElements(): List<Element> = nextSiblings().filterIsInstance<Node, Element>(javaClass<Element>())

/** Returns an [[Iterator]] of all the previous [[Element]] siblings */
public fun Node.previousElements(): List<Element> = previousSiblings().filterIsInstance<Node, Element>(javaClass<Element>())


public var Element.classSet: MutableSet<String>
    get() {
        val answer = LinkedHashSet<String>()
        val array = this.classes.split("""\s""")
        for (s in array) {
            if (s.size > 0) {
                answer.add(s)
            }
        }
        return answer
    }
    set(value) {
        this.classes = value.join(" ")
    }

/** Adds the given CSS class to this element's 'class' attribute */
public fun Element.addClass(cssClass: String): Boolean {
    val classSet = this.classSet
    val answer = classSet.add(cssClass)
    if (answer) {
        this.classSet = classSet
    }
    return answer
}

/** Removes the given CSS class to this element's 'class' attribute */
public fun Element.removeClass(cssClass: String): Boolean {
    val classSet = this.classSet
    val answer = classSet.remove(cssClass)
    if (answer) {
        this.classSet = classSet
    }
    return answer
}


/** Creates a new document with the given document builder*/
public fun createDocument(builder: DocumentBuilder): Document {
    return builder.newDocument()!!
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
public fun createTransformer(source: Source? = null, factory: TransformerFactory = TransformerFactory.newInstance()!!): Transformer {
    val transformer = if (source != null) {
        factory.newTransformer(source)
    } else {
        factory.newTransformer()
    }
    return transformer!!
}

/** Converts the node to an XML String */
public fun Node.toXmlString(): String = toXmlString(false)

/** Converts the node to an XML String */
public fun Node.toXmlString(xmlDeclaration: Boolean): String {
    val writer = StringWriter()
    writeXmlString(writer, xmlDeclaration)
    return writer.toString()
}

/** Converts the node to an XML String and writes it to the given [[Writer]] */
public fun Node.writeXmlString(writer: Writer, xmlDeclaration: Boolean): Unit {
    val transformer = createTransformer()
    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, if (xmlDeclaration) "no" else "yes")
    transformer.transform(DOMSource(this), StreamResult(writer))
}
