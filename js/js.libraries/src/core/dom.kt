package org.w3c.dom

import js.noImpl

/**
 * A stub class definition so we can work with the DOM APIs as native objects
 */
native public trait Document : Node {
    fun getElementById(id : String) : Element = js.noImpl
    fun createTextNode(text : String) : Node = js.noImpl
}

native public trait Attr : Node {}
native public trait CDATASection : Text {}
native public trait CharacterData : Node {}
native public trait Comment : CharacterData {}
native public trait DOMImplementation {}
native public trait DocumentType : Node {}
native public trait Element : Node {
    fun appendChild(child : Node) : Unit = js.noImpl
    fun getTextContent() : String = js.noImpl
}
native public trait Entity : Node {}
native public trait EntityReference : Node {}
native public trait NameList {}
native public trait NamedNodeMap {}
native public trait Node {}
native public trait NodeList {}
native public trait Notation : Node {}
native public trait ProcessingInstruction : Node {}
native public trait Text : CharacterData {}
native public trait TypeInfo {}
native public trait UserDataHandler {}

