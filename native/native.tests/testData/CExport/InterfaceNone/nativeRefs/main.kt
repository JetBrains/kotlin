import kotlin.native.internal.ExportedBridge
import kotlin.native.internal.*
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.COpaquePointer

data object MyObject
const val string = "Hello, world!"
data class Data(var data: Unit = Unit)

@ExportedBridge("get_null")
@OptIn(ExperimentalForeignApi::class)
fun getNull(): COpaquePointer? = createSpecialRef(null)

@ExportedBridge("get_singleton_object")
@OptIn(ExperimentalForeignApi::class)
fun getSingletonObject(): COpaquePointer? = createSpecialRef(MyObject)

@ExportedBridge("get_static_object")
@OptIn(ExperimentalForeignApi::class)
fun getStaticObject(): COpaquePointer? = createSpecialRef(string)

@ExportedBridge("get_local_object")
@OptIn(ExperimentalForeignApi::class)
fun getLocalObject(): COpaquePointer? = createSpecialRef(Data())

@ExportedBridge("get_array")
@OptIn(ExperimentalForeignApi::class)
fun getArray(): COpaquePointer? = createSpecialRef(arrayOf(Data(), Data()))

@ExportedBridge("compare_identities")
@OptIn(ExperimentalForeignApi::class)
fun compareIdentities(obj1: COpaquePointer?, obj2: COpaquePointer?): Boolean = dereferenceSpecialRef(obj1) === dereferenceSpecialRef(obj2)

@ExportedBridge("compare_objects")
@OptIn(ExperimentalForeignApi::class)
fun compareObjects(obj1: COpaquePointer?, obj2: COpaquePointer?): Boolean = dereferenceSpecialRef(obj1) == dereferenceSpecialRef(obj2)

@ExportedBridge("compare_arrays")
@OptIn(ExperimentalForeignApi::class)
fun compareArrays(obj1: COpaquePointer?, obj2: COpaquePointer?): Boolean = (dereferenceSpecialRef(obj1) as? Array<Any>) contentEquals (dereferenceSpecialRef(obj2) as? Array<Any>)

@ExportedBridge("dispose_object")
@OptIn(ExperimentalForeignApi::class)
fun disposeObject(obj: COpaquePointer?): Unit = disposeSpecialRef(obj)

@ExportedBridge("retain_object")
@OptIn(ExperimentalForeignApi::class)
fun retainObject(ref: COpaquePointer?) = retainSpecialRef(ref)

@ExportedBridge("release_object")
@OptIn(ExperimentalForeignApi::class)
fun releaseObject(ref: COpaquePointer?) = releaseSpecialRef(ref)