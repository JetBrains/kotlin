actual public class A1 actual constructor()
public class A2
public class A3
public class A4

actual protected class B1 actual constructor()
protected class B2
protected class B3

actual internal class C1 actual constructor()
internal class C2

private class D1

actual public typealias E1 = A1
actual public typealias E2 = A1
actual public typealias E3 = A1
actual public typealias E4 = A1

actual protected typealias F1 = A1
protected typealias F2 = A1
actual protected typealias F3 = A1

actual internal typealias G1 = A1
actual internal typealias G2 = A1

actual private typealias H1 = A1

actual public typealias I1 = A1 // points to public
actual public typealias I2 = B1 // points to protected
actual public typealias I3 = C1 // points to internal
public typealias I4 = D1 // points to private
