/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

package com.intellij.stats.personalization

/**
 * @author Vitaliy.Bibaev
 */
abstract class UserFactorBase<in R : FactorReader>(override val id: String, private val descriptor: UserFactorDescription<*, R>) : UserFactor {
    final override fun compute(storage: UserFactorStorage): String? {
        return compute(storage.getFactorReader(descriptor))
    }

    abstract fun compute(reader: R): String?
}