/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jvm.internal.markers

public interface KMappedMarker

public interface KMutableIterable : KMappedMarker

public interface KMutableCollection : KMutableIterable

public interface KMutableList : KMutableCollection

public interface KMutableIterator : KMappedMarker

public interface KMutableListIterator : KMutableIterator

public interface KMutableMap : KMappedMarker {
    public interface Entry : KMappedMarker
}

public interface KMutableSet : KMutableCollection