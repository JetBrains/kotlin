/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm

import org.jetbrains.kotlin.gradle.kpm.idea.InternalKotlinGradlePluginApi
import java.io.Serializable
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf

@Suppress("FunctionName")
inline fun <reified T : Any> KotlinExternalModelType(): KotlinExternalModelType<T> {
    return KotlinExternalModelType(externalModelTypeSignature(typeOf<T>()))
}

class KotlinExternalModelType</* Used as phantom type */ @Suppress("unused") T: Any>
@PublishedApi internal constructor(private val signature: String) : Serializable {
    override fun equals(other: Any?): Boolean {
        if (other !is KotlinExternalModelType<*>) return false
        if (other.signature != this.signature) return false
        return true
    }

    override fun hashCode(): Int = 31 * signature.hashCode()
    override fun toString(): String = "ExternalModelType($signature)"

    @InternalKotlinGradlePluginApi
    companion object {
        private const val serialVersionUID = 0L
    }
}

@PublishedApi
internal fun externalModelTypeSignature(type: KType): String {
    require(!type.isMarkedNullable) { "Unexpected nullable type. Found $type" }
    require(type.arguments.isEmpty()) { "Parameterized types are not supported. Found $type" }
    val classifier = requireNotNull(type.classifier) { "Expected classifier. Found $type" }
    val clazz = (classifier as? KClass<*>) ?: throw IllegalArgumentException("Expected KClass classifier. Found $classifier")
    return clazz.java.name ?: throw IllegalArgumentException("Missing qualifiedName in $type")
}
