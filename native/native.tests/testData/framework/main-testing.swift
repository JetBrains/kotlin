/*
 * Copyright 2010-2025 JetBrains s.r.o.
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
import Testing

@preconcurrency
@main
struct TestHarness {
    static func main() async throws {
        // NOTE: this follows swift-testing/Sources/Testing/ABI/EntryPoints/ABIEntryPoint.swift
        
        typealias EntryPoint = @convention(thin) @Sendable (
            _ configurationJSON: UnsafeRawBufferPointer?,
            _ recordHandler: @escaping @Sendable (_ recordJSON: UnsafeRawBufferPointer) -> Void
        ) async throws -> Bool

        // RTLD_DEFAULT defined as a macro in Darwin and not properly imported to swift
        let RTLD_DEFAULT = UnsafeMutableRawPointer(bitPattern: -2)

        let swiftTestingMainGetter = dlsym(RTLD_DEFAULT, "swt_abiv0_getEntryPoint").map {
            unsafeBitCast($0, to: (@convention(c) () -> UnsafeRawPointer).self)
        }

        guard let swiftTestingMainGetter else {
            let displayError = dlerror().flatMap { String.init(validatingUTF8: $0) } ?? "no dlerror reported"
            fatalError("Could not find swift-testing abi getter: \(displayError)")
        }

        let swiftTestingMain = unsafeBitCast(swiftTestingMainGetter(), to: EntryPoint.self)

        // NOTE: we disable parallelization here since the current set of tests in /execution was written with no concurrency in mind
        let config = """
            {
                "verbosity": 2,
                "parallel": false
            }
            """.data(using: .utf8)!

        let configBytes = UnsafeMutableBufferPointer<UInt8>.allocate(capacity: config.count)
        _ = config.copyBytes(to: configBytes)
        nonisolated(unsafe) let configPtr = UnsafeRawBufferPointer(configBytes)

        let hadSucceeded = try await swiftTestingMain(configPtr) { event in
            guard let e = event.baseAddress, let json = String(utf8String: e.assumingMemoryBound(to: CChar.self)) else { return }
            _ = json // NOTE: swift-testing reports progress events here in a stable json format.
        }

        exit(hadSucceeded ? 0 : 1)
    }
}