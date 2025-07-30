import Foundation
import Stacktrace

func testStackTrace() throws {
    let trace = StacktraceKt.getStackTrace()
    print(trace)
    try assertTrue(trace[0].contains("Throwable.kt"))
    try assertTrue(trace[1].contains("Exceptions.kt"))
    try assertTrue(trace[2].contains("stacktrace.kt:8"))
    try assertTrue(trace[3].contains("stacktrace.kt:13"))
    try assertTrue(trace[4].contains("<compiler-generated>"))
    try assertTrue(trace[5].contains("stacktrace.swift:5"))
    try assertTrue(trace[6].contains("main.swift"))
}

class StacktraceTests : TestProvider {
    var tests: [TestCase] = []

    init() {
        providers.append(self)
        tests = [
            TestCase(name: "Stacktrace", method: withAutorelease(testStackTrace)),
        ]
    }
}