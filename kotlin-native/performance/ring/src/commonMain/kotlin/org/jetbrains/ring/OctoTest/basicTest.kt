/**
 * Created by semoro on 07.07.17.
 */

import kotlinx.benchmark.*
import org.jetbrains.benchmarksLauncher.SkipWhenBaseOnly

@State(Scope.Benchmark)
@Measurement(time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class OctoTestHideName {
    @Benchmark
    fun OctoTest(bh: Blackhole) {
        val tree = OctoTree<Boolean>(4)
        val to = (2 shl tree.depth)

        var x = 0
        var y = 0
        var z = 0

        while (x < to) {
            y = 0
            while (y < to) {
                z = 0
                while (z < to) {
                    val c = (z + to * y + to * to * x) % 2 == 0

                    tree.set(x, y, z, c)
                    z++
                }
                y++
            }
            x++
        }

        var result = 0
        x = 0
        y = 0
        z = 0
        while (x < to) {
            y = 0
            while (y < to) {
                z = 0
                while (z < to) {
                    val c = (z + to * y + to * to * x) % 2 == 0

                    val res = tree.get(x, y, z)

                    if (res == c)
                        result += 1
                    z++
                }
                y++
            }
            x++
        }
        bh.consume(result)
    }
}
