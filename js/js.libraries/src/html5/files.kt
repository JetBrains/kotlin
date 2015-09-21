package html5.files

@native
@Deprecated("Use org.w3c.dom")
public class FileReader() {
    public var onloadend : ((FileReaderEvent)->Unit)? = noImpl //
    public fun readAsDataURL(blob : Blob): Unit = noImpl
    public val result : File = noImpl
}

@native
@Deprecated("Use org.w3c.dom")
public class FileReaderEvent() {
    public val target : FileReader = noImpl
}

@native
@Deprecated("Use org.w3c.dom")
public class FileList() {
    public fun item(index : Int) : File? = noImpl
    public val length : Int = noImpl
}

@native
@Deprecated("Use org.w3c.dom")
public class File() : Blob() {
    public val name : String = noImpl
   // readonly attribute Date lastModifiedDate;
}

@native
@Deprecated("Use org.w3c.dom")
public open class Blob(blobParts: Array<Any>? = undefined, options: BlobPropertyBag? = undefined) {
    public val size: Int = noImpl
    public val type: String = noImpl
    //Blob slice(optional long long start,
    //optional long long end,
    //optional DOMString contentType);
    //
}

@native
@Deprecated("Use org.w3c.dom")
public interface BlobPropertyBag {
    public val type: String
}