/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("UNCHECKED_CAST")

/**
 * Utility helper filer for creating Scala's datastructures
 * in a Kotlin-idiomatic way.
 */

package org.jetbrains.kotlin.formver.scala

import scala.Tuple2
import scala.collection.immutable.Map
import scala.collection.immutable.`Map$`
import scala.collection.immutable.Seq
import scala.jdk.javaapi.CollectionConverters

interface IntoScala<T> {
    fun toScala(): T
}

sealed class Option<in T> : IntoScala<scala.Option<@UnsafeVariance T>> {
    class None<T> : Option<T>() {
        override fun toScala(): scala.Option<T> = scala.`None$`.`MODULE$` as scala.Option<T>
    }

    class Some<T>(private val value: T) : Option<T>() {
        override fun toScala(): scala.Option<T> = scala.Some(value)
    }
}

fun Int.toScalaBigInt(): scala.math.BigInt = scala.math.BigInt.apply(this)

fun Long.toScalaBigInt(): scala.math.BigInt = scala.math.BigInt.apply(this)

fun <T> T?.toScalaOption(): scala.Option<T> = scala.Option.apply(this)

fun <K, V> emptyScalaMap(): Map<K, V> = `Map$`.`MODULE$`.empty()

fun <K, V> kotlin.collections.Map<K, V>.toScalaMap(): Map<K, V> =
    `Map$`.`MODULE$`.from(entries.map { Tuple2(it.key, it.value) }.toScalaSeq())

fun <T> emptySeq(): Seq<T> = CollectionConverters.asScala(emptyList<T>()).toSeq()

fun <T> seqOf(vararg elements: T): Seq<T> = CollectionConverters.asScala(elements.asList()).toSeq()

fun <T> seqOf(elements: List<T>): Seq<T> = CollectionConverters.asScala(elements).toSeq()

fun <T> List<T>.toScalaSeq() = seqOf(this)

