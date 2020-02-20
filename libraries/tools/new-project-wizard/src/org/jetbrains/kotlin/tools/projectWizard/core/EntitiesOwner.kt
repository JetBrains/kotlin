package org.jetbrains.kotlin.tools.projectWizard.core

interface EntitiesOwnerDescriptor {
    val id: String
}

interface EntitiesOwner<D : EntitiesOwnerDescriptor> {
    val descriptor: D
}