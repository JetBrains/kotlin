/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import Foundation

public class Value: Hashable {
    public func hash(into hasher: inout Hasher) {
        return value.hash(into: &hasher)
    }
    
    public static func == (lhs: Value, rhs: Value) -> Bool {
        return lhs.value == rhs.value
    }
    
    public var hashValue: Int {
        return value.hashValue
    }

    var value: Int
    init(_ value: Int) {
        self.value = value
    }
    lazy var text = String(String(value).reversed())
}

public func intValues(_ size: Int) -> [Int] {
    return Array(1...size)
}

public func classValues(_ size: Int) -> [Value] {
    return intValues(size).map { Value($0) }
}

public func stringValues(_ size: Int) -> [String] {
    return intValues(size).map { String($0) }
}

public func filterLoad(_ v: Value) -> Bool {
    return v.text.contains(String(v.value))
}

public func mapLoad(_ v: Value) -> String {
    return String(v.text.reversed())
}

public func filterLoad(_ v: Int) -> Bool {
    return "0123456789".contains(String(v))
}

public func mapLoad(_ v: Int) -> String {
    return String(v)
}

public func filterSome(_ v: Int) -> Bool {
    return v % 7 == 0 || v % 11 == 0
}

public func filterPrime(_ v: Int) -> Bool {
    if (v <= 1) {
        return false
    }
    if (v <= 3) {
        return true
    }
    if (v % 2 == 0) {
        return false
    }
    var i = 3
    while (i*i <= v) {
        if (v % i == 0) {
            return false
        }
        i += 2
    }
    return true
}

extension Array where Element: Value {
    @inlinable func cnt(predicate: (Value) -> Bool) -> Int {
        var count = 0
        for element in self {
            if (predicate(element)) {
                count += 1
            }
        }
        return count
    }
}

extension Array where Element == Int {
    @inlinable func cnt(predicate: (Int) -> Bool) -> Int {
        var count = 0
        for element in self {
            if (predicate(element)) {
                count += 1
            }
        }
        return count
    }
}

extension Sequence where Element == Int {
    @inlinable func cnt(predicate: (Int) -> Bool) -> Int {
        var count = 0
        for element in self {
            if (predicate(element)) {
                count += 1
            }
        }
        return count
    }
}
