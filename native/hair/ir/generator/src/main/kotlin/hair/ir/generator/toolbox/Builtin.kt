package hair.ir.generator.toolbox

object Builtin : ModelDSL() {

    val controlFlow by nodeInterface(builtin = true)

    val controlled by nodeInterface(controlFlow, builtin = true)

    val controlling by abstractClass(builtin = true) {
        interfaces(controlFlow)
    }

    val controlMerge by abstractClass(controlling, builtin = true)

    val spinal by abstractClass(controlling, builtin = true)

    val blockEnd by abstractClass(builtin = true) {
        interfaces(controlled)
    }

    val noExit by abstractClass(blockEnd, builtin = true)

    val singleExit by abstractClass(blockEnd, builtin = true)

    val twoExits by abstractClass(blockEnd, builtin = true)

//
    val throwing by nodeInterface(controlled, builtin = true)

    val throwExit by abstractClass(noExit, builtin = true) {
        interfaces(throwing)
    }

    val throwingSpinal by abstractClass(spinal, builtin = true) {
        interfaces(throwing)
    }

//

    val projection by nodeInterface(builtin = true) {
        param("owner")
    }
}