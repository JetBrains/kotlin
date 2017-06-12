// EXPECTED_REACHABLE_NODES: 505
package foo

class Data(val rawData: Array<Int>, val width: Int, val height: Int) {
    operator fun get(x: Int, y: Int): ColorLike {
        return object : ColorLike {
            override val red: Int = rawData[(y * width + x) * 4 + 0];
            override val green: Int = rawData[(y * width + x) * 4 + 1];
            override val blue: Int = rawData[(y * width + x) * 4 + 2];
        }
    }

    operator fun set(x: Int, y: Int, color: ColorLike) {
        rawData[(y * width + x) * 4 + 0] = color.red;
        rawData[(y * width + x) * 4 + 1] = color.green;
        rawData[(y * width + x) * 4 + 2] = color.blue;
    }

    fun each(block: (x: Int, y: Int) -> Unit) {
        for (x in 0..width - 1) {
            for (y in 0..height - 1) {
                block(x, y)
            }
        }
    }
}

class Color(r: Int, g: Int, b: Int) : ColorLike {
    override val red: Int = r
    override val green: Int = g
    override val blue: Int = b
}

interface ColorLike {
    val red: Int;
    val green: Int;
    val blue: Int;
}

fun box(): String {
    val d = Data(Array(4) { 0 }, 1, 1)
    if (d[0, 0].red != 0) {
        return "fail1"
    }
    if (d[0, 0].green != 0) {
        return "fail2"
    }
    if (d[0, 0].blue != 0) {
        return "fail3"
    }
    return "OK"
}