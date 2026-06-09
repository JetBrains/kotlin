package z

fun main(args: Array<String>) {
    if (args.isNotEmpty()) error("Expected empty args")
    value.ok += "Z1"
}
