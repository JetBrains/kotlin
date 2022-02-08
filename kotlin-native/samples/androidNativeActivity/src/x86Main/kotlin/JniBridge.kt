/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package sample.androidnative

import kotlinx.cinterop.*
import platform.android.*

data class JniClass(val jclass: jclass)
data class JniObject(val jobject: jobject)
data class JniMethod(val jmethod: jmethodID)

fun asJniClass(jclass: jclass?) =
        if (jclass != null) JniClass(jclass) else null

fun asJniObject(jobject: jobject?) =
        if (jobject != null) JniObject(jobject) else null

fun asJniMethod(jmethodID: jmethodID?) =
        if (jmethodID != null) JniMethod(jmethodID) else null

class JniBridge(val vm: CPointer<JavaVMVar>) {
    private val vmFunctions: JNIInvokeInterface = vm.pointed.pointed!!
    val jniEnv = memScoped {
        val envStorage = alloc<CPointerVar<JNIEnvVar>>()
        if (vmFunctions.AttachCurrentThreadAsDaemon!!(vm, envStorage.ptr, null) != 0)
            throw Error("Cannot attach thread to the VM")
        envStorage.value!!
    }
    private val envFunctions: JNINativeInterface = jniEnv.pointed.pointed!!

    // JNI operations.
    private val fNewStringUTF = envFunctions.NewStringUTF!!
    private val fFindClass = envFunctions.FindClass!!
    private val fGetMethodID = envFunctions.GetMethodID!!
    private val fCallVoidMethodA = envFunctions.CallVoidMethodA!!
    private val fCallObjectMethodA = envFunctions.CallObjectMethodA!!
    private val fExceptionCheck = envFunctions.ExceptionCheck!!
    private val fExceptionDescribe = envFunctions.ExceptionDescribe!!
    private val fExceptionClear = envFunctions.ExceptionClear!!
    val fPushLocalFrame = envFunctions.PushLocalFrame!!
    val fPopLocalFrame = envFunctions.PopLocalFrame!!

    private fun check() {
        if (fExceptionCheck(jniEnv) != 0.toUByte()) {
            fExceptionDescribe(jniEnv)
            fExceptionClear(jniEnv)
            throw Error("JVM exception thrown")
        }
    }

    fun toJString(string: String) = memScoped {
        val result = asJniObject(fNewStringUTF(jniEnv, string.cstr.ptr))
        check()
        result
    }

    fun toJValues(arguments: Array<out Any?>, scope: MemScope): CPointer<jvalue>? {
        val result = scope.allocArray<jvalue>(arguments.size)
        arguments.mapIndexed { index, it ->
            when (it) {
                null -> result[index].l = null
                is JniObject -> result[index].l = it.jobject
                is String -> result[index].l = toJString(it)?.jobject
                is Int -> result[index].i = it
                is Long -> result[index].j = it
                else -> throw Error("Unsupported conversion for ${it::class.simpleName}")
            }
        }
        return result
    }

    fun FindClass(name: String) = memScoped {
        val result = asJniClass(fFindClass(jniEnv, name.cstr.ptr))
        check()
        result
    }

    fun GetMethodID(clazz: JniClass?, name: String, signature: String) = memScoped {
        val result = asJniMethod(fGetMethodID(jniEnv, clazz?.jclass, name.cstr.ptr, signature.cstr.ptr))
        check()
        result
    }

    fun CallVoidMethod(receiver: JniObject?, method: JniMethod, vararg arguments: Any?) = memScoped {
        fCallVoidMethodA(jniEnv, receiver?.jobject, method.jmethod,
                toJValues(arguments, this@memScoped))
        check()
    }

    fun CallObjectMethod(receiver: JniObject?, method: JniMethod, vararg arguments: Any?) = memScoped {
        val result = asJniObject(fCallObjectMethodA(jniEnv, receiver?.jobject, method.jmethod,
                toJValues(arguments, this@memScoped)))
        check()
        result
    }

    // Usually, use this
    inline fun <T> withLocalFrame(block: JniBridge.() -> T): T {
        if (fPushLocalFrame(jniEnv, 0) < 0)
            throw Error("Cannot push new local frame")
        try {
            return block()
        } finally {
            fPopLocalFrame(jniEnv, null)
        }
    }
}
