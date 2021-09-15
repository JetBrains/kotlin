package transitiveStory.bottomActual.mid.jtest.smokeTest

import transitiveStory.apiJvm.beginning.KotlinApiContainer
import transitiveStory.apiJvm.jbeginning.JavaApiContainer
import transitiveStory.bottomActual.intermediateSrc.InBottomActualIntermediate
import transitiveStory.bottomActual.intermediateSrc.IntermediateMPPClassInBottomActual

class BottmoActualPlatformKTest {
    // ================ api =================
    // java
    // JClassForTheSmokeTestFromApi jClassForTheSmokeTestFromApi = new JClassForTheSmokeTestFromApi();
    internal var javaApiContainer =
        JavaApiContainer()

    // kotlin
    // ForTheSmokeTestFromApi forTheSmokeTestFromApi = new ForTheSmokeTestFromApi();
    internal var kotlinApiContainer =
        KotlinApiContainer()

    // ================ intermediate src of this module =================
    internal var inBottomActualIntermediate =
        InBottomActualIntermediate()

    // ================ jvm18Main =================
    internal var intermediateMPPClassInBottomActual =
        IntermediateMPPClassInBottomActual()
}