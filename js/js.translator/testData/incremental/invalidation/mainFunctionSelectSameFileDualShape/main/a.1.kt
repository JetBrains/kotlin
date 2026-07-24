package a

fun main() {
    value.ok += "BAD0"
}

fun main(args: Array<String>) {
    if (args.isNotEmpty()) error("Expected empty args")
    value.ok += "A0"
}
