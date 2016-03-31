/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.uast

import org.jetbrains.uast.visitor.UastVisitor

/**
    Represents an applied JVM annotation.
 */
interface UAnnotation : UElement, UNamed, UFqNamed {
    /**
     * Returns the list of named value arguments.
     */
    val valueArguments: List<UNamedExpression>

    /**
     * Returns the class of this annotation.
     *
     * @return annotation class or null if the class was not resolved
     */
    fun resolve(context: UastContext): UClass?

    /**
     * Returns the evaluated value of the argument with the specified name.
     *
     * @param name name of the annotation value parameter
     * @return the argument value
     */
    fun getValue(name: String) = valueArguments.firstOrNull { it.name == name }?.expression?.evaluate()

    /**
     * Returns the list of Pair(name, evaluatedValue) for all applied annotation arguments.
     *
     * @return the list of name-value pairs.
     */
    fun getValues() = valueArguments.map { Pair(it.name, it.expression.evaluate()) }

    override fun logString() = log("UAnnotation ($name)")

    override fun renderString(): String {
        return if (valueArguments.isEmpty())
            "@$name"
        else
            "@$name(" + valueArguments.joinToString { it.renderString() } + ")"
    }

    override fun accept(visitor: UastVisitor) {
        if (visitor.visitAnnotation(this)) return
        valueArguments.acceptList(visitor)
        visitor.afterVisitAnnotation(this)
    }
}

/**
 * Represents an annotated element.
 */
interface UAnnotated : UElement {
    /**
     * Returns the list of annotations applied to the current element.
     */
    val annotations: List<UAnnotation>
}