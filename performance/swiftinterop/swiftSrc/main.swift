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
runner.runBenchmarks(args: args, run: { (parser: ArgParser) -> [BenchmarkResult] in
    var swiftBenchmarks = SwiftInteropBenchmarks()
    var swiftLauncher = SwiftLauncher(numWarmIterations: parser.get(name: "warmup") as! Int32,
        numberOfAttempts: parser.get(name: "repeat") as! Int32, prefix: parser.get(name: "prefix") as! String)
    swiftLauncher.add(name: "createMultigraphOfInt", benchmark: swiftBenchmarks.createMultigraphOfInt)
    swiftLauncher.add(name: "fillCityMap", benchmark: swiftBenchmarks.fillCityMap)
    swiftLauncher.add(name: "searchRoutesInSwiftMultigraph", benchmark: swiftBenchmarks.searchRoutesInSwiftMultigraph)
    swiftLauncher.add(name: "searchTravelRoutes", benchmark: swiftBenchmarks.searchTravelRoutes)
    swiftLauncher.add(name: "availableTransportOnMap", benchmark: swiftBenchmarks.availableTransportOnMap)
    swiftLauncher.add(name: "allPlacesMapedByInterests", benchmark: swiftBenchmarks.allPlacesMapedByInterests)
    swiftLauncher.add(name: "getAllPlacesWithStraightRoutesTo", benchmark: swiftBenchmarks.getAllPlacesWithStraightRoutesTo)
    swiftLauncher.add(name: "goToAllAvailablePlaces", benchmark: swiftBenchmarks.goToAllAvailablePlaces)
    swiftLauncher.add(name: "removeVertexAndEdgesSwiftMultigraph", benchmark: swiftBenchmarks.removeVertexAndEdgesSwiftMultigraph)
    swiftLauncher.add(name: "stringInterop", benchmark: swiftBenchmarks.stringInterop)
    swiftLauncher.add(name: "simpleFunction", benchmark: swiftBenchmarks.simpleFunction)
    return swiftLauncher.launch(filters: parser.getAll(name: "filter"), filterRegexes: parser.getAll(name: "filterRegex"))
}, parseArgs: runner.parse, collect: { (benchmarks: [BenchmarkResult], parser: ArgParser) -> Void in
    runner.collect(results: benchmarks, parser: parser)
})