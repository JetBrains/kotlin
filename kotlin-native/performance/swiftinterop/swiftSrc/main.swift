/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import Foundation
import benchmark

var runner = BenchmarksRunner()
let args = KotlinArray<NSString>(size: Int32(CommandLine.arguments.count - 1), init: {index in
    CommandLine.arguments[Int(truncating: index) + 1] as NSString
})

let companion = BenchmarkEntryWithInit.Companion()

var swiftLauncher = SwiftLauncher()
swiftLauncher.addBase(name: "createMultigraphOfInt", benchmark: companion.create(ctor: { return SwiftInteropBenchmarks() },
        lambda: { ($0 as! SwiftInteropBenchmarks).createMultigraphOfInt() }))
    swiftLauncher.addBase(name: "fillCityMap", benchmark: companion.create(ctor: { return SwiftInteropBenchmarks() },
        lambda: { ($0 as! SwiftInteropBenchmarks).fillCityMap() }))
    swiftLauncher.addBase(name: "searchRoutesInSwiftMultigraph", benchmark: companion.create(ctor: { return SwiftInteropBenchmarks() },
        lambda: { ($0 as! SwiftInteropBenchmarks).searchRoutesInSwiftMultigraph () }))
    swiftLauncher.addExtended(name: "searchTravelRoutes", benchmark: companion.create(ctor: { return SwiftInteropBenchmarks() },
        lambda: { ($0 as! SwiftInteropBenchmarks).searchTravelRoutes() }))
    swiftLauncher.addBase(name: "availableTransportOnMap", benchmark: companion.create(ctor: { return SwiftInteropBenchmarks() },
        lambda: { ($0 as! SwiftInteropBenchmarks).availableTransportOnMap() }))
    swiftLauncher.addExtended(name: "allPlacesMapedByInterests", benchmark: companion.create(ctor: { return SwiftInteropBenchmarks() },
        lambda: { ($0 as! SwiftInteropBenchmarks).allPlacesMapedByInterests() }))
    swiftLauncher.addBase(name: "getAllPlacesWithStraightRoutesTo", benchmark: companion.create(ctor: { return SwiftInteropBenchmarks() },
        lambda: { ($0 as! SwiftInteropBenchmarks).getAllPlacesWithStraightRoutesTo() }))
    swiftLauncher.addExtended(name: "goToAllAvailablePlaces", benchmark: companion.create(ctor: { return SwiftInteropBenchmarks() },
        lambda: { ($0 as! SwiftInteropBenchmarks).goToAllAvailablePlaces() }))
    swiftLauncher.addBase(name: "removeVertexAndEdgesSwiftMultigraph", benchmark: companion.create(ctor: { return SwiftInteropBenchmarks() },
        lambda: { ($0 as! SwiftInteropBenchmarks).removeVertexAndEdgesSwiftMultigraph() }))
    swiftLauncher.addBase(name: "stringInterop", benchmark: companion.create(ctor: { return SwiftInteropBenchmarks() },
        lambda: { ($0 as! SwiftInteropBenchmarks).stringInterop() }))
    swiftLauncher.addBase(name: "simpleFunction", benchmark: companion.create(ctor: { return SwiftInteropBenchmarks() },
        lambda: { ($0 as! SwiftInteropBenchmarks).simpleFunction() }))
swiftLauncher.addBase(
    name: "WeakRefBenchmark.aliveReference",
    benchmark: BenchmarkEntryWithInitAndValidation.companion.create(
        ctor: { return WeakRefBenchmark() },
        benchmark: { ($0 as! WeakRefBenchmark).aliveReference() },
        validation: { ($0 as! WeakRefBenchmark).clean() }
    )
)
swiftLauncher.addBase(
    name: "WeakRefBenchmark.deadReference",
    benchmark: BenchmarkEntryWithInitAndValidation.companion.create(
        ctor: { return WeakRefBenchmark() },
        benchmark: { ($0 as! WeakRefBenchmark).deadReference() },
        validation: { ($0 as! WeakRefBenchmark).clean() }
    )
)
swiftLauncher.addBase(
    name: "WeakRefBenchmark.dyingReference",
    benchmark: BenchmarkEntryWithInitAndValidation.companion.create(
        ctor: { return WeakRefBenchmark() },
        benchmark: { ($0 as! WeakRefBenchmark).dyingReference() },
        validation: { ($0 as! WeakRefBenchmark).clean() }
    )
)

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
}, parseArgs: { (args: KotlinArray,  benchmarksListAction: ((KotlinBoolean) -> KotlinUnit)) -> BenchmarkArguments? in
    return runner.parse(args: args, benchmarksListAction: { (baseOnly: KotlinBoolean) in swiftLauncher.benchmarksListAction(baseOnly: baseOnly.boolValue) }) },
  collect: { (benchmarks: [BenchmarkResult], arguments: BenchmarkArguments) -> Void in
    runner.collect(results: benchmarks, arguments: arguments)
}, benchmarksListAction: { (baseOnly: KotlinBoolean) in swiftLauncher.benchmarksListAction(baseOnly: baseOnly.boolValue) })
