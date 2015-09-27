
/**
 * JVM specific API implementations using JAXP and so forth which would not be used when compiling to JS
 */
@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("DomKt")
package kotlin.dom

import org.w3c.dom.*
import org.xml.sax.InputSource
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

// JavaScript style properties - TODO could auto-generate these
@Deprecated("Use getNodeName()", ReplaceWith("getNodeName() ?: \"\""))
public val Node.nodeName: String
    get() = getNodeName() ?: ""

@Deprecated("Use getNodeValue()", ReplaceWith("getNodeValue() ?: \"\""))
public val Node.nodeValue: String
    get() = getNodeValue() ?: ""

@HiddenDeclaration
@Deprecated("Is replaced with automatic synthetic extension", ReplaceWith("nodeType"))
public val Node.nodeType: Short
    get() = getNodeType()

@HiddenDeclaration
@Deprecated("Is replaced with automatic synthetic extension", ReplaceWith("parentNode"))
public val Node.parentNode: Node?
    get() = getParentNode()

@Deprecated("Use getChildNodes()", ReplaceWith("getChildNodes()!!"))
public val Node.childNodes: NodeList
    get() = getChildNodes()!!

@HiddenDeclaration
@Deprecated("Is replaced with automatic synthetic extension", ReplaceWith("firstChild"))
public val Node.firstChild: Node?
    get() = getFirstChild()

@HiddenDeclaration
@Deprecated("Is replaced with automatic synthetic extension", ReplaceWith("lastChild"))
public val Node.lastChild: Node?
    get() = getLastChild()

@HiddenDeclaration
@Deprecated("Is replaced with automatic synthetic extension", ReplaceWith("nextSibling"))
public val Node.nextSibling: Node?
    get() = getNextSibling()

@HiddenDeclaration
@Deprecated("Is replaced with automatic synthetic extension", ReplaceWith("previousSibling"))
public val Node.previousSibling: Node?
    get() = getPreviousSibling()

@HiddenDeclaration
@Deprecated("Is replaced with automatic synthetic extension", ReplaceWith("attributes"))
public val Node.attributes: NamedNodeMap?
    get() = getAttributes()

@HiddenDeclaration
@Deprecated("Is replaced with automatic synthetic extension", ReplaceWith("ownerDocument"))
public val Node.ownerDocument: Document?
    get() = getOwnerDocument()

@HiddenDeclaration
@Deprecated("Is replaced with automatic synthetic extension", ReplaceWith("documentElement"))
public val Document.documentElement: Element?
    get() = this.getDocumentElement()

@Deprecated("Use getNamespaceURI()", ReplaceWith("getNamespaceURI() ?: \"\""))
public val Node.namespaceURI: String
    get() = getNamespaceURI() ?: ""

@Deprecated("Use getPrefix()", ReplaceWith("getPrefix() ?: \"\""))
public val Node.prefix: String
    get() = getPrefix() ?: ""

@Deprecated("Use getLocalName()", ReplaceWith("getLocalName() ?: \"\""))
public val Node.localName: String
    get() = getLocalName() ?: ""

@Deprecated("Use getBaseURI", ReplaceWith("getBaseURI() ?: \"\""))
public val Node.baseURI: String
    get() = getBaseURI() ?: ""

@Deprecated("Use getTextContent()/setTextContent()")
public var Node.textContent: String
    get() = getTextContent() ?: ""
    set(value) {
        setTextContent(value)
    }

@HiddenDeclaration
@Deprecated("Is replaced with automatic synthetic extension", ReplaceWith("length"))
public val DOMStringList.length: Int
    get() = this.getLength()

@HiddenDeclaration
@Deprecated("Is replaced with automatic synthetic extension", ReplaceWith("length"))
public val NameList.length: Int
    get() = this.getLength()

@HiddenDeclaration
@Deprecated("Is replaced with automatic synthetic extension", ReplaceWith("length"))
public val DOMImplementationList.length: Int
    get() = this.getLength()

@HiddenDeclaration
@Deprecated("Is replaced with automatic synthetic extension", ReplaceWith("length"))
public val NodeList.length: Int
    get() = this.getLength()

@HiddenDeclaration
@Deprecated("Is replaced with automatic synthetic extension", ReplaceWith("length"))
public val CharacterData.length: Int
    get() = this.getLength()

@HiddenDeclaration
@Deprecated("Is replaced with automatic synthetic extension", ReplaceWith("length"))
public val NamedNodeMap.length: Int
    get() = this.getLength()

public var Element.id: String
    get() = this.getAttribute("id") ?: ""
    set(value) {
        this.setAttribute("id", value)
        this.setIdAttribute("id", true)
    }

public var Element.style: String
    get() = this.getAttribute("style") ?: ""
    set(value) {
        this.setAttribute("style", value)
    }

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

/** Returns an [Iterator] of all the next [Element] siblings */
public fun Node.nextElements(): List<Element> = nextSiblings().filterIsInstance(javaClass<Element>())

/** Returns an [Iterator] of all the previous [Element] siblings */
public fun Node.previousElements(): List<Element> = previousSiblings().filterIsInstance(javaClass<Element>())


public var Element.classSet: MutableSet<String>
    get() {
        val answer = LinkedHashSet<String>()
        val array = this.classes.split("""\s""".toPattern())
        for (s in array) {
            if (s.length() > 0) {
                answer.add(s)
            }
        }
        return answer
    }
    set(value) {
        this.classes = value.join(" ")
    }


/** Creates a new document with the given document builder*/
public fun createDocument(builder: DocumentBuilder): Document {
    return builder.newDocument()
}

/** Creates a new document with an optional DocumentBuilderFactory */
public fun createDocument(builderFactory: DocumentBuilderFactory = defaultDocumentBuilderFactory()): Document {
    return createDocument(builderFactory.newDocumentBuilder())
}

/**
 * Returns the default [DocumentBuilderFactory]
 */
public fun defaultDocumentBuilderFactory(): DocumentBuilderFactory {
    return DocumentBuilderFactory.newInstance()!!
}

/**
 * Returns the default [DocumentBuilder]
 */
public fun defaultDocumentBuilder(builderFactory: DocumentBuilderFactory = defaultDocumentBuilderFactory()): DocumentBuilder {
    return builderFactory.newDocumentBuilder()
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

/** Converts the node to an XML String and writes it to the given [Writer] */
public fun Node.writeXmlString(writer: Writer, xmlDeclaration: Boolean): Unit {
    val transformer = createTransformer()
    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, if (xmlDeclaration) "no" else "yes")
    transformer.transform(DOMSource(this), StreamResult(writer))
}
