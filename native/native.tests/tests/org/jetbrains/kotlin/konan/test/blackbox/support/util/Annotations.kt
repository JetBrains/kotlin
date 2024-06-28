/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.util

/**
 * Since Kotlin annotations are not inherited (see KT-22265), we need a utility to collect all of them.
 */
internal val Class<*>.allInheritedAnnotations: List<Annotation>
    get() {
        val annotations = mutableListOf<Annotation>()

        val processedClasses = hashSetOf<Class<*>>()
        fun process(clazz: Class<*>) {
            if (processedClasses.add(clazz)) {
                clazz.declaredAnnotations.mapNotNullTo(annotations) { annotation ->
                    annotation.takeIf { it !is Metadata }
                }
                clazz.interfaces.forEach(::process)
                clazz.superclass.takeIf { it != Object::class.java }?.let(::process)
            }
        }

        process(this)

        return annotations
    }
