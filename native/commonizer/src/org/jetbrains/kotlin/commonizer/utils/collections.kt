/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.utils

internal typealias CommonizerMap<K, V> = it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap<K, V>
internal typealias CommonizerSet<E> = it.unimi.dsi.fastutil.objects.ObjectOpenHashSet<E>
internal typealias CommonizerIntObjectMap<V> = it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<V>
internal typealias CommonizerIntSet = it.unimi.dsi.fastutil.ints.IntOpenHashSet
