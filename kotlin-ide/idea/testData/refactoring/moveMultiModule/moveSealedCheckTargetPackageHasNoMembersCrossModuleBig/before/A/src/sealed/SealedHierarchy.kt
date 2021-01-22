package sealed

sealed interface <caret>SealedInterfaceA
sealed interface SealedInterfaceB
sealed interface SealedInterfaceC
sealed interface SealedInterfaceD: InterfaceI, SealedInterfaceA, SealedInterfaceB, SealedInterfaceC
interface InterfaceE: SealedInterfaceD
sealed interface SealedInterfaceF: InterfaceE
sealed interface SealedInterfaceG
interface InterfaceH: InterfaceE
interface InterfaceI

class ClassA: InterfaceE
class ClassC
sealed class SealedClassB: SealedInterfaceB, SealedInterfaceC, ClassC()
class ClassD: SealedClassB()
sealed class SealedClassE: SealedClassB(), SealedInterfaceG
class ClassF: ClassD()
