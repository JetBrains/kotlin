interface UpperBound
interface UpperBound1 : UpperBound

abstract class ReturnA<A : UpperBound, B : A>(
    returnA: () -> A
)

abstract class ReturnB<A : UpperBound, B : A>(
    returnB: () -> B
)

class FooReturnA : ReturnA<UpperBound1, UpperBound1>(
    returnA = { null!! }
)

class FooReturnB : ReturnB<UpperBound1, UpperBound1>(
    returnB = { null!! }
)