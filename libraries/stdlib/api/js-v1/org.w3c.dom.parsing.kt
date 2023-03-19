/*∆*/ public open external class DOMParser {
/*∆*/     public constructor DOMParser()
/*∆*/ 
/*∆*/     public final fun parseFromString(str: kotlin.String, type: dynamic): org.w3c.dom.Document
/*∆*/ }
/*∆*/ 
/*∆*/ public open external class XMLSerializer {
/*∆*/     public constructor XMLSerializer()
/*∆*/ 
/*∆*/     public final fun serializeToString(root: org.w3c.dom.Node): kotlin.String
/*∆*/ }