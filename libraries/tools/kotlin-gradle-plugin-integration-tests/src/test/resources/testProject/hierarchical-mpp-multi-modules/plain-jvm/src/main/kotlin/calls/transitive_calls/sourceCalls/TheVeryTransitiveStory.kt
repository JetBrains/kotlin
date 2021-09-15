package calls.transitive_calls.sourceCalls

import transitiveStory.apiJvm.beginning.KotlinApiContainer
import transitiveStory.apiJvm.jbeginning.JavaApiContainer
import transitiveStory.bottomActual.apiCall.Jvm18JApiInheritor
import transitiveStory.bottomActual.apiCall.Jvm18KApiInheritor
import transitiveStory.bottomActual.intermediateSrc.InBottomActualIntermediate
import transitiveStory.bottomActual.intermediateSrc.IntermediateMPPClassInBottomActual
import transitiveStory.bottomActual.jApiCall.JApiCallerInJVM18
import transitiveStory.bottomActual.mppBeginning.BottomActualDeclarations
import transitiveStory.bottomActual.mppBeginning.regularTLfunInTheBottomActualCommmon
import transitiveStory.midActual.allTheCallsMirror.TheSameCallsButJava
import transitiveStory.midActual.commonSource.SomeMPPInTheCommon
import transitiveStory.midActual.commonSource.regularTLfunInTheMidActualCommmon
import transitiveStory.midActual.sourceCalls.allTheCalls.INeedAllTheSourceSets
import transitiveStory.midActual.sourceCalls.intemediateCall.SecondModCaller

class TheVeryTransitiveStory {
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

    // ========= jvm18 source set of `mpp-bottom-actual` ==========
    // java
    val interCallSix = JApiCallerInJVM18()

    // kotlin
    val interCallSeven = Jvm18KApiInheritor()
    val interCallEight = Jvm18JApiInheritor()
    val interCallNine = IntermediateMPPClassInBottomActual()

    // ========= mpp-mid-actual calls ==========
    // common source set
    val midCommonCallOne = regularTLfunInTheMidActualCommmon("The message from `plain-jvm` module")
    val midCommonCallTwo = SomeMPPInTheCommon().simpleVal

    // intermediate source set
    val midIntermediateCall = SecondModCaller()
    class TransitiveInheritor : BottomActualDeclarations()

    // ========= jvmWithJava source set of `mpp-mid-actual` ==========
    // java
    val midEndCallOne = TheSameCallsButJava()

    // kotlin
    val midEndCallTwo = INeedAllTheSourceSets()

}

fun main() {
    val arg = TheVeryTransitiveStory()
    println("Test printing: `${arg.jApiOne}`; \n `${arg.kApiOne}`")
}

class SomeWComp {
    companion object {
        val callMe = "sfjn"
    }
}