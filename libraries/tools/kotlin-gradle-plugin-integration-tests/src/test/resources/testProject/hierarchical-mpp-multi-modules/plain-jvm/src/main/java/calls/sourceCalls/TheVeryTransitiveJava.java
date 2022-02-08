package calls.sourceCalls;

import calls.transitive_calls.sourceCalls.TheVeryTransitiveStory;
import transitiveStory.apiJvm.beginning.KotlinApiContainer;
import transitiveStory.apiJvm.jbeginning.JavaApiContainer;
import transitiveStory.bottomActual.apiCall.Jvm18JApiInheritor;
import transitiveStory.bottomActual.apiCall.Jvm18KApiInheritor;
import transitiveStory.bottomActual.intermediateSrc.InBottomActualIntermediate;
import transitiveStory.bottomActual.jApiCall.JApiCallerInJVM18;
import transitiveStory.bottomActual.mppBeginning.BottomActualDeclarations;
import transitiveStory.midActual.allTheCallsMirror.TheSameCallsButJava;
import transitiveStory.midActual.commonSource.SomeMPPInTheCommon;
import transitiveStory.midActual.sourceCalls.allTheCalls.INeedAllTheSourceSets;
import transitiveStory.midActual.sourceCalls.intemediateCall.SecondModCaller;
// import transitiveStory.bottomActual.mppBeginning.BottomActualDeclarations;
import static transitiveStory.bottomActual.mppBeginning.BottomActualDeclarationsKt.regularTLfunInTheBottomActualCommmon;
import static transitiveStory.midActual.commonSource.SomethinInTheCommonKt.regularTLfunInTheMidActualCommmon;

public class TheVeryTransitiveJava extends BottomActualDeclarations {
    // ========= api calls ==========
    // java
    JavaApiContainer javaApiContainer = new JavaApiContainer();

    // kotlin
    KotlinApiContainer kotlinApiContainer = new KotlinApiContainer();

    // ========= mpp-bottom-actual calls ==========
    // common source set
    String regularTLfunInTheBottomActualCall = regularTLfunInTheBottomActualCommmon("Some string from `plain-jvm` module, java source set");
    String inTheCompanionOfBottomActual = BottomActualDeclarations.Compainon.getInTheCompanionOfBottomActualDeclarations();

    // intermediate source set
    int badSimple = new BottomActualDeclarations().getSimpleVal();
    int jIntermBottomActualCall = new InBottomActualIntermediate().getP();

    // ========= jvm18 source set of `mpp-bottom-actual` ==========
    // java
    JApiCallerInJVM18 jApiCallerInJVM18 = new JApiCallerInJVM18();

    // kotlin
    Jvm18KApiInheritor jvm18KApiInheritor = new Jvm18KApiInheritor();
    Jvm18JApiInheritor jvm18JApiInheritor = new Jvm18JApiInheritor();

    // ========= mpp-mid-actual calls ==========
    // common source set
    String regTLfunInTeMidCommon = regularTLfunInTheMidActualCommmon("The message from `plain-jvm` module");
    int platformMppCall = new SomeMPPInTheCommon().getSimpleVal();

    // intermediate source set
    SecondModCaller secondModCaller = new SecondModCaller();

    // ========= jvmWithJava source set of `mpp-mid-actual` ==========
    // java
    TheSameCallsButJava theSameCallsButJava = new TheSameCallsButJava();

    // kotlin
    INeedAllTheSourceSets iNeedAllTheSourceSets = new INeedAllTheSourceSets();

    public static void main(String[] args) {
        System.out.println("Some basic printing from Java of `plain-jvm`: `" + new TheVeryTransitiveStory() + "`");
    }
}


