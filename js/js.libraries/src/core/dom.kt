package org.w3c.dom

/**
 * A stub class definition so we can work with the DOM APIs as native objects
 */
native interface Document : Node {
}

native interface Attr : Node {}
native interface CDATASection : Text {}
native interface CharacterData : Node {}
native interface Comment : CharacterData {}
native interface DOMImplementation {}
native interface DocumentType : Node {}
native interface Element : Node {}
native interface Entity : Node {}
native interface EntityReference : Node {}
native interface NameList {}
native interface NamedNodeMap {}
native interface Node {}
native interface NodeList {}
native interface Notation : Node {}
native interface ProcessingInstruction : Node {}
native interface Text : CharacterData {}
native interface TypeInfo {}
native interface UserDataHandler {}

