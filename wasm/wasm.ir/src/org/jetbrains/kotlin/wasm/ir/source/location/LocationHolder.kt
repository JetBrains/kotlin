/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.ir.source.location

@JvmInline
value class LocationHolder(val location: SourceLocation)

inline fun <R> withLocation(location: SourceLocation, body: LocationHolder.() -> R): R = LocationHolder(location).body()

inline fun <R> withNoLocation(body: LocationHolder.() -> R): R = withLocation(SourceLocation.NoLocation, body)

inline fun <R> withTBDLocation(body: LocationHolder.() -> R): R = withLocation<R>(SourceLocation.TBDLocation, body)
