/**
 * Created by semoro on 07.07.17.
 */

import org.jetbrains.benchmarksLauncher.assert

fun octoTest() {
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

                assert(res == c)
                z++
            }
            y++
        }
        x++
    }
}
