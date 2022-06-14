/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.kpm.idea

import java.io.Serializable

fun IdeaKpmProjectContainer(project: ByteArray): IdeaKpmProjectBinaryContainer {
    return IdeaKpmProjectBinaryContainerImpl(project)
}

fun IdeaKpmProjectContainer(project: IdeaKpmProject): IdeaKpmProjectInstanceContainer {
    return IdeaKpmProjectInstanceContainerImpl(project)
}

/**
 * Wrapper around [IdeaKpmProject] which can store the project in two forms
 * - binary : [IdeaKpmProjectBinaryContainer]
 * - instance: [IdeaKpmProjectInstanceContainer]
 *
 * This class is used to transport the [IdeaKpmProject] into the IDE, where it needs
 * to take those two forms, while keeping the same class as key on IJ side.
 *
 * This class overcomes a limitation in IntelliJ's SerializationService, which basically
 * requires a single class.
 *
 * When this container is requested from a Model Builder, it will
 * return the binary form. This gets deserialized by the SerializationService and transformed
 * into [IdeaKpmProjectInstanceContainer].
 */
sealed interface IdeaKpmProjectContainer<T : Any> : Serializable {
    val project: T
    val binaryOrNull: ByteArray?
    val instanceOrNull: IdeaKpmProject?
}

interface IdeaKpmProjectBinaryContainer : IdeaKpmProjectContainer<ByteArray> {
    override val instanceOrNull: Nothing? get() = null
    override val binaryOrNull: ByteArray get() = project
}

interface IdeaKpmProjectInstanceContainer : IdeaKpmProjectContainer<IdeaKpmProject> {
    override val instanceOrNull: IdeaKpmProject get() = project
    override val binaryOrNull: Nothing? get() = null
}

private data class IdeaKpmProjectBinaryContainerImpl(override val project: ByteArray) : IdeaKpmProjectBinaryContainer {
    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is IdeaKpmProjectBinaryContainer) return false
        return other.project.contentEquals(this.project)
    }

    override fun hashCode(): Int {
        return project.contentHashCode()
    }

    companion object {
        const val serialVersionUID = 0L
    }
}

private data class IdeaKpmProjectInstanceContainerImpl(
    override val project: IdeaKpmProject
) : IdeaKpmProjectInstanceContainer {
    companion object {
        const val serialVersionUID = 0L
    }
}
