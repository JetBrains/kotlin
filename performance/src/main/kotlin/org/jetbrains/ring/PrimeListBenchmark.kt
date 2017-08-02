package org.jetbrains.ring

/**
 * This class tests linked list performance
 * using prime number calculation algorithms
 *
 * @author Mikhail Glukhikh
 */
open class PrimeListBenchmark {
    private var primes: MutableList<Int> = mutableListOf()

    //Benchmark
    fun calcDirect() {
        primes.clear()
        primes.add(2)
        var i = 3
        while (i <= BENCHMARK_SIZE) {
            var simple = true
            for (prime in primes) {
                if (prime * prime > i)
                    break
                if (i % prime == 0) {
                    simple = false
                    break
                }
            }
            if (simple)
                primes.add(i)
            i += 2
        }
    }

    //Benchmark
    fun calcEratosthenes() {
        primes.clear()
        primes.addAll(2..BENCHMARK_SIZE)
        var i = 0
        while (i < primes.size) {
            val divisor = primes[i]
            primes.removeAll { it -> it > divisor && it % divisor == 0 }
            i++
        }
    }
}