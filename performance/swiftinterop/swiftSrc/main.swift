/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import Foundation
import CityMap

var runner = BenchmarksRunner()
let args = KotlinArray(size: Int32(CommandLine.arguments.count - 1), init: {index in
    CommandLine.arguments[Int(truncating: index) + 1]
})

let companion = BenchmarkEntryWithInit.Companion()

var swiftLauncher = SwiftLauncher()

runner.runBenchmarks(args: args, run: { (parser: ArgParser) -> [BenchmarkResult] in
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
    return swiftLauncher.launch(numWarmIterations: parser.get(name: "warmup") as! Int32,
        numberOfAttempts: parser.get(name: "repeat") as! Int32,
        prefix: parser.get(name: "prefix") as! String, filters: parser.getAll(name: "filter"),
        filterRegexes: parser.getAll(name: "filterRegex"))
}, parseArgs: { (args: KotlinArray,  benchmarksListAction: ((ArgParser) -> KotlinUnit)) -> ArgParser? in
    return runner.parse(args: args, benchmarksListAction: swiftLauncher.benchmarksListAction) },
  collect: { (benchmarks: [BenchmarkResult], parser: ArgParser) -> Void in
    runner.collect(results: benchmarks, parser: parser)
}, benchmarksListAction: swiftLauncher.benchmarksListAction)