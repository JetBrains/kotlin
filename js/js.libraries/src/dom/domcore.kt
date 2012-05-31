package js.dom.core

import js.native
import js.noImpl

public native trait DOMImplementationRegistry {
    public native fun getDOMImplementation(features: String): Unit = js.noImpl
    public native fun getDOMImplementationList(features: String): Unit = js.noImpl
}

public native trait DOMException {
    public native var code: Double = js.noImpl
}

public native trait DOMStringList {
    public native val length: Double = js.noImpl
    public native fun item(index: Number): String = js.noImpl
    public native fun contains(str: String): Boolean = js.noImpl
}

public native trait NameList {
    public native val length: Double = js.noImpl
    public native fun getName(index: Number): String = js.noImpl
    public native fun getNamespaceURI(index: Number): String = js.noImpl
    public native fun contains(str: String): Boolean = js.noImpl
    public native fun containsNS(namespaceURI: String, name: String): Boolean = js.noImpl
}

public native trait DOMImplementationList {
    public native val length: Double = js.noImpl
    public native fun item(index: Number): DOMImplementation? = js.noImpl
}

public native trait DOMImplementationSource {
    public native fun getDOMImplementation(features: String): DOMImplementation? = js.noImpl
    public native fun getDOMImplementationList(features: String): DOMImplementationList? = js.noImpl
}

public native trait DOMImplementation {
    public native fun hasFeature(feature: String, version: String): Boolean = js.noImpl
    public native fun createDocumentType(qualifiedName: String, publicId: String, systemId: String): DocumentType? = js.noImpl
    public native fun createDocument(namespaceURI: String, qualifiedName: String, doctype: DocumentType): Document? = js.noImpl
    public native fun getFeature(feature: String, version: String): Any? = js.noImpl
}

public native trait DocumentFragment : Node {

}

public native trait Document : Node {
    public native val doctype: DocumentType = js.noImpl
    public native val implementation: DOMImplementation = js.noImpl
    public native val documentElement: Element = js.noImpl
    public native val inputEncoding: String = js.noImpl
    public native val xmlEncoding: String = js.noImpl
    public native var xmlStandalone: Boolean = js.noImpl
    public native var xmlVersion: String = js.noImpl
    public native var strictErrorChecking: Boolean = js.noImpl
    public native var documentURI: String = js.noImpl
    public native val domConfig: DOMConfiguration = js.noImpl
    public native fun createElement(tagName: String): Element? = js.noImpl
    public native fun createDocumentFragment(): DocumentFragment? = js.noImpl
    public native fun createTextNode(data: String): Text? = js.noImpl
    public native fun createComment(data: String): Comment? = js.noImpl
    public native fun createCDATASection(data: String): CDATASection? = js.noImpl
    public native fun createProcessingInstruction(target: String, data: String): ProcessingInstruction? = js.noImpl
    public native fun createAttribute(name: String): Attr? = js.noImpl
    public native fun createEntityReference(name: String): EntityReference? = js.noImpl
    public native fun getElementsByTagName(tagname: String): NodeList = js.noImpl
    public native fun importNode(importedNode: Node, deep: Boolean): Node? = js.noImpl
    public native fun createElementNS(namespaceURI: String, qualifiedName: String): Element? = js.noImpl
    public native fun createAttributeNS(namespaceURI: String, qualifiedName: String): Attr? = js.noImpl
    public native fun getElementsByTagNameNS(namespaceURI: String, localName: String): NodeList = js.noImpl
    public native fun getElementById(elementId: String): Element? = js.noImpl
    public native fun adoptNode(source: Node): Node? = js.noImpl
    public native fun normalizeDocument(): Unit = js.noImpl
    public native fun renameNode(n: Node, namespaceURI: String, qualifiedName: String): Node? = js.noImpl
}

