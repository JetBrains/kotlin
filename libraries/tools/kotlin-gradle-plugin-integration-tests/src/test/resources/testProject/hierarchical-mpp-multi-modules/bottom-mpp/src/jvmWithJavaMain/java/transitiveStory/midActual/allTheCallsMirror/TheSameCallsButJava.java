package transitiveStory.midActual.allTheCallsMirror;

import transitiveStory.apiJvm.beginning.KotlinApiContainer;
import transitiveStory.apiJvm.jbeginning.JavaApiContainer;
import transitiveStory.bottomActual.apiCall.Jvm18KApiInheritor;
import transitiveStory.bottomActual.intermediateSrc.IntermediateMPPClassInBottomActual;
import transitiveStory.bottomActual.jApiCall.JApiCallerInJVM18;
import transitiveStory.bottomActual.mppBeginning.BottomActualDeclarations;

import static transitiveStory.bottomActual.mppBeginning.BottomActualDeclarationsKt.regularTLfunInTheBottomActualCommmon;

public class TheSameCallsButJava {
    // ========= api calls ==========
    // java
    JavaApiContainer jApiOne = new JavaApiContainer();

    // kotlin
    KotlinApiContainer kApiOne = new KotlinApiContainer();

    // ========= mpp-bottom-actual calls ==========
    // common source set
    String interCallOne = regularTLfunInTheBottomActualCommmon("Some string from `mpp-mid-actual` module");
    // String interCallTwo = inTheCompanionOfBottomActualDeclarations;
    String some = regularTLfunInTheBottomActualCommmon("");

    // intermediate source set
    // https://youtrack.jetbrains.com/issue/KT-33733
    BottomActualDeclarations interCallThree = new BottomActualDeclarations();

    // ========= jvm18 source set (attempt to) ==========
    // java
    JApiCallerInJVM18 jApiCallerInJVM18 = new JApiCallerInJVM18();

    // kotlin
    Jvm18KApiInheritor jvm18KApiInheritor = new Jvm18KApiInheritor();
    IntermediateMPPClassInBottomActual intermediateMPPClassInBottomActual = new IntermediateMPPClassInBottomActual();
}

/*
// experiments with intermod inheritance
class BottomActualCommonInheritorInJVM : BottomActualDeclarations()
*/