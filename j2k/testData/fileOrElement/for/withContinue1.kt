public class TestClass {
    companion object {
        public fun main(args: Array<String>) {
            run {
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
    }
}

fun main(args: Array<String>) = TestClass.main(args)
