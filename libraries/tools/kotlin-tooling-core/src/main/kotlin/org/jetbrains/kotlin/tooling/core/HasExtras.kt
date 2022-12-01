/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tooling.core

import kotlin.reflect.KProperty


interface HasExtras {
    val extras: Extras
}

interface HasMutableExtras : HasExtras {
    override val extras: MutableExtras
}

operator fun <T : Any> Extras.Key<T>.getValue(receiver: HasExtras, property: KProperty<*>): T? {
    return receiver.extras[this]
}

operator fun <T : Any> Extras.Key<T>.setValue(receiver: HasMutableExtras, property: KProperty<*>, value: T?) {
    if (value == null) {
        receiver.extras.remove(this)
    } else {
        receiver.extras[this] = value
    }
}


