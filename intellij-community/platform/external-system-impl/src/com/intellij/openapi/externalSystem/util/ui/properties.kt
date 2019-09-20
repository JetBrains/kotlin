// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.util.ui

import kotlin.properties.ReadWriteProperty

fun <T> property(initial: () -> T) = UiProperty(initial)

fun <R, T> ReadWriteProperty<R, T>.map(transform: (T) -> T) = Property.PropertyView(this, transform, { it })

fun <R, T> ReadWriteProperty<R, T>.comap(transform: (T) -> T) = Property.PropertyView(this, { it }, transform)