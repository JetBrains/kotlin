// ISSUE: KT-90505
// DUMP_IR_DIFFERENCE: NATIVE, JS_IR
// LANGUAGE: +CompanionBlocks

import kotlinx.atomicfu.AtomicIntArray
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.getAndUpdate
import kotlinx.atomicfu.loop
import kotlinx.atomicfu.update
import kotlinx.atomicfu.updateAndGet

class Host {
    companion {
        private val counter = atomic(0)
        private val flag = atomic(false)
        private val total = atomic(10L)
        private val label = atomic<String?>("a")
        private val rows = AtomicIntArray(2)

        private fun doubleViaLoop(): Int {
            counter.loop { cur ->
                if (counter.compareAndSet(cur, cur * 2)) return cur * 2
            }
        }

        fun test(): String {
            if (counter.updateAndGet { it + 1 } != 1) return "Fail: updateAndGet"
            if (counter.getAndUpdate { it + 2 } != 1) return "Fail: getAndUpdate"
            counter.update { it + 1 }
            if (doubleViaLoop() != 8) return "Fail: loop"
            flag.update { !it }
            if (!flag.value) return "Fail: Boolean update"
            if (total.updateAndGet { it * 10 } != 100L) return "Fail: Long updateAndGet"
            if (label.updateAndGet { it + "b" } != "ab") return "Fail: Ref updateAndGet"
            if (rows[1].updateAndGet { it + 5 } != 5) return "Fail: array element updateAndGet"
            return "OK"
        }
    }
}

fun box(): String = Host.test()
