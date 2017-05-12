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

package konan.internal

import kotlin.reflect.KFunction

@FixmeReflection
open class KFunctionImpl<out R>(override val name: String, val fqName: String, val bound: Boolean, val receiver: Any?): KFunction<R> {
    override fun equals(other: Any?): Boolean {
        if (other !is KFunctionImpl<*>) return false
        return fqName == other.fqName && bound == other.bound && receiver == other.receiver
    }

    override fun hashCode(): Int {
        return (fqName.hashCode() * 31 + if (bound) 1 else 0) * 31 + receiver.hashCode()
    }

    override fun toString(): String {
        return fqName
    }
}