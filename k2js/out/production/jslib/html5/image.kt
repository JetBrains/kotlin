package html5

import js.Exception
import js.native
import js.DomElement
import html5.files.FileList

native
class HTMLInputElement() : DomElement() {
    val files : FileList = js.noImpl
    var onchange : (HTMLInputElementEvent)->Unit = js.noImpl
}

native
class HTMLInputElementEvent() {
    val target : HTMLInputElement = js.noImpl
}

native
class Image() : HTMLImageElement() {
    var height : Int = js.noImpl
    var width : Int = js.noImpl
    var src : String = js.noImpl
    var onload : ()->Unit = js.noImpl
}