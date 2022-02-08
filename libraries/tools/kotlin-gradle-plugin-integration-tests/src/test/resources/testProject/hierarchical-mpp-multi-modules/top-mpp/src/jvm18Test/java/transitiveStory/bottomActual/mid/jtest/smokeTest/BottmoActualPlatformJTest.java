package transitiveStory.bottomActual.mid.jtest.smokeTest;

import transitiveStory.apiJvm.beginning.KotlinApiContainer;
import transitiveStory.apiJvm.jbeginning.JavaApiContainer;
import transitiveStory.bottomActual.intermediateSrc.InBottomActualIntermediate;
import transitiveStory.bottomActual.intermediateSrc.IntermediateMPPClassInBottomActual;

public class BottmoActualPlatformJTest {
    // ================ api =================
    // java
    // JClassForTheSmokeTestFromApi jClassForTheSmokeTestFromApi = new JClassForTheSmokeTestFromApi();
    JavaApiContainer javaApiContainer = new JavaApiContainer();

    // kotlin
    // ForTheSmokeTestFromApi forTheSmokeTestFromApi = new ForTheSmokeTestFromApi();
    KotlinApiContainer kotlinApiContainer = new KotlinApiContainer();

    // ================ intermediate src of this module =================
    InBottomActualIntermediate inBottomActualIntermediate = new InBottomActualIntermediate();

    // ================ jvm18Main =================
    IntermediateMPPClassInBottomActual intermediateMPPClassInBottomActual = new IntermediateMPPClassInBottomActual();
}
