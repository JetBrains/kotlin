public typealias AsyncClosure = () async throws -> Swift.Void
public class AsyncMethods {
    public func asyncMethodEmptyArgs() async -> Swift.Bool {
        stub()
    }
    public func asyncMethod(
        value: Swift.Int32
    ) async -> Swift.Void {
        stub()
    }
}
public var asyncProperty: Swift.Int32 {
    get async {
        stub()
    }
}
public func asyncFunction(
    arg1: Swift.Int32
) async -> Any {
    stub()
}
public func asyncFunctionEmptyArgs() async -> Swift.Void {
    stub()
}
public func throwingAsyncFunction(
    arg1: Swift.Int32
) async throws -> Any {
    stub()
}
public func functionWithAsyncClosure(
    asyncEmptyClosure: () async throws -> Swift.Void
) -> Swift.Void {
    stub()
}
public func functionWithAsyncReturnClosure(
    asyncReturnClosure: (Swift.Int32) async throws -> Swift.Bool
) -> Swift.Void {
    stub()
}
public func functionConsumingClosureConsumingClosure(
    asyncReturnClosure: @escaping (@escaping (Swift.Int32) async throws -> Swift.Bool) async throws -> Swift.Bool,
    typealiasedClosure: @escaping Test.AsyncClosure
) -> (@escaping (Swift.Int32) async throws -> Swift.Bool) async throws -> Swift.Bool {
    stub()
}