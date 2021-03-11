package test

fun main() {
    val seq = sequence {
        //Breakpoint!
        yield(
            sequence {
                yield(1)
                yield(2)
                yield(3)
            }
        )

        yield(
            sequence {
                yield(4)
                yield(5)
                yield(6)
            }
        )
    }

    seq.map { it.toList() }.flatten().toList()
}

// STEP_OVER: 10