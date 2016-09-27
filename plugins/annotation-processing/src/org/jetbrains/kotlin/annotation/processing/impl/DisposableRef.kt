/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.annotation.processing.impl

import com.intellij.openapi.Disposable

class DisposableRef<out T : Any>(initialValue: T): Disposable {
    @Volatile
    private var value: T? = initialValue
    
    operator fun invoke() = value ?: throw IllegalStateException("Reference is disposed")
    
    override fun dispose() {
        value = null
    }
}

fun <T : Any> T.toDisposable() = DisposableRef(this)

fun dispose(vararg refs: DisposableRef<*>?) {
    refs.forEach { it?.dispose() }
}