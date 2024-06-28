/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import Foundation
import benchmark

class SimpleCost: Cost {
    let value: Int

    init(cost: Int) {
        value = cost
    }

    func plus(other: Cost) -> Cost {
        let otherInstance = other as! SimpleCost
        return SimpleCost(cost: value + otherInstance.value)
    }

    func minus(other: Cost) -> Cost {
        let otherInstance = other as! SimpleCost
        return SimpleCost(cost: value - otherInstance.value)
    }

    func compareTo(other_ other: Cost) -> Int32 {
        let otherInstance = other as! SimpleCost
        if (value < otherInstance.value) {
            return -1
        }
        if (value == otherInstance.value) {
            return 0
        }
        return 1
    }
}

class SwiftInteropBenchmarks {

    let BENCHMARK_SIZE = 10000
    let SMALL_BENCHMARK_SIZE = 50
    let MEDIUM_BENCHMARK_SIZE = 100
    var multigraph = Multigraph<NSNumber>()
    var cityMap = CityMap()

    func randomInt(border: Int) -> Int {
        return Int.random(in: 1 ..< border)
    }

    func randomDouble(border: Double) -> Double {
        return Double.random(in: 0 ..< border)
    }

    func randomString(length: Int) -> String {
        let letters = "abcdefghijklmnopqrst"
        return String((0..<length).map{ _ in letters.randomElement()! })
    }

    func randomTransport() -> Transport {
        let allValues = [Transport.car, Transport.underground, Transport.bus, Transport.trolleybus, Transport.tram, Transport.taxi, Transport.foot]
        return allValues.randomElement()!
    }

    func randomInterest() -> Interest {
        let allValues = [Interest.sight, Interest.culture, Interest.park, Interest.entertainment]
        return allValues.randomElement()!
    }

    func randomPlace() -> Place {
        return Place(geoCoordinateX: randomDouble(border: 180), geoCoordinateY: randomDouble(border: 90), name: randomString(length: 5), interestCategory: randomInterest())
    }

    func randomRouteCost() -> RouteCost {
        let transportCount = randomInt(border: 7)
        let interestCount = randomInt(border: 4)
        var transports = Set<Transport>()
        var interests = Set<Interest>()

        for _ in 0...transportCount {
            transports.insert(randomTransport())
        }

        for _ in 0...interestCount {
            interests.insert(randomInterest())
        }

        return RouteCost(moneyCost: randomDouble(border: 10000), timeCost: randomDouble(border: 24), interests: interests, transport: transports)
    }

    func initMultigraph(size: Int) {
        multigraph = Multigraph<NSNumber>()
        for i in 1...size {
            multigraph.addEdge(from: NSNumber(value: i), to: NSNumber(value: i + 1), cost: SimpleCost(cost: (i + 1) % i ))
            multigraph.addEdge(from: NSNumber(value: i), to: NSNumber(value: i * 2), cost: SimpleCost(cost: (i * 2) % i))
            multigraph.addEdge(from: NSNumber(value: i + 5), to: NSNumber(value: i), cost: SimpleCost(cost: (i + 5) % i))
        }
    }

    func createMultigraphOfInt() {
        initMultigraph(size: BENCHMARK_SIZE)
    }

    func fillCityMap() {
        cityMap = CityMap()
        for _ in 0...BENCHMARK_SIZE {
            cityMap.addRoute(from: randomPlace(), to: randomPlace(), cost: randomRouteCost())
        }
    }

    func initCityMap(size: Int) {
        cityMap = CityMap()
        let allValuesInterests = [Interest.sight, Interest.culture, Interest.park, Interest.entertainment]
        let allValuesTransport = [Transport.car, Transport.underground, Transport.bus, Transport.trolleybus, Transport.tram, Transport.taxi, Transport.foot]
        for i in 1...size {
            let from = Place(geoCoordinateX: Double(i % 180), geoCoordinateY: Double(i % 90 + 90 % i),
                  name: randomString(length: 5), interestCategory: allValuesInterests[i % allValuesInterests.count])
            let to = Place(geoCoordinateX: Double(i % 180 + 180 % i), geoCoordinateY: Double(i % 90),
                           name: randomString(length: 5), interestCategory: allValuesInterests[(i + 5) % allValuesInterests.count])
            var transports = Set<Transport>()
            var interests = Set<Interest>()
            for j in 0...i % 2 + 1 {
                interests.insert(allValuesInterests[(i + j) % allValuesInterests.count])
            }
            for j in 0...i % 2 + 1 {
                transports.insert(allValuesTransport[i % allValuesTransport.count])
            }
            let cost = RouteCost(moneyCost: Double((i * 2) % 10000), timeCost: Double((i + 3) % 24), interests: interests, transport: transports)
            cityMap.addRoute(from: from, to: to, cost: cost)
        }
    }

