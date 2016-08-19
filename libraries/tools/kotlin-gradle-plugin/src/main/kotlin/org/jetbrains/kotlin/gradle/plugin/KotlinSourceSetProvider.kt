package org.jetbrains.kotlin.gradle.plugin

interface KotlinSourceSetProvider {
    fun create(displayName: String): KotlinSourceSet
}