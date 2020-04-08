// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtClass
// OPTIONS: derivedInterfaces
interface X {

}

open class <caret>A: X {

}

interface Y: X {

}
