@file:OptIn(ExperimentalAssociatedObjects::class)

import kotlin.reflect.*

@AssociatedObjectKey
annotation class Annotation2(val kClass: KClass<out Any>)
