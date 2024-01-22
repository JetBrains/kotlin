/*
 * Copyright 2010-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import Foundation

enum TestError : Error {
    case assertFailed(String)
    case failure
    case testsFailed([String])
}

// ---------------- Assertions ----------------

private func throwAssertFailed(message: String, file: String, line: Int) throws -> Never {
    throw TestError.assertFailed("\(file):\(line): \(message)")
}

func assertEquals<T: Equatable>(actual: T, expected: T,
                                _ message: String = "Assertion failed:",
                                file: String = #file, line: Int = #line) throws {
    if (actual != expected) {
        try throwAssertFailed(message: message + " Expected value: \(expected), but got: \(actual)",
                file: file, line: line)
    }
}

func assertSame(actual: AnyObject?, expected: AnyObject?,
                _ message: String = "Assertion failed:",
                file: String = #file, line: Int = #line) throws {
    if (actual !== expected) {
        try throwAssertFailed(message: message + " Expected value: \(expected), but got: \(actual)",
                file: file, line: line)
    }
}

func assertEquals<T: Equatable>(actual: [T], expected: [T],
                                _ message: String = "Assertion failed: arrays not equal",
                                file: String = #file, line: Int = #line) throws {
    try assertEquals(actual: actual.count, expected: expected.count, "Size differs", file: file, line: line)
    try assertTrue(actual.elementsEqual(expected), "Arrays elements are not equal", file: file, line: line)
}

func assertTrue(_ value: Bool,
                _ message: String = "Assertion failed:",
                file: String = #file, line: Int = #line) throws {
    if (value != true) {
        try throwAssertFailed(message: message + " Expected value to be TRUE, but got: \(value)",
                file: file, line: line)
    }
}

func assertFalse(_ value: Bool,
                 _ message: String = "Assertion failed:",
                 file: String = #file, line: Int = #line) throws {
    if (value != false) {
        try throwAssertFailed(message: message + " Expected value to be FALSE, but got: \(value)",
                file: file, line: line)
    }
}

func assertNil(_ value: Any?,
                 _ message: String = "Assertion failed:",
                 file: String = #file, line: Int = #line) throws {
    if (value != nil) {
        try throwAssertFailed(message: message + " Expected value to be nil, but got: \(value!)",
                file: file, line: line)
    }
}

func fail(_ message: String = "Should not reach here", file: String = #file, line: Int = #line) throws -> Never {
    try throwAssertFailed(message: message, file: file, line: line)
}

func assertFailsWith<T : Error>(_ errorType: T.Type,
                                _ message: String = "Assertion failed:",
                                file: String = #file, line: Int = #line,
                                block: () throws -> Void) throws {
    do {
        try block()
    } catch let error {
        if error is T { return }
        try throwAssertFailed(message: message + " Expected error \(errorType), got \(error)",
                file: file, line: line)
    }

    try throwAssertFailed(message: message + " Expected error \(errorType), but finished successfully",
            file: file, line: line)
}

func assertFailsWithKotlin<T>(_ exceptionType: T.Type,
                              _ message: String = "Assertion failed:",
                              file: String = #file, line: Int = #line,
                              block: () throws -> Void) throws {
    do {
        try block()
    } catch let error {
        let kotlinException = error.kotlinException
        if kotlinException is T { return }
        let got = kotlinException ?? error
        try throwAssertFailed(message: message + " Expected Kotlin exception \(exceptionType), got \(got)",
                file: file, line: line)
    }

    try throwAssertFailed(message: message + " Expected Kotlin exception \(exceptionType), but finished successfully",
            file: file, line: line)
}


// ---------------- Utils --------------------

func withAutorelease( _ method: @escaping () throws -> Void) -> () throws -> Void {
    return { () throws -> Void in
        try autoreleasepool { try method() }
    }
}

extension Error {
    var kotlinException: Any? {
        get {
            return (self as NSError).userInfo["KotlinException"]
        }
    }
}

// ---------------- Execution ----------------

private final class Statistics : CustomStringConvertible {
    var passed: [String] = []
    var failed: [String] = []

    var description: String {
        return """
        ---- RESULTS:
         PASSED: \(passed.count)
         FAILED: \(failed.count)
        """
    }

    static let instance = Statistics()

    static func getInstance() -> Statistics {
        return instance
    }

    func start(_ name: String) {
        print("---- Starting test: \(name)")
    }

    func passed(_ name: String) {
        print("---- PASSED test: \(name)")
        passed.append(name)
    }

    func failed(_ name: String, error: Error) {
        print("---- FAILED test: \(name) with error: \(error)")
        failed.append(name)
    }
}
/**
 *  TestCase represents a single test
 */
struct TestCase {
    let name: String
    let method: () throws -> Void

    init(name: String, method: @escaping () throws -> Void) {
        self.name = name
        self.method = method
    }

    func run() {
        let stats = Statistics.getInstance()
        stats.start(name)
        do {
            try method()
            stats.passed(name)
        } catch {
            stats.failed(name, error: error)
        }
    }
}

protocol TestProvider {
    var tests: [TestCase] { get }
}

class SimpleTestProvider : TestProvider {
    var tests: [TestCase] = []

    init() {
        providers.append(self)
    }

    func test(_ name: String, _ method: @escaping () throws -> Void) {
        tests.append(TestCase(name: name, method: withAutorelease(method)))
    }
}

var providers: [TestProvider] = []

private func execute(tests: [TestCase]) {
    for test in tests {
        test.run()
    }
}

/**
 * Entry point of the test
 */
private func main() {
    // Generated method that instantiates test providers.
    registerProviders()

    let stats = Statistics.getInstance()
    for pr in providers {
        let name = String(describing: type(of: pr))
        print("-- \(name) started")
        execute(tests: pr.tests)
        print("-- \(name) finished")
    }
    print(stats)

    let failed = stats.failed
    if !failed.isEmpty {
        print()
        print("Tests failed:")
        for testName in failed {
            print(":: \(testName)")
        }
        abort()
    }
}

main()