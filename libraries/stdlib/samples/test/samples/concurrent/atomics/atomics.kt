/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package samples.concurrent.atomics

import samples.Sample
import samples.assertPrints
import kotlin.concurrent.atomics.*
import kotlin.concurrent.thread

@OptIn(ExperimentalAtomicApi::class)
class AtomicInt {
    @Sample
    fun load() {
        val a = AtomicInt(7)
        assertPrints(a.load(), "7")
    }

    @Sample
    fun store() {
        val a = AtomicInt(7)
        a.store(10)
        assertPrints(a.load(), "10")
    }

    @Sample
    fun exchange() {
        val a = AtomicInt(7)
        assertPrints(a.exchange(10), "7")
        assertPrints(a.load(), "10")
    }

    @Sample
    fun compareAndSet() {
        val a = AtomicInt(7)
        // Current value 7 is equal to the expected value 7 -> compareAndSet succeeds.
        assertPrints(a.compareAndSet(7, 10), "true")
        assertPrints(a.load(), "10")

        // Current value 10 is not equal to the expected value 2 -> compareAndSet fails.
        assertPrints(a.compareAndSet(2, 12), "false")
        assertPrints(a.load(), "10")
    }

    @Sample
    fun compareAndExchange() {
        val a = AtomicInt(7)
        // Current value 7 is equal to the expected value 7 ->
        // compareAndExchange succeeds, stores the new value 10 and returns the old value 7.
        assertPrints(a.compareAndExchange(7, 10), "7")
        assertPrints(a.load(), "10")

        // Current value 10 is not equal to the expected value 2 ->
        // compareAndExchange fails, does not store the new value and returns the current value 10.
        assertPrints(a.compareAndExchange(2, 12), "10")
        assertPrints(a.load(), "10")
    }

    @Sample
    fun fetchAndAdd() {
        val a = AtomicInt(7)
        // Returns the old value before the addition.
        assertPrints(a.fetchAndAdd(10), "7")
        assertPrints(a.load(), "17")
    }

    @Sample
    fun addAndFetch() {
        val a = AtomicInt(7)
        // Returns the new value after the addition.
        assertPrints(a.addAndFetch(10), "17")
        assertPrints(a.load(), "17")
    }

    @Sample
    fun plusAssign() {
        val counter = AtomicInt(7)
        counter += 10
        assertPrints(counter.load(), "17")
    }

    @Sample
    fun minusAssign() {
        val counter = AtomicInt(7)
        counter -= 10
        assertPrints(counter.load(), "-3")
    }

    @Sample
    fun fetchAndIncrement() {
        val a = AtomicInt(7)
        assertPrints(a.fetchAndIncrement(), "7")
        assertPrints(a.load(), "8")
    }

    @Sample
    fun incrementAndFetch() {
        val a = AtomicInt(7)
        assertPrints(a.incrementAndFetch(), "8")
        assertPrints(a.load(), "8")
    }

    @Sample
    fun fetchAndDecrement() {
        val a = AtomicInt(7)
        assertPrints(a.fetchAndDecrement(), "7")
        assertPrints(a.load(), "6")
    }

    @Sample
    fun decrementAndFetch() {
        val a = AtomicInt(7)
        assertPrints(a.decrementAndFetch(), "6")
        assertPrints(a.load(), "6")
    }

    @Sample
    fun update() {
        val a = AtomicInt(7)
        a.update { currentValue ->
            when (currentValue % 2) {
                0 -> currentValue / 2
                else -> 3 * currentValue + 1
            }
        }
        assertPrints(a.load(), "22")
    }

    @Sample
    fun updateAndFetch() {
        val a = AtomicInt(7)
        val updatedValue = a.updateAndFetch { currentValue ->
            when (currentValue % 2) {
                0 -> currentValue / 2
                else -> 3 * currentValue + 1
            }
        }
        assertPrints(updatedValue, "22")
        assertPrints(a.load(), "22")
    }

    @Sample
    fun fetchAndUpdate() {
        val a = AtomicInt(7)
        val oldValue = a.updateAndFetch { currentValue ->
            when (currentValue % 2) {
                0 -> currentValue / 2
                else -> 3 * currentValue + 1
            }
        }
        assertPrints(oldValue, "7")
        assertPrints(a.load(), "22")
    }
}

@OptIn(ExperimentalAtomicApi::class)
class AtomicLong {
    @Sample
    fun load() {
        val a = AtomicLong(7)
        assertPrints(a.load(), "7")
    }

    @Sample
    fun store() {
        val a = AtomicLong(7)
        a.store(10)
        assertPrints(a.load(), "10")
    }

