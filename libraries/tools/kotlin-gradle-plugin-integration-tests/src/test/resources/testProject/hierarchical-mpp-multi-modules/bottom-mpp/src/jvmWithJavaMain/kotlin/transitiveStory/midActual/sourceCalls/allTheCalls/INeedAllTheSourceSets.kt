package transitiveStory.midActual.sourceCalls.allTheCalls


import transitiveStory.apiJvm.beginning.KotlinApiContainer
import transitiveStory.apiJvm.jbeginning.JavaApiContainer
import transitiveStory.bottomActual.apiCall.Jvm18JApiInheritor
import transitiveStory.bottomActual.apiCall.Jvm18KApiInheritor
import transitiveStory.bottomActual.mppBeginning.BottomActualDeclarations
import transitiveStory.bottomActual.mppBeginning.regularTLfunInTheBottomActualCommmon
import transitiveStory.bottomActual.intermediateSrc.*
import transitiveStory.bottomActual.jApiCall.JApiCallerInJVM18

class INeedAllTheSourceSets {
    // ========= api calls ==========
    // java
    val jApiOne = JavaApiContainer()

    // kotlin
    val kApiOne = KotlinApiContainer()

    // ========= mpp-bottom-actual calls ==========
    // common source set
    val interCallOne = regularTLfunInTheBottomActualCommmon("Some string from `mpp-mid-actual` module")
    val interCallTwo = BottomActualDeclarations.inTheCompanionOfBottomActualDeclarations
    val interCallThree = BottomActualDeclarations().simpleVal

    // intermediate source set
    val interCallFour = InBottomActualIntermediate().p
    val interCallFive = IntermediateMPPClassInBottomActual()

    // ========= jvm18 source set (attempt to) ==========
    // java
    val interCallSix = JApiCallerInJVM18()

    // kotlin
    val interCallSeven = Jvm18KApiInheritor()
    val interCallEight = Jvm18JApiInheritor()
    val interCallNine = IntermediateMPPClassInBottomActual()
}


// experiments with intermod inheritance
class BottomActualCommonInheritorInJVM : BottomActualDeclarations()
