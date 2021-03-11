@test.anno.EntryPoint
val entryPoint = ""

class Owner(@test.anno.EntryPoint val xx: Int) {
    @test.anno.EntryPoint val yy = 42
}

@test.anno.EntryPoint
class WithGetter {
    @get:test.anno.EntryPoint
    val zz = 13
}