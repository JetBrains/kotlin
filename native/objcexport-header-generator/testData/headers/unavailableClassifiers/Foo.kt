import kotlinx.cinterop.ObjCObject
import kotlinx.cinterop.ExternalObjCClass

abstract class ListClass<T> : List<T>
interface ListInterface<T> : List<T>
interface ListInterfaceExtension<T> : ListInterface<T>

class ObjCObjectClass : ObjCObject
interface ObjCObjectInterface : ObjCObject

@ExternalObjCClass
class AnnotatedClass

@ExternalObjCClass
interface AnnotatedInterface

@ExternalObjCClass
object AnnotatedObject

@ExternalObjCClass
enum class AnnotatedEnum

@ExternalObjCClass
class AnnotatedObjCObjectClass : ObjCObject

@ExternalObjCClass
interface AnnotatedObjCObjectInterface : ObjCObject

@ExternalObjCClass
object AnnotatedObjCObjectObject : ObjCObject

@ExternalObjCClass
enum class AnnotatedObjCObjectEnum : ObjCObject