public native trait Node {
    public native val nodeName: String = js.noImpl
    public native var nodeValue: String = js.noImpl
    public native val nodeType: Double = js.noImpl
    public native val parentNode: Node = js.noImpl
    public native val childNodes: NodeList = js.noImpl
    public native val firstChild: Node = js.noImpl
    public native val lastChild: Node = js.noImpl
    public native val previousSibling: Node = js.noImpl
    public native val nextSibling: Node = js.noImpl
    public native val attributes: NamedNodeMap = js.noImpl
    public native val ownerDocument: Document = js.noImpl
    public native val namespaceURI: String = js.noImpl
    public native var prefix: String = js.noImpl
    public native val localName: String = js.noImpl
    public native val baseURI: String = js.noImpl
    public native var textContent: String = js.noImpl
    public native fun insertBefore(newChild: Node, refChild: Node): Node? = js.noImpl
    public native fun replaceChild(newChild: Node, refChild: Node): Node? = js.noImpl
    public native fun removeChild(oldChild: Node): Node? = js.noImpl
    public native fun appendChild(newChild: Node): Node? = js.noImpl
    public native fun hasChildNodes(): Boolean = js.noImpl
    public native fun cloneNode(deep: Node): Node? = js.noImpl
    public native fun normalize(): Unit = js.noImpl
    public native fun isSupported(feature: String, version: String): Boolean = js.noImpl
    public native fun hasAttributes(): Boolean = js.noImpl
    public native fun compareDocumentPosition(other: Node): Node? = js.noImpl
    public native fun isSameNode(other: Node): Boolean = js.noImpl
    public native fun lookupPrefix(namespaceURI: String): String = js.noImpl
    public native fun isDefaultNamespace(namespaceURI: String): String = js.noImpl
    public native fun lookupNamespaceURI(prefix: String): String = js.noImpl
    public native fun isEqualNode(arg: Node): Boolean = js.noImpl
    public native fun getFeature(feature: String, version: String): Any? = js.noImpl
    public native fun setUserData(key: String, data: Any, handler: UserDataHandler): Unit = js.noImpl
    public native fun getUserData(key: String): Unit = js.noImpl
}

public native trait NodeList {
    public native val length: Double = js.noImpl
    public native fun item(index: Number): Node? = js.noImpl
}

public native trait NamedNodeMap {
    public native val length: Double = js.noImpl
    public native fun getNamedItem(name: String): Node? = js.noImpl
    public native fun setNamedItem(arg: Node): Node? = js.noImpl
    public native fun removeNamedItem(name: String): Node? = js.noImpl
    public native fun item(index: Number): Node? = js.noImpl
    public native fun getNamedItemNS(namespaceURI: String, localName: String): Node? = js.noImpl
    public native fun setNamedItemNS(arg: Node): Node? = js.noImpl
    public native fun removeNamedItemNS(namespaceURI: String, localName: String): Node? = js.noImpl
}

public native trait CharacterData : Node {
    public native var data: String = js.noImpl
    public native val length: Double = js.noImpl
    public native fun substringData(offset: Number, count: Number): String = js.noImpl
    public native fun appendData(arg: String): Unit = js.noImpl
    public native fun insertData(offset: Number, arg: String): Unit = js.noImpl
    public native fun deleteData(offset: Number, count: Number): Unit = js.noImpl
    public native fun replaceData(offset: Number, count: Number, arg: String): Unit = js.noImpl
}

public native trait Attr : Node {
    public native val name: String = js.noImpl
    public native val specified: Boolean = js.noImpl
    public native val value: String = js.noImpl
    public native val ownerElement: Element = js.noImpl
    public native val schemaTypeInfo: TypeInfo = js.noImpl
    public native val isId: Boolean = js.noImpl
}

