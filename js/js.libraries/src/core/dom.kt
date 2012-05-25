package org.w3c.dom

/**
 * A stub class definition so we can work with the DOM APIs as native objects
 */
native public interface Document : Node {
}

native public interface Attr : Node {}
native public interface CDATASection : Text {}
native public interface CharacterData : Node {}
native public interface Comment : CharacterData {}
native public interface DOMImplementation {}
native public interface DocumentType : Node {}
native public interface Element : Node {}
native public interface Entity : Node {}
native public interface EntityReference : Node {}
native public interface NameList {}
native public interface NamedNodeMap {}
native public interface Node {}
native public interface NodeList {}
native public interface Notation : Node {}
native public interface ProcessingInstruction : Node {}
native public interface Text : CharacterData {}
native public interface TypeInfo {}
native public interface UserDataHandler {}

