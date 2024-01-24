@file:OptIn(ExperimentalAssociatedObjects::class)

import kotlin.reflect.AssociatedObjectKey
import kotlin.reflect.ExperimentalAssociatedObjects
import kotlin.reflect.findAssociatedObject
import kotlin.reflect.KClass

private var obj1Init = false
private var obj2Init = false
private var obj3Init = false

@AssociatedObjectKey
annotation class KEY1(val kClass: KClass<*>)

@AssociatedObjectKey
annotation class KEY2(val kClass: KClass<*>)

private object OBJ1 {
    init { obj1Init = true }
}

private object OBJ2 {
    init { obj2Init = true }
}

private object OBJ3 {
    init { obj3Init = true }
}

@KEY1(OBJ1::class)
class CLS1

@KEY1(OBJ2::class)
@KEY2(OBJ3::class)
class CLS2

fun box(): String {
    // No objects initialised
    if (obj1Init) return "FAIL1"
    if (obj2Init) return "FAIL2"
    if (obj3Init) return "FAIL3"

    CLS1::class.findAssociatedObject<KEY2>()
    if (obj1Init) return "FAIL4"
    if (obj2Init) return "FAIL5"
    if (obj3Init) return "FAIL6"

    CLS1::class.findAssociatedObject<KEY1>()
    if (!obj1Init) return "FAIL7"
    if (obj2Init) return "FAIL8"
    if (obj3Init) return "FAIL9"

    CLS2::class.findAssociatedObject<KEY1>()
    if (!obj1Init) return "FAIL10"
    if (!obj2Init) return "FAIL11"
    if (obj3Init) return "FAIL12"

    CLS2::class.findAssociatedObject<KEY2>()
    if (!obj1Init) return "FAIL13"
    if (!obj2Init) return "FAIL14"
    if (!obj3Init) return "FAIL15"

    return "OK"
}