public native trait Element : Node {
    public native val tagName: String = js.noImpl
    public native val schemaTypeInfo: TypeInfo = js.noImpl
    public native fun getAttribute(name: String): String = js.noImpl
    public native fun setAttribute(name: String, value: String): Unit = js.noImpl
    public native fun removeAttribute(name: String): Unit = js.noImpl
    public native fun getAttributeNode(name: String): Attr? = js.noImpl
    public native fun setAttributeNode(newAttr: Attr): Attr? = js.noImpl
    public native fun removeAttributeNode(oldAttr: Attr): Attr? = js.noImpl
    public native fun getElementsByTagName(name: String): NodeList = js.noImpl
    public native fun getAttributeNS(namespaceURI: String, localName: String): String = js.noImpl
    public native fun setAttributeNS(namespaceURI: String, qualifiedName: String, value: String): Unit = js.noImpl
    public native fun removeAttributeNS(namespaceURI: String, localName: String): Unit = js.noImpl
    public native fun getAttributeNodeNS(namespaceURI: String, localName: String): Unit = js.noImpl
    public native fun setAttributeNodeNS(newAttr: Attr): Unit = js.noImpl
    public native fun getElementsByTagNameNS(namespaceURI: String, localName: String): NodeList = js.noImpl
    public native fun hasAttribute(name: String): Boolean = js.noImpl
    public native fun hasAttributeNS(namespaceURI: String, localName: String): Boolean = js.noImpl
    public native fun setIdAttribute(name: String, isId: Boolean): Unit = js.noImpl
    public native fun setIdAttributeNS(namespaceURI: String, localName: String, isId: Boolean): Unit = js.noImpl
    public native fun setIdAttributeNode(idAttr: Attr, isId: Boolean): Unit = js.noImpl
}

public native trait Text : CharacterData {
    public native val isElementContentWhitespace: Boolean = js.noImpl
    public native val wholeText: String = js.noImpl
    public native fun splitText(offset: Number): Text? = js.noImpl
    public native fun replaceWholeText(content: String): Text? = js.noImpl
}

public native trait Comment : CharacterData {

}

public native trait TypeInfo {
    public native val typeName: String = js.noImpl
    public native val typeNamespace: String = js.noImpl
    public native fun isDerivedFrom(typeNamespaceArg: String, typeNameArg: String, derivationMethod:  Number): Boolean = js.noImpl
}

public native trait UserDataHandler {
    public native fun handle(param1: Number, param2: String, param3: Any, param4: Node, param5: Node): Unit = js.noImpl
}

public native trait DOMError {
    public native var severity: Double = js.noImpl
    public native var message: String = js.noImpl
    public native var `type`: String = js.noImpl
    public native var relatedException: Any = js.noImpl
    public native var relatedData: Any = js.noImpl
    public native var location: DOMLocator = js.noImpl
}

public native trait DOMErrorHandler {
    public native fun handler(error: DOMError): Boolean = js.noImpl
}

public native trait DOMLocator {
    public native var lineNumber: Double = js.noImpl
    public native var columnNumber: Double = js.noImpl
    public native var byteOffset: Double = js.noImpl
    public native var utf16Offset: Double = js.noImpl
    public native var relatedNode: Node = js.noImpl
    public native var uri: String = js.noImpl
}

public native trait DOMConfiguration {
    public native var parameterNames: DOMStringList = js.noImpl
    public native fun setParameter(name: String, value: Any): Unit = js.noImpl
    public native fun getParameter(name: String): Unit = js.noImpl
    public native fun canSetParameter(name: String, value: Any): Boolean = js.noImpl
}

public native trait CDATASection : Text {

}

public native trait DocumentType : Node {
    public native val name: String = js.noImpl
    public native val entities: NamedNodeMap = js.noImpl
    public native val notations: NamedNodeMap = js.noImpl
    public native val publicId: String = js.noImpl
    public native val systemId: String = js.noImpl
    public native val internalSubset: String = js.noImpl
}

public native trait Notation : Node {
    public native val publicId: String = js.noImpl
    public native val systemId: String = js.noImpl
}

public native trait Entity : Node {
    public native val publicId: String = js.noImpl
    public native val systemId: String = js.noImpl
    public native val notationName: String = js.noImpl
    public native val inputEncoding: String = js.noImpl
    public native val xmlEncoding: String = js.noImpl
    public native val xmlVersion: String = js.noImpl
}

public native trait EntityReference : Node {

}

public native trait ProcessingInstruction : Node {
    public native val target: String = js.noImpl
    public native var data: String = js.noImpl
}

