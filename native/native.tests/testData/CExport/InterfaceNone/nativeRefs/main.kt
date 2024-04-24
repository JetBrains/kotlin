import kotlin.native.internal.ExportedBridge
import kotlin.native.internal.*
import kotlin.native.internal.ref.*
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.COpaquePointer

data object MyObject
const val string = "Hello, world!"
data class Data(var data: Unit = Unit)

@ExportedBridge("get_singleton_object")
@OptIn(ExperimentalForeignApi::class)
fun getSingletonObject(): NativePtr = createRetainedExternalRCRef(MyObject)

@ExportedBridge("get_static_object")
@OptIn(ExperimentalForeignApi::class)
fun getStaticObject(): NativePtr = createRetainedExternalRCRef(string)

@ExportedBridge("get_local_object")
@OptIn(ExperimentalForeignApi::class)
fun getLocalObject(): NativePtr = createRetainedExternalRCRef(Data())

@ExportedBridge("get_array")
@OptIn(ExperimentalForeignApi::class)
fun getArray(): NativePtr = createRetainedExternalRCRef(arrayOf(Data(), Data()))

@ExportedBridge("compare_identities")
@OptIn(ExperimentalForeignApi::class)
fun compareIdentities(obj1: NativePtr, obj2: NativePtr): Boolean = dereferenceExternalRCRef(obj1) === dereferenceExternalRCRef(obj2)

@ExportedBridge("compare_objects")
@OptIn(ExperimentalForeignApi::class)
fun compareObjects(obj1: NativePtr, obj2: NativePtr): Boolean = dereferenceExternalRCRef(obj1) == dereferenceExternalRCRef(obj2)

@ExportedBridge("compare_arrays")
@OptIn(ExperimentalForeignApi::class)
fun compareArrays(obj1: NativePtr, obj2: NativePtr): Boolean = (dereferenceExternalRCRef(obj1) as? Array<Any>) contentEquals (dereferenceExternalRCRef(obj2) as? Array<Any>)

@ExportedBridge("dispose_object")
@OptIn(ExperimentalForeignApi::class)
fun disposeObject(obj: NativePtr): Unit = disposeExternalRCRef(obj)

@ExportedBridge("retain_object")
@OptIn(ExperimentalForeignApi::class)
fun retainObject(ref: NativePtr) = retainExternalRCRef(ref)

@ExportedBridge("release_object")
@OptIn(ExperimentalForeignApi::class)
fun releaseObject(ref: NativePtr) = releaseExternalRCRef(ref)
