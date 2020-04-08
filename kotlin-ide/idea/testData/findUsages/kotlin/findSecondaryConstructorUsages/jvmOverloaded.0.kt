// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtSecondaryConstructor
// OPTIONS: usages
public class A {
    @JvmOverloads <caret>constructor(x: Int = 0, y: Double = 0.0, z: String = "0")
}