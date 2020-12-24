// COMPILER_ARGUMENTS: -XXLanguage:+SealedInterfaces -XXLanguage:+AllowSealedInheritorsInDifferentFilesOfSamePackage
package sealed.otherpackage
import sealed.SealedDeclarationInterface
import sealed.SealedDeclarationClass

class D: <error descr="[SEALED_INHERITOR_IN_DIFFERENT_PACKAGE] Inheritor of sealed class or interface declared in package sealed.otherpackage but it must be in package sealed where base class is declared">SealedDeclarationInterface</error>
class DClass: <error descr="[SEALED_INHERITOR_IN_DIFFERENT_PACKAGE] Inheritor of sealed class or interface declared in package sealed.otherpackage but it must be in package sealed where base class is declared">SealedDeclarationClass</error>()
