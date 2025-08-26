public expect class A1()
expect class A2()
expect class B1()
internal expect class C1()

public typealias E1 = A1
typealias E2 = A1
internal typealias E3 = A1

public expect class E4()

typealias F1 = A1
internal typealias F2 = A1

public expect class F3()

internal typealias G1 = A1

public expect class G2()

public expect class H1()

public typealias I1 = A1
public typealias I2 = B1
internal typealias I3 = C1
