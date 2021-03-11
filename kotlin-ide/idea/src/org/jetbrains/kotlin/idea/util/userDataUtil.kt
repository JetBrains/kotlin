/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.util

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.util.Key
import kotlin.reflect.KProperty

class DataNodeUserDataProperty<in R : DataNode<*>, T : Any>(val key: Key<T>) {
    operator fun getValue(thisRef: R, property: KProperty<*>) = thisRef.getUserData(key)

    operator fun setValue(thisRef: R, property: KProperty<*>, value: T?) = thisRef.putUserData(key, value)
}

class CopyableDataNodeUserDataProperty<in R : DataNode<*>, T : Any>(val key: Key<T>) {
    operator fun getValue(thisRef: R, property: KProperty<*>) = thisRef.getCopyableUserData(key)

    operator fun setValue(thisRef: R, property: KProperty<*>, value: T?) = thisRef.putCopyableUserData(key, value)
}

class NotNullableCopyableDataNodeUserDataProperty<in R : DataNode<*>, T : Any>(val key: Key<T>, val defaultValue: T) {
    operator fun getValue(thisRef: R, property: KProperty<*>) = thisRef.getCopyableUserData(key) ?: defaultValue

    operator fun setValue(thisRef: R, property: KProperty<*>, value: T) {
        thisRef.putCopyableUserData(key, if (value != defaultValue) value else null)
    }
}