package org.w3c.dom.parsing

public open external class DOMParser {
    /*primary*/ public constructor DOMParser()
    public final fun parseFromString(/*0*/ str: kotlin.String, /*1*/ type: dynamic): org.w3c.dom.Document
}

public open external class XMLSerializer {
    /*primary*/ public constructor XMLSerializer()
    public final fun serializeToString(/*0*/ root: org.w3c.dom.Node): kotlin.String
}