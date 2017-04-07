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

import kotlinx.cinterop.*
import MessageChannel.*

val nameToWorker = mapOf("worker1" to ::Worker1, "worker2" to ::Worker2)

fun StartWorker(target: String, name: String, vararg args: String): worker_id_t {
    return memScoped {
        val workerArgs = arrayOf(name, *args)
        CreateWorker(target, workerArgs.size, workerArgs.map { it.cstr.getPointer(memScope) }.toCValues())
    }
}

fun CheckWorker(args: Array<String>) : Boolean {
    if (args.size == 0) return false
    val handler = nameToWorker[args[0]]
    if (handler == null) return false
    handler(args)
    return true
}

fun Worker1(args: Array<String>) {
    println("I am Worker1 passed ${args[1]}")
    memScoped {
        val message = CreateMessage(10)!!
        var main_thread : Int
        while (true) {
            val result = GetMessage(message, -1)
            if (result == 0) {
                main_thread = message.pointed.source_
                println("${args[1]} got message ${message.pointed.kind_} ${message.pointed.source_}->${message.pointed.destination_}")
                if (message.pointed.kind_ == 42) break
            }
        }
        message.pointed.kind_ = 1
        SendMessage(main_thread, message)
    }
}

fun Worker2(args: Array<String>) {
    println("I am Worker2 passed ${args[1]}")
    memScoped {
        val message = CreateMessage(10)!!
        var main_thread : Int
        while (true) {
            val result = GetMessage(message, -1)
            if (result == 0) {
                main_thread = message.pointed.source_
                println("${args[1]} got message ${message.pointed.kind_} ${message.pointed.source_}->${message.pointed.destination_}")
                if (message.pointed.kind_ == 42) break
            }
        }
        message.pointed.kind_ = 2
        SendMessage(main_thread, message)
    }
}

fun main(args: Array<String>) {
    val id = if (args.size > 0) args[0] else "main"
    println("Hi from $id")
    if (CheckWorker(args)) return

    memScoped {
        val worker1 = StartWorker("thread", "worker1", "Foo")
        val worker2 = StartWorker("thread", "worker2", "Bar")

        val message1 = CreateMessage(20)!!
        val message2 = CreateMessage(20)!!
        message1.pointed.kind_ = 1239
        message2.pointed.kind_ = 1566

        for (i in 1 .. 200) {
            SendMessage(worker1, message1)
            message1.pointed.kind_++
            SendMessage(worker2, message2)
            message2.pointed.kind_--
        }

        // Send termination message.
        message1.pointed.kind_ = 42
        SendMessage(worker1, message1)
        message2.pointed.kind_ = 42
        SendMessage(worker2, message2)

        // Wait for termination acknowledgment.
        for (i in 1 .. 2) {
            val result = GetMessage(message1, 1000)
            if (result == 0) {
                println("main got pingpack ${message1.pointed.kind_}")
            }
        }
    }
}
