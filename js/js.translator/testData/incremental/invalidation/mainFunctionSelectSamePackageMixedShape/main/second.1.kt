package p

private fun main(args: Array<String>) {
    if (args.isNotEmpty()) error("Expected empty args")
    value.ok += "P2"
}
