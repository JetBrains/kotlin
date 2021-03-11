package foo

public fun accessM3() {
    <error>accessM1</error>()
    accessM2()
    accessM3()
    <error>accessM4</error>()
}