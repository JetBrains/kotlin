package transitiveStory.bottomActual.jApiCall;

import transitiveStory.apiJvm.beginning.KotlinApiContainer;
import transitiveStory.apiJvm.jbeginning.JavaApiContainer;

public class JApiCallerInJVM18 extends JavaApiContainer {
    String protectedCall = protectedJavaDeclaration;
    KotlinApiContainer kotlinApiContainer = new KotlinApiContainer();

    void caller(JavaApiContainer j) {
        String s1 = j.publicJavaDeclaration;
        String s2 = publicStaticJavaDeclaration;
        String s3 = kotlinApiContainer.getPublicKotlinDeclaration();
        //String s4 = kotlinApiContainer.getPackageVisibleKotlinDeclaration$api_jvm();
    }
}