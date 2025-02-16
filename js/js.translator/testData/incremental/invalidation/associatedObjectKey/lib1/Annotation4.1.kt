@file:OptIn(ExperimentalAssociatedObjects::class)

import kotlin.reflect.*

@AssociatedObjectKey
annotation class Annotation4(val kClass: KClass<out Any>)
