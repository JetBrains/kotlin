package html5.files

native
public class FileReader() {
    public var onloadend : ((FileReaderEvent)->Unit)? = noImpl //
    public fun readAsDataURL(blob : Blob): Unit = noImpl
    public val result : File = noImpl
}

native
public class FileReaderEvent() {
    public val target : FileReader = noImpl
}

native
public class FileList() {
    public fun item(index : Int) : File? = noImpl
    public val length : Int = noImpl
}

native
public class File() : Blob() {
    public val name : String = noImpl
   // readonly attribute Date lastModifiedDate;
}

native
public open class Blob() {
    public val size : Int = noImpl
    public val type : String = noImpl
    //Blob slice(optional long long start,
    //optional long long end,
    //optional DOMString contentType);
    //
}
