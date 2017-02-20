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

@file:JvmName("ProtocolCallSite")

package kotlin.jvm.internal

import java.lang.invoke.*
import java.lang.reflect.Method

class ProtocolCallSite(private val lookup: MethodHandles.Lookup, name: String, type: MethodType, private val callableName: String, private val callableType: MethodType) {

    private var cache: Class<*>? = null
    private var handle: MethodHandle? = null
    private var reflectMethod: Method? = null

    companion object {
        @JvmStatic
        fun getBootstrap(lookup: MethodHandles.Lookup, name: String, type: MethodType, callableName: String, callableType: MethodType): CallSite {
            val instance = ProtocolCallSite(lookup, name, type, callableName, callableType)
            return ConstantCallSite(MethodHandles.constant(ProtocolCallSite::class.java, instance))
        }
    }

    fun getMethod(receiver: Any): MethodHandle? {
        val receiverClass = receiver.javaClass
        if (cache == null || cache != receiverClass) {
            cache = receiverClass
            val method = setupAccessible(receiverClass)
            handle = lookup.unreflect(method)
        }
        return handle
    }

    fun getReflectMethod(receiver: Any): Method? {
        val receiverClass = receiver.javaClass
        if (cache == null || cache != receiverClass) {
            cache = receiverClass
            reflectMethod = setupAccessible(receiverClass)
        }

        return reflectMethod
    }

    private fun setupAccessible(cls: Class<*>): Method {
        val method = cls.getDeclaredMethod(callableName, *callableType.parameterArray())
        if (!method.isAccessible) {
            method.isAccessible = true
        }

        return method
    }
}