@file:OptIn(ExperimentalAssociatedObjects::class)

import kotlin.reflect.*

@AssociatedObjectKey
annotation class Annotation3(val kClass: KClass<out Any>)
