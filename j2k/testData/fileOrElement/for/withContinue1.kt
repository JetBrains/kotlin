import kotlin.platform.platformStatic

public object TestClass {
    platformStatic public fun main(args: Array<String>) {
        var i = 0
        while (i < 10) {
            if (i == 4 || i == 8) {
                i++
                ++i
                continue
            }
            System.err.println(i)
            ++i
        }
    }
}