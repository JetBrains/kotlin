@file:OptIn(ExperimentalAssociatedObjects::class)

import kotlin.reflect.*

@AssociatedObjectKey
annotation class Annotation1(val kClass: KClass<out Any>)
