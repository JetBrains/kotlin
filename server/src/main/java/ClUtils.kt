import com.martiansoftware.jsap.FlaggedOption
import com.martiansoftware.jsap.JSAP
import com.martiansoftware.jsap.StringParser
import com.martiansoftware.jsap.Switch

fun setClOptions(clParser: JSAP) {
    val optEmulator = Switch("emulator", 'e', "emulator")
    val optUseRandom = Switch("random", 'r', "random")
    val optSeedForRandom = getOption("seed", JSAP.LONG_PARSER, "1111", 's', "seed")
    val optTestRoom = getOption("test room", JSAP.STRING_PARSER, "", null, "room")
    val optHelp = getOption("help", JSAP.BOOLEAN_PARSER, "false", null, "help")

    clParser.registerParameter(optEmulator)
    clParser.registerParameter(optUseRandom)
    clParser.registerParameter(optSeedForRandom)
    clParser.registerParameter(optTestRoom)
    clParser.registerParameter(optHelp)
}

fun getOption(name: String, parser: StringParser, default: String, shortFlag: Char?, longFlag: String, required: Boolean = false): FlaggedOption {
    val result = FlaggedOption(name)
            .setStringParser(parser)
            .setDefault(default)
            .setRequired(required)
    if (shortFlag != null) {
        result.setShortFlag(shortFlag)
    }
    if (!longFlag.equals("")) {
        result.setLongFlag(longFlag)
    }

    return result
}
