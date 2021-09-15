package transitiveStory.bottomActual.apiCall

import playground.moduleName
import transitiveStory.apiJvm.beginning.KotlinApiContainer
import transitiveStory.apiJvm.jbeginning.JavaApiContainer

open class Jvm18JApiInheritor : JavaApiContainer() {
    // override var protectedJavaDeclaration = ""
    var callProtectedJavaDeclaration = protectedJavaDeclaration
}

open class Jvm18KApiInheritor : KotlinApiContainer() {
    public override val protectedKotlinDeclaration =
        "I'm an overridden Kotlin string in `$this` from `" + moduleName +
                "` and shall be never visible to the other modules except my subclasses."
}

/**
 * Some class which type is lately used in the function.
 *
 */
open class FindMyDocumantationPlease

/**
 * A function using a class type placed right into the same file.
 *
 * @param f The parameter of the type under the investigation
 * */
fun iWantSomeDocumentationFromDokka(f: FindMyDocumantationPlease) {}

fun bottActualApiCaller(k: KotlinApiContainer, s: JavaApiContainer, ij: Jvm18JApiInheritor, ik: Jvm18KApiInheritor) {
    // val first = k.privateKotlinDeclaration
    // val second = k.packageVisibleKotlinDeclaration
    // val third = k.protectedKotlinDeclaration
    val fourth = ik.protectedKotlinDeclaration
    val fifth = k.publicKotlinDeclaration
    val sixth = KotlinApiContainer.publicStaticKotlinDeclaration

    // val seventh = s.privateJavaDeclaration
    // val eighth = s.packageVisibleJavaDeclaration
    val ninth = s.publicJavaDeclaration
    val tenth = JavaApiContainer.publicStaticJavaDeclaration
    // val eleventh = ij.protectedJavaDeclaration
}