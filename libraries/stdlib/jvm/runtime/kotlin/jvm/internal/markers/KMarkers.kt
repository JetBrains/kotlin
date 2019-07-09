/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jvm.internal.markers

interface KMappedMarker

interface KMutableIterable : KMappedMarker

interface KMutableCollection : KMutableIterable

interface KMutableList : KMutableCollection

interface KMutableIterator : KMappedMarker

interface KMutableListIterator : KMutableIterator

interface KMutableMap : KMappedMarker {
    interface Entry : KMappedMarker
}

interface KMutableSet : KMutableCollection