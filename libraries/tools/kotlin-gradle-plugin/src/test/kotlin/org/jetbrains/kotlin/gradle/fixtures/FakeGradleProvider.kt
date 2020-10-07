/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.fixtures

import org.gradle.api.Transformer
import org.gradle.api.provider.Provider
import java.util.function.BiFunction

class FakeGradleProvider<T>(private val v: (()-> T)?): Provider<T> {

    constructor(v: T): this({v})

    override fun <S : Any?> flatMap(transformer: Transformer<out Provider<out S>, in T>): Provider<S> {
        @Suppress("UNCHECKED_CAST")
        return transformer.transform(v!!.invoke()) as Provider<S>
    }

    override fun isPresent() = v != null

    override fun getOrElse(p0: T) = if (isPresent) orNull else p0

    override fun <S : Any> map(transformer: Transformer<out S, in T>): Provider<S> {
        return FakeGradleProvider { transformer.transform(get()) }
    }

    override fun get() = orNull!!

    override fun getOrNull() = v?.invoke()

    override fun orElse(p0: T): Provider<T> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun orElse(p0: Provider<out T>): Provider<T> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun forUseAtConfigurationTime(): Provider<T> {
        TODO("Not yet implemented")
    }

    override fun <B : Any?, R : Any?> zip(p0: Provider<B>, p1: BiFunction<T, B, R>): Provider<R> {
        TODO("Not yet implemented")
    }
}