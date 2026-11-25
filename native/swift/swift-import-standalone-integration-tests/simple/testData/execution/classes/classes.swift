import Foundation

public class Counter: NSObject {
    private var count = 0

    public override init() {
        print("Counter.init")
    }

    public func increment() {
        count += 1
    }

    public func printCount() {
        print("count: \(count)")
    }
}

public class PureSwift {
    public init() {
        print("PureSwift.init")
    }

    public func hello() {
        print("PureSwift.hello")
    }
}
