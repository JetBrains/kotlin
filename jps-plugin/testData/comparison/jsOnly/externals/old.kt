package test

external class ExternalClass {
    fun removedExternalFun()
    val removedExternalVal: Int
}

class ClassBecameExternal
external class ClassBecameNonExternal

external fun removedExternalFun()
external val removedExternalVal: Int

fun funBecameExternal() {}
external fun funBecameNonExternal()
