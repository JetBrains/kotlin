import kotlinx.cli.*

fun main(args: Array<String>) {
    val argParser = ArgParser("test")
    val mode by argParser.option(
        ArgType.Choice(listOf("video", "audio", "both")), shortName = "m", description = "Play mode")
        .default("both")
    val size by argParser.option(ArgType.Int, shortName = "s", description = "Required size of videoplayer window")
        .delimiter(",")
    val fileName by argParser.argument(ArgType.String, description = "File to play")
    argParser.parse(args)
}
