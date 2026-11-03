// LANGUAGE: +CompanionBlocks +CompanionExtensions
// WITH_STDLIB

// Constants of companion blocks and companion extensions used as annotation arguments must be evaluated in the stubs.

annotation class Tag(val name: String, val number: Int)

class Constants {
    companion {
        const val NAME = "block"
        const val NUMBER = 40
    }
}

interface InterfaceConstants {
    companion {
        const val NAME = "interface"
    }
}

companion const val Constants.EXT_NUMBER: Int = 2

@Tag(Constants.NAME, Constants.EXT_NUMBER)
class UsesConstants

@Tag(InterfaceConstants.NAME + "-" + Constants.NAME, Constants.NUMBER + Constants.EXT_NUMBER)
class UsesExpressions

class Members {
    companion {
        @Tag(Constants.NAME, Constants.NUMBER)
        fun onBlockMember() = 1
    }
}
