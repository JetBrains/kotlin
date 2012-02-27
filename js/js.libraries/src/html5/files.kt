package html5.files

import js.native

native
class FileReader() {
    var onloadend : ((FileReaderEvent)->Unit)? = js.noImpl //
    fun readAsDataURL(blob : Blob) = js.noImpl
    val result : File = js.noImpl
}

native
class FileReaderEvent() {
    val target : FileReader = js.noImpl
}

native
class FileList() {
    fun item(index : Int) : File? = js.noImpl
    val length : Int = js.noImpl
}

native
class File() : Blob() {
    val name : String = js.noImpl
   // readonly attribute Date lastModifiedDate;
}

native
open class Blob() {
    val size : Int = js.noImpl
    val `type` : String = js.noImpl
    //Blob slice(optional long long start,
    //optional long long end,
    //optional DOMString contentType);
    //
}
