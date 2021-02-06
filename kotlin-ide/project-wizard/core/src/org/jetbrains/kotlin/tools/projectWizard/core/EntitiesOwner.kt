package org.jetbrains.kotlin.tools.projectWizard.core

import org.jetbrains.annotations.NonNls

interface EntitiesOwnerDescriptor {
    @get:NonNls
    val id: String
}

interface EntitiesOwner<D : EntitiesOwnerDescriptor> {
    val descriptor: D
}