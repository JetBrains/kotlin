package xxx

import xxx.E1.AAA1
import xxx.E2.*
import xxx.E3.AAA3
import xxx.E4.*

enum class E1 {
    AAA1, BBB1
}

enum class E2 {
    AAA2, BBB2
}

enum class E3 {
    AAA3, BBB3
}

enum class E4 {
    AAA4, BBB4
}

fun f() {
    print(AAA1)
    print(AAA2)
}
