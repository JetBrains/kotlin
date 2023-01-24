import clib.*
import cnames.structs.ForwardDeclaredStruct
import objcnames.classes.ForwardDeclaredClass
import objcnames.protocols.ForwardDeclaredProtocolProtocol
import kotlinx.cinterop.CPointer

fun sameForwardDeclaredStruct(ptr: CPointer<ForwardDeclaredStruct>?): CPointer<ForwardDeclaredStruct>? = sameStruct(ptr)
fun sameForwardDeclaredClass(obj: ForwardDeclaredClass?): ForwardDeclaredClass? = sameClass(obj)
fun sameForwardDeclaredProtocol(obj: ForwardDeclaredProtocolProtocol?): ForwardDeclaredProtocolProtocol? = sameProtocol(obj)
