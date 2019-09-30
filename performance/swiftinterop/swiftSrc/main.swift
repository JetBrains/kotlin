/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import Foundation
import benchmark

var runner = BenchmarksRunner()
let args = KotlinArray(size: Int32(CommandLine.arguments.count - 1), init: {index in
    CommandLine.arguments[Int(truncating: index) + 1]
})

let companion = BenchmarkEntryWithInit.Companion()

var swiftLauncher = SwiftLauncher()
swiftLauncher.add(name: "createMultigraphOfInt", benchmark: companion.create(ctor: { return SwiftInteropBenchmarks() },
        lambda: { ($0 as! SwiftInteropBenchmarks).createMultigraphOfInt() }))
    swiftLauncher.add(name: "fillCityMap", benchmark: companion.create(ctor: { return SwiftInteropBenchmarks() },
        lambda: { ($0 as! SwiftInteropBenchmarks).fillCityMap() }))
    swiftLauncher.add(name: "searchRoutesInSwiftMultigraph", benchmark: companion.create(ctor: { return SwiftInteropBenchmarks() },
        lambda: { ($0 as! SwiftInteropBenchmarks).searchRoutesInSwiftMultigraph () }))
    swiftLauncher.add(name: "searchTravelRoutes", benchmark: companion.create(ctor: { return SwiftInteropBenchmarks() },
        lambda: { ($0 as! SwiftInteropBenchmarks).searchTravelRoutes() }))
    swiftLauncher.add(name: "availableTransportOnMap", benchmark: companion.create(ctor: { return SwiftInteropBenchmarks() },
        lambda: { ($0 as! SwiftInteropBenchmarks).availableTransportOnMap() }))
    swiftLauncher.add(name: "allPlacesMapedByInterests", benchmark: companion.create(ctor: { return SwiftInteropBenchmarks() },
        lambda: { ($0 as! SwiftInteropBenchmarks).allPlacesMapedByInterests() }))
    swiftLauncher.add(name: "getAllPlacesWithStraightRoutesTo", benchmark: companion.create(ctor: { return SwiftInteropBenchmarks() },
        lambda: { ($0 as! SwiftInteropBenchmarks).getAllPlacesWithStraightRoutesTo() }))
    swiftLauncher.add(name: "goToAllAvailablePlaces", benchmark: companion.create(ctor: { return SwiftInteropBenchmarks() },
        lambda: { ($0 as! SwiftInteropBenchmarks).goToAllAvailablePlaces() }))
    swiftLauncher.add(name: "removeVertexAndEdgesSwiftMultigraph", benchmark: companion.create(ctor: { return SwiftInteropBenchmarks() },
        lambda: { ($0 as! SwiftInteropBenchmarks).removeVertexAndEdgesSwiftMultigraph() }))
    swiftLauncher.add(name: "stringInterop", benchmark: companion.create(ctor: { return SwiftInteropBenchmarks() },
        lambda: { ($0 as! SwiftInteropBenchmarks).stringInterop() }))
    swiftLauncher.add(name: "simpleFunction", benchmark: companion.create(ctor: { return SwiftInteropBenchmarks() },
        lambda: { ($0 as! SwiftInteropBenchmarks).simpleFunction() }))
runner.runBenchmarks(args: args, run: { (arguments: BenchmarkArguments) -> [BenchmarkResult] in
    if arguments is BaseBenchmarkArguments {
        let argumentsList: BaseBenchmarkArguments = arguments as! BaseBenchmarkArguments
        return swiftLauncher.launch(numWarmIterations: argumentsList.warmup,
            numberOfAttempts: argumentsList.repeat,
            prefix: argumentsList.prefix, filters: argumentsList.filter,
            filterRegexes: argumentsList.filterRegex,
            verbose: argumentsList.verbose)
    }
    return [BenchmarkResult]()
}, parseArgs: { (args: KotlinArray,  benchmarksListAction: (() -> KotlinUnit)) -> BenchmarkArguments? in
    return runner.parse(args: args, benchmarksListAction: swiftLauncher.benchmarksListAction) },
  collect: { (benchmarks: [BenchmarkResult], arguments: BenchmarkArguments) -> Void in
    runner.collect(results: benchmarks, arguments: arguments)
}, benchmarksListAction: swiftLauncher.benchmarksListAction)