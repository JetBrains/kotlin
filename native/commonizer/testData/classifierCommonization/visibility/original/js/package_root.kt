public class A1
public class A2
public class A3
public class A4

protected class B1
protected class B2
protected class B3

internal class C1
internal class C2

private class D1

public typealias E1 = A1
public typealias E2 = A1
public typealias E3 = A1
public typealias E4 = A1

protected typealias F1 = A1
protected typealias F2 = A1
protected typealias F3 = A1

internal typealias G1 = A1
internal typealias G2 = A1

private typealias H1 = A1

public typealias I1 = A1 // points to public
public typealias I2 = B1 // points to protected
public typealias I3 = C1 // points to internal
public typealias I4 = D1 // points to private
