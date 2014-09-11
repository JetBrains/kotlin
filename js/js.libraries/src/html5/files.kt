package html5.files

native
public class FileReader() {
    public var onloadend : ((FileReaderEvent)->Unit)? = js.noImpl //
    public fun readAsDataURL(blob : Blob) = js.noImpl
    public val result : File = js.noImpl
}

native
public class FileReaderEvent() {
    public val target : FileReader = js.noImpl
}

native
public class FileList() {
    public fun item(index : Int) : File? = js.noImpl
    public val length : Int = js.noImpl
}

native
public class File() : Blob() {
    public val name : String = js.noImpl
   // readonly attribute Date lastModifiedDate;
}

native
public open class Blob() {
    public val size : Int = js.noImpl
    public val `type` : String = js.noImpl
    //Blob slice(optional long long start,
    //optional long long end,
    //optional DOMString contentType);
    //
}
