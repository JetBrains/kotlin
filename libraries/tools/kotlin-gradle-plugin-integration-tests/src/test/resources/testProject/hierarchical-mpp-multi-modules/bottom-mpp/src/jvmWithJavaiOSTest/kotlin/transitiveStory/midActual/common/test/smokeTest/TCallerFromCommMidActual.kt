package transitiveStory.midActual.common.test.smokeTest

// https://youtrack.jetbrains.com/issue/KT-33731
import transitiveStory.bottomActual.intermediateSrc.InBottomActualIntermediate
// import transitiveStory.bottomActual.intermediateSrc.IntermediateMPPClassInBottomActual

class TCallerFromCommMidActual {
    // ================ intermediate src of mpp-bottom-actual module =================
    internal var inBottomActualIntermediate =
        InBottomActualIntermediate()

/*    // ================ jvm18Main =================
    internal var intermediateMPPClassInBottomActual =
        IntermediateMPPClassInBottomActual()*/
}