package test

external class ExternalClass {
    fun addedExternalFun()
    val addedExternalVal: Int
}

external class ClassBecameExternal
class ClassBecameNonExternal

external fun addedExternalFun()
external val addedExternalVal: Int

external fun funBecameExternal()
fun funBecameNonExternal() {}