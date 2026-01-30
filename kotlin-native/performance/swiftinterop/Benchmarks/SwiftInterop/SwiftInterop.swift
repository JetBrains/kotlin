/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import Benchmark
import Foundation

let benchmarks = {
    Benchmark("createMultigraphOfInt") { benchmark, instance in
        for _ in benchmark.scaledIterations {
            instance.createMultigraphOfInt()
        }
    } setup: {
        SwiftInteropBenchmarks()
    }
    Benchmark("fillCityMap") { benchmark, instance in
        for _ in benchmark.scaledIterations {
            instance.fillCityMap()
        }
    } setup: {
        SwiftInteropBenchmarks()
    }
    Benchmark("searchRoutesInSwiftMultigraph") { benchmark, instance in
        for _ in benchmark.scaledIterations {
            instance.searchRoutesInSwiftMultigraph()
        }
    } setup: {
        SwiftInteropBenchmarks()
    }
    Benchmark("searchTravelRoutesExtended") { benchmark, instance in
        for _ in benchmark.scaledIterations {
            instance.searchTravelRoutes()
        }
    } setup: {
        SwiftInteropBenchmarks()
    }
    Benchmark("availableTransportOnMap") { benchmark, instance in
        for _ in benchmark.scaledIterations {
            instance.availableTransportOnMap()
        }
    } setup: {
        SwiftInteropBenchmarks()
    }
    Benchmark("allPlacesMapedByInterestsExtended") { benchmark, instance in
        for _ in benchmark.scaledIterations {
            instance.allPlacesMapedByInterests()
        }
    } setup: {
        SwiftInteropBenchmarks()
    }
    Benchmark("getAllPlacesWithStraightRoutesTo") { benchmark, instance in
        for _ in benchmark.scaledIterations {
            instance.getAllPlacesWithStraightRoutesTo()
        }
    } setup: {
        SwiftInteropBenchmarks()
    }
    Benchmark("goToAllAvailablePlacesExtended") { benchmark, instance in
        for _ in benchmark.scaledIterations {
            instance.goToAllAvailablePlaces()
        }
    } setup: {
        SwiftInteropBenchmarks()
    }
    Benchmark("removeVertexAndEdgesSwiftMultigraph") { benchmark, instance in
        for _ in benchmark.scaledIterations {
            instance.removeVertexAndEdgesSwiftMultigraph()
        }
    } setup: {
        SwiftInteropBenchmarks()
    }
    Benchmark("stringInterop") { benchmark, instance in
        for _ in benchmark.scaledIterations {
            instance.stringInterop()
        }
    } setup: {
        SwiftInteropBenchmarks()
    }
    Benchmark("simpleFunction") { benchmark, instance in
        for _ in benchmark.scaledIterations {
            instance.simpleFunction()
        }
    } setup: {
        SwiftInteropBenchmarks()
    }

    Benchmark("WeakReference.aliveReference") { benchmark, instance in
        for _ in benchmark.scaledIterations {
            instance.aliveReference()
        }
        benchmark.stopMeasurement()
        instance.clean()
    } setup: {
        WeakRefBenchmark()
    }
    Benchmark("WeakReference.deadReference") { benchmark, instance in
        for _ in benchmark.scaledIterations {
            instance.deadReference()
        }
        benchmark.stopMeasurement()
        instance.clean()
    } setup: {
        WeakRefBenchmark()
    }
    Benchmark("WeakReference.dyingReference") { benchmark, instance in
        for _ in benchmark.scaledIterations {
            instance.dyingReference()
        }
        benchmark.stopMeasurement()
        instance.clean()
    } setup: {
        WeakRefBenchmark()
    }
}