    func searchRoutesInSwiftMultigraph() {
        autoreleasepool {
            initMultigraph(size: SMALL_BENCHMARK_SIZE)
            for _ in 0...SMALL_BENCHMARK_SIZE {
                var vertexes = Array(multigraph.allVertexes)
                var result = multigraph.searchRoutesWithLimits(start: 1,
                                                               finish: NSNumber(value: SMALL_BENCHMARK_SIZE / 2 + SMALL_BENCHMARK_SIZE / 4),
                                                               limits: SimpleCost(cost: SMALL_BENCHMARK_SIZE / 5))
                var count = result.map{ $0.count }.reduce(0, +)
            }
        }
    }

    func searchTravelRoutes() {
        autoreleasepool {
            cityMap = CityMap()
            initCityMap(size: BENCHMARK_SIZE)
            let transports: Set = [Transport.car, Transport.underground]
            let interests: Set = [Interest.sight, Interest.culture]
            let cost = RouteCost(moneyCost: 500, timeCost: 6, interests: interests, transport: transports)
            for _ in 0...SMALL_BENCHMARK_SIZE {
                var result = cityMap.getRoutes(start: Array(cityMap.allPlaces).first!, finish: Array(cityMap.allPlaces).last!, limits: cost)
                var totalCost = result.flatMap{ $0 }.map { $0.cost.moneyCost }.reduce(0, +)
            }
        }
    }

    func availableTransportOnMap() {
        autoreleasepool {
            cityMap = CityMap()
            initCityMap(size: MEDIUM_BENCHMARK_SIZE)
            Set(cityMap.allRoutes.map { (cityMap.getRouteById(id: $0.id).cost as! RouteCost).transport })
        }
    }

    func allPlacesMapedByInterests() {
        autoreleasepool {
            cityMap = CityMap()
            initCityMap(size: MEDIUM_BENCHMARK_SIZE)
            let placesByInterests = cityMap.allPlaces.reduce([Interest: Array<Place>]()) { (dict, place) -> [Interest: Array<Place>] in
                var dict = dict
                if (dict[place.interestCategory] != nil) {
                    dict[place.interestCategory]?.append(place)
                } else {
                    dict[place.interestCategory] = [place]
                }
                return dict
            }
        }
    }

    func getAllPlacesWithStraightRoutesTo() {
        autoreleasepool {
            cityMap = CityMap()
            initCityMap(size: MEDIUM_BENCHMARK_SIZE)
            for _ in 0...SMALL_BENCHMARK_SIZE {
                let start = cityMap.allPlaces.randomElement()!
                let availableRoutes = cityMap.getAllStraightRoutesFrom(place: start)
                var _ = availableRoutes.map { $0.to }
            }
        }
    }

    func goToAllAvailablePlaces() {
        autoreleasepool {
            cityMap = CityMap()
            initCityMap(size: MEDIUM_BENCHMARK_SIZE)
            for _ in 0...SMALL_BENCHMARK_SIZE {
                let start = cityMap.allPlaces.randomElement()!
                let availableRoutes = cityMap.getAllStraightRoutesFrom(place: start)
                availableRoutes.map { 2 * $0.cost.moneyCost }
            }
        }
    }

    func removeVertexAndEdgesSwiftMultigraph() {
        autoreleasepool {
            initMultigraph(size: SMALL_BENCHMARK_SIZE)
            let multigraphCopy = multigraph.doCopyMultigraph()
            var edges = multigraphCopy.allEdges
            while (!edges.isEmpty) {
                multigraphCopy.removeEdge(id: UInt32(edges.randomElement()!))
                let vertexes = multigraphCopy.allVertexes as! Set<NSNumber>
                multigraphCopy.removeVertex(vertex: vertexes.randomElement()!)
                edges = multigraphCopy.allEdges
            }
        }
    }

    func stringInterop() {
        autoreleasepool {
            cityMap = CityMap()
            fillCityMap()
            let place = cityMap.allPlaces.first!
            for _ in 0...BENCHMARK_SIZE {
                let _ = place.fullDescription
            }
        }
    }

    func simpleFunction() {
        autoreleasepool {
            cityMap = CityMap()
            fillCityMap()
            let place = cityMap.allPlaces.first!
            for _ in 0...BENCHMARK_SIZE {
                let _ = place.compareTo(other:place)
            }
        }
    }
}