package foo

public fun accessM1() {
    accessM1()
    accessM2()
    <error>accessM3</error>()
    <error>accessM4</error>()
}