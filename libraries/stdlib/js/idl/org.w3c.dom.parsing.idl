namespace org.w3c.dom.parsing;


// Downloaded from http://www.w3.org/TR/DOM-Parsing/
[Constructor]
interface DOMParser {
 [NewObject] Document parseFromString (DOMString str, SupportedType type);
};
[Constructor]
interface XMLSerializer {
 DOMString serializeToString (Node root);
};
partial interface Element {
 [CEReactions, TreatNullAs=EmptyString] attribute DOMString innerHTML;
 [CEReactions, TreatNullAs=EmptyString] attribute DOMString outerHTML;
 [CEReactions] void insertAdjacentHTML (DOMString position, DOMString text);
};
partial interface Range {
 [CEReactions, NewObject] DocumentFragment createContextualFragment (DOMString fragment);
};

