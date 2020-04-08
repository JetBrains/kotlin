// WITH_RUNTIME

data class Package(val name: String, val version: String, val source: String, val id: String)

val pkgs = listOf<Package>().mapIndexed { i, <caret>p -> p.id to i }.toMap()