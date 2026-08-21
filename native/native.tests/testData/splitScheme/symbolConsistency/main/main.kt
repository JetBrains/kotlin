import kotlin.test.*

@Test
fun useProbeFromLibrary() {
    val probe = SplitSchemeProbe()
    println(probe.describe())
    println(probe.compute())
    println(topLevelProbeEntry())
    assertTrue(true)
}
