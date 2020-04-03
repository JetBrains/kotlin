actual public class A1 actual constructor()
protected class A2
internal class A3
private class A4

actual protected class B1 actual constructor()
internal class B2
private class B3

actual internal class C1 actual constructor()
private class C2

private class D1

actual public typealias E1 = A1
actual protected typealias E2 = A1
actual internal typealias E3 = A1
actual private typealias E4 = A1

actual protected typealias F1 = A1
internal typealias F2 = A1
actual private typealias F3 = A1

actual internal typealias G1 = A1
actual private typealias G2 = A1

actual private typealias H1 = A1

actual public typealias I1 = A1 // points to public
actual public typealias I2 = B1 // points to protected
actual public typealias I3 = C1 // points to internal
public typealias I4 = D1 // points to private
