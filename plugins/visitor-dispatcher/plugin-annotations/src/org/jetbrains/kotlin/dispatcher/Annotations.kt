/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.dispatcher

@Target(AnnotationTarget.CLASS)
annotation class DispatchedVisitor<ExprT>

@Target(AnnotationTarget.CLASS)
annotation class GenerateDispatchFunction

@Target(AnnotationTarget.FUNCTION)
annotation class Dispatched

@Target(AnnotationTarget.CLASS)
annotation class WithAbstractKind<T: Enum<*>>

@Target(AnnotationTarget.CLASS)
annotation class WithKind<T: Enum<*>>(val kind: String)