    @Sample
    fun exchange() {
        val a = AtomicLong(7)
        assertPrints(a.exchange(10), "7")
        assertPrints(a.load(), "10")
    }

    @Sample
    fun compareAndSet() {
        val a = AtomicLong(7)
        // Current value 7 is equal to the expected value 7 -> compareAndSet succeeds.
        assertPrints(a.compareAndSet(7, 10), "true")
        assertPrints(a.load(), "10")

        // Current value 10 is not equal to the expected value 2 -> compareAndSet fails.
        assertPrints(a.compareAndSet(2, 12), "false")
        assertPrints(a.load(), "10")
    }

    @Sample
    fun compareAndExchange() {
        val a = AtomicLong(7)
        // Current value 7 is equal to the expected value 7 ->
        // compareAndExchange succeeds, stores the new value 10 and returns the old value 7.
        assertPrints(a.compareAndExchange(7, 10), "7")
        assertPrints(a.load(), "10")

        // Current value 10 is not equal to the expected value 2 ->
        // compareAndExchange fails, does not store the new value and returns the current value 10.
        assertPrints(a.compareAndExchange(2, 12), "10")
        assertPrints(a.load(), "10")
    }

    @Sample
    fun fetchAndAdd() {
        val a = AtomicLong(7)
        // Returns the old value before the addition.
        assertPrints(a.fetchAndAdd(10), "7")
        assertPrints(a.load(), "17")
    }

    @Sample
    fun addAndFetch() {
        val a = AtomicLong(7)
        // Returns the new value after the addition.
        assertPrints(a.addAndFetch(10), "17")
        assertPrints(a.load(), "17")
    }

    @Sample
    fun plusAssign() {
        val counter = AtomicLong(7)
        counter += 10
        assertPrints(counter.load(), "17")
    }

    @Sample
    fun minusAssign() {
        val counter = AtomicLong(7)
        counter -= 10
        assertPrints(counter.load(), "-3")
    }

    @Sample
    fun fetchAndIncrement() {
        val a = AtomicLong(7)
        assertPrints(a.fetchAndIncrement(), "7")
        assertPrints(a.load(), "8")
    }

    @Sample
    fun incrementAndFetch() {
        val a = AtomicLong(7)
        assertPrints(a.incrementAndFetch(), "8")
        assertPrints(a.load(), "8")
    }

    @Sample
    fun fetchAndDecrement() {
        val a = AtomicLong(7)
        assertPrints(a.fetchAndDecrement(), "7")
        assertPrints(a.load(), "6")
    }

    @Sample
    fun decrementAndFetch() {
        val a = AtomicLong(7)
        assertPrints(a.decrementAndFetch(), "6")
        assertPrints(a.load(), "6")
    }


    @Sample
    fun update() {
        val a = AtomicLong(7L)
        a.update { currentValue ->
            when (currentValue % 2) {
                0L -> currentValue / 2L
                else -> 3 * currentValue + 1
            }
        }
        assertPrints(a.load(), "22")
    }

    @Sample
    fun updateAndFetch() {
        val a = AtomicLong(7L)
        val updatedValue = a.updateAndFetch { currentValue ->
            when (currentValue % 2) {
                0L -> currentValue / 2
                else -> 3 * currentValue + 1
            }
        }
        assertPrints(updatedValue, "22")
        assertPrints(a.load(), "22")
    }

    @Sample
    fun fetchAndUpdate() {
        val a = AtomicLong(7L)
        val oldValue = a.updateAndFetch { currentValue ->
            when (currentValue % 2) {
                0L -> currentValue / 2
                else -> 3 * currentValue + 1
            }
        }
        assertPrints(oldValue, "7")
        assertPrints(a.load(), "22")
    }
}

@OptIn(ExperimentalAtomicApi::class)
class AtomicBoolean {
    @Sample
    fun load() {
        val a = AtomicBoolean(true)
        assertPrints(a.load(), "true")
    }

    @Sample
    fun store() {
        val a = AtomicBoolean(true)
        a.store(false)
        assertPrints(a.load(), "false")
    }

    @Sample
    fun exchange() {
        val a = AtomicBoolean(true)
        assertPrints(a.exchange(false), "true")
        assertPrints(a.load(), "false")
    }

    @Sample
    fun compareAndSet() {
        val a = AtomicBoolean(true)
        // Current value true is equal to the expected value true -> compareAndSet succeeds.
        assertPrints(a.compareAndSet(true, false), "true")
        assertPrints(a.load(), "false")

        // Current value false is not equal to the expected value true -> compareAndSet fails.
        assertPrints(a.compareAndSet(true, false), "false")
        assertPrints(a.load(), "false")
    }

