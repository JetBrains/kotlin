class Planet(val name: String, val diameter: Double)

expect val intProperty: Int
expect val Int.intProperty: Int
expect val Short.intProperty: Int
expect val Long.intProperty: Int
expect val String.intProperty: Int
expect val Planet.intProperty: Int

expect fun intFunction(): Int
expect fun Int.intFunction(): Int
expect fun Short.intFunction(): Int
expect fun Long.intFunction(): Int
expect fun String.intFunction(): Int
expect fun Planet.intFunction(): Int
