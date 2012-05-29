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

/** Returns an [[Iterator]] of all the next [[Element]] siblings */
fun Node.nextElements(): Iterator<Element> = nextSiblings().filterIsInstance<Node, Element>(javaClass<Element>())

/** Returns an [[Iterator]] of all the previous [[Element]] siblings */
fun Node.previousElements(): Iterator<Element> = previousSiblings().filterIsInstance<Node, Element>(javaClass<Element>())


/** Searches for elements using the element name, an element ID (if prefixed with dot) or element class (if prefixed with #) */
fun Document?.get(selector: String): List<Element> {
    val root = this?.getDocumentElement()
    return if (root != null) {
        if (selector == "*") {
            elements
        } else if (selector.startsWith(".")) {
            elements.filter{ it.hasClass(selector.substring(1)) }.toList()
        } else if (selector.startsWith("#")) {
            val id = selector.substring(1)
            val element = this?.getElementById(id)
            return if (element != null)
                Collections.singletonList(element).sure() as List<Element>
            else
                Collections.EMPTY_LIST.sure() as List<Element>
        } else {
            //  assume its a vanilla element name
            elements(selector)
        }
    } else {
        Collections.EMPTY_LIST as List<Element>
    }
}

/** Searches for elements using the element name, an element ID (if prefixed with dot) or element class (if prefixed with #) */
fun Element.get(selector: String): List<Element> {
    return if (selector == "*") {
        elements
    } else if (selector.startsWith(".")) {
        elements.filter{ it.hasClass(selector.substring(1)) }.toList()
    } else if (selector.startsWith("#")) {
        val element = this.getOwnerDocument()?.getElementById(selector.substring(1))
        return if (element != null)
            Collections.singletonList(element).sure() as List<Element>
        else
            Collections.EMPTY_LIST.sure() as List<Element>
    } else {
        //  assume its a vanilla element name
        elements(selector)
    }
}

var Element.classSet : Set<String>
get() {
    val answer = LinkedHashSet<String>()
    val array = this.classes.split("""\s""")
    for (s in array) {
        if (s != null && s.size > 0) {
            answer.add(s)
        }
    }
    return answer
}
set(value) {
    this.classes = value.makeString(" ")
}

/** Returns true if the element has the given CSS class style in its 'class' attribute */
fun Element.hasClass(cssClass: String): Boolean {
    val c = this.classes
    return if (c != null)
        c.matches("""(^|.*\s+)$cssClass($|\s+.*)""")
    else false
}

/** Adds the given CSS class to this element's 'class' attribute */
fun Element.addClass(cssClass: String): Boolean {
    val classSet = this.classSet
    val answer = classSet.add(cssClass)
    if (answer) {
        this.classSet = classSet
    }
    return answer
}

/** Removes the given CSS class to this element's 'class' attribute */
fun Element.removeClass(cssClass: String): Boolean {
    val classSet = this.classSet
    val answer = classSet.remove(cssClass)
    if (answer) {
        this.classSet = classSet
    }
    return answer
}


/** Converts the node list to an XML String */
fun NodeList?.toXmlString(xmlDeclaration: Boolean = false): String {
    return if (this == null)
        "" else {
        nodesToXmlString(this.toList(), xmlDeclaration)
    }
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