    @Sample
    fun compareAndExchange() {
        val a = AtomicBoolean(true)
        // Current value true is equal to the expected value true ->
        // compareAndExchange succeeds, stores the new value false and returns the old value true.
        assertPrints(a.compareAndExchange(true, false), "true")
        assertPrints(a.load(), "false")

        // Current value false is not equal to the expected value true ->
        // compareAndExchange fails, does not store the new value and returns the current value false.
        assertPrints(a.compareAndExchange(true, false), "false")
        assertPrints(a.load(), "false")
    }
}

@OptIn(ExperimentalAtomicApi::class)
class AtomicReference {
    @Sample
    fun load() {
        val a = AtomicReference("aaa")
        assertPrints(a.load(), "aaa")
    }

    @Sample
    fun store() {
        val a = AtomicReference("aaa")
        a.store("bbb")
        assertPrints(a.load(), "bbb")
    }

    @Sample
    fun exchange() {
        val a = AtomicReference("aaa")
        assertPrints(a.exchange("bbb"), "aaa")
        assertPrints(a.load(), "bbb")
    }

    @Sample
    fun compareAndSet() {
        val a = AtomicReference("aaa")
        // Current value "aaa" is equal to the expected value "aaa" -> compareAndSet succeeds.
        assertPrints(a.compareAndSet("aaa", "bbb"), "true")
        assertPrints(a.load(), "bbb")

        // Current value "bbb" is not equal to the expected value "aaa" -> compareAndSet fails.
        assertPrints(a.compareAndSet("aaa", "kkk"), "false")
        assertPrints(a.load(), "bbb")
    }

    @Sample
    fun compareAndExchange() {
        val a = AtomicReference("aaa")
        // Current value "aaa" is equal to the expected value "aaa" ->
        // compareAndExchange succeeds, stores the new value "bbb" and returns the old value "aaa".
        assertPrints(a.compareAndExchange("aaa", "bbb"), "aaa")
        assertPrints(a.load(), "bbb")

        // Current value "bbb" is not equal to the expected value "aaa" ->
        // compareAndExchange fails, does not store the new value and returns the current value "bbb".
        assertPrints(a.compareAndExchange("aaa", "kkk"), "bbb")
        assertPrints(a.load(), "bbb")
    }

    @Sample
    fun update() {
        data class Wallet(val owner: String, val balance: Long)

        val a = AtomicReference(Wallet("Kodee", 100_00L))
        a.update { wallet ->
            wallet.copy(balance = wallet.balance + 1_00) // Kodee got a buck!
        }
        assertPrints(a.load(), "Wallet(owner=Kodee, balance=10100)")
    }

    @Sample
    fun updateAndFetch() {
        data class Wallet(val owner: String, val balance: Long)

        val a = AtomicReference(Wallet("Kodee", 100_00L))
        val updatedWallet = a.updateAndFetch { wallet ->
            wallet.copy(balance = wallet.balance + 1_00) // Kodee got a buck!
        }
        assertPrints(updatedWallet, "Wallet(owner=Kodee, balance=10100)")
        assertPrints(a.load(), "Wallet(owner=Kodee, balance=10100)")
    }

    @Sample
    fun fetchAndUpdate() {
        data class Wallet(val owner: String, val balance: Long)

        val a = AtomicReference(Wallet("Kodee", 100_00L))
        val oldWallet = a.fetchAndUpdate { wallet ->
            wallet.copy(balance = wallet.balance + 1_00) // Kodee got a buck!
        }
        assertPrints(oldWallet, "Wallet(owner=Kodee, balance=10000)")
        assertPrints(a.load(), "Wallet(owner=Kodee, balance=10100)")
    }
}

@OptIn(ExperimentalAtomicApi::class)
class AtomicJvmSamples {
    @Sample
    fun processItems() {
        // The atomic counter of processed items
        val processedItems = AtomicInt(0)
        val totalItems = 100
        val items = List(totalItems) { "item$it" }
        // Split the items into chunks for processing by multiple threads
        val chunkSize = 20
        val itemChunks = items.chunked(chunkSize)
        val threads = itemChunks.map { chunk ->
            thread {
                for (item in chunk) {
                    println("Processing $item in thread ${Thread.currentThread()}")
                    // Increment the counter atomically
                    processedItems += 1
                }
            }
        }
        threads.forEach { it.join() }
        // The total number of processed items
        assertPrints(processedItems.load(), totalItems.toString())
    }
}
