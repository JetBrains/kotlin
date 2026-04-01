@file:OptIn(ExperimentalAssociatedObjects::class)

import kotlin.reflect.*

@AssociatedObjectKey
annotation class ZClass(val kClass: KClass<out Any>)
