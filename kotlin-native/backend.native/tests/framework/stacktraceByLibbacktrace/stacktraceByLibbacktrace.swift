import Foundation
import StacktraceByLibbacktrace

func testStackTrace() throws {
    let trace = StacktraceByLibbacktraceKt.getStackTrace()
    try assertTrue(trace[0].contains("Throwable.kt"))
    try assertTrue(trace[1].contains("Exceptions.kt"))
    try assertTrue(trace[2].contains("stacktraceByLibbacktrace.kt:7"))
    try assertTrue(trace[2].contains("[inlined]"))
    try assertTrue(trace[3].contains("stacktraceByLibbacktrace.kt:11"))
    try assertTrue(trace[4].contains("stacktraceByLibbacktrace.kt:16"))
    try assertTrue(trace[5].contains("<compiler-generated>"))
    try assertTrue(trace[6].contains("stacktraceByLibbacktrace.swift:5"))
    try assertTrue(trace[7].contains("main.swift:126"))
}

class StacktraceByLibbacktraceTests : TestProvider {
    var tests: [TestCase] = []

    init() {
        providers.append(self)
        tests = [
            TestCase(name: "Stacktrace", method: withAutorelease(testStackTrace)),
        ]
    }
}