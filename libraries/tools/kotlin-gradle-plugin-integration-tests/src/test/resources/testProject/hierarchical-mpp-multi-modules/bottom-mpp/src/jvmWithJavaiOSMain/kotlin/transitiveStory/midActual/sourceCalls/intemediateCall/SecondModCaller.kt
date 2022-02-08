package transitiveStory.midActual.sourceCalls.intemediateCall

import transitiveStory.bottomActual.mppBeginning.BottomActualDeclarations
import transitiveStory.bottomActual.mppBeginning.regularTLfunInTheBottomActualCommmon

// https://youtrack.jetbrains.com/issue/KT-33731
import transitiveStory.bottomActual.intermediateSrc.*

class SecondModCaller {
    // ========= mpp-bottom-actual calls ==========
    // common source set
    val interCallOne = regularTLfunInTheBottomActualCommmon("Some string from `mpp-mid-actual` module")
    val interCallTwo = BottomActualDeclarations.inTheCompanionOfBottomActualDeclarations
    val interCallThree = BottomActualDeclarations().simpleVal

    // https://youtrack.jetbrains.com/issue/KT-33731
    // intermediate source set
    val interCallFour = InBottomActualIntermediate().p
    val interCallFive = IntermediateMPPClassInBottomActual()

    // kotlin
    val interCallNine = IntermediateMPPClassInBottomActual()
}


// experiments with intermod inheritance
class BottomActualCommonInheritor : BottomActualDeclarations()
expect class BottomActualMPPInheritor : BottomActualDeclarations
