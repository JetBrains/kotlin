package org.jetbrains.kotlin.tools.projectWizard.core

import kotlin.reflect.KClass

interface EntitiesOwnerDescriptor {
    val id: String
}

interface EntitiesOwner<D : EntitiesOwnerDescriptor> {
    val descriptor: D
}