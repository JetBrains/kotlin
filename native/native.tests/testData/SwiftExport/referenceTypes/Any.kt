import kotlin.native.internal.isPermanent

class Class

val instance = Class()

object Object

fun isMainObject(obj: Any): Boolean = obj == instance

fun getMainObject(): Any = instance

fun isMainPermanentObject(obj: Any): Boolean = obj == Object

fun getMainPermanentObject(): Any = Object

