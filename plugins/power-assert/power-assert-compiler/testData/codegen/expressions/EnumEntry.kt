fun box(): String = runAll(
    "test1" to { test1() },
)

enum class Planet {
    Mercury,
    Venus,
    Earth,
    Mars,
    Jupiter,
    Saturn,
    Uranus,
    Neptune,
    Pluto, // Still a planet in our hearts.
}

fun test1() {
    val actual = Planet.Pluto
    assert(actual == Planet.Earth)
}
