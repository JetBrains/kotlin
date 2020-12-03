import kotlinx.cli.*

fun main(args: Array<String>) {
    val argParser = ArgParser("test")
    val size by argParser.option(ArgType.Int, shortName = "s", description = "Required size of videoplayer window")
        .delimiter(",")
    val fileName by argParser.argument(ArgType.String, description = "File to play")
    argParser.parse(args)
}
