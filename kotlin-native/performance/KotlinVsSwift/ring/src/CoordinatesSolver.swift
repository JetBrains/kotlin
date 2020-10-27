/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import Foundation

class CoordinatesSolverBenchmark {
    let solver: Solver

    init() {
        let inputValue = """
12 5 3 25 3 9 3 9 1 3
13 3 12 6 10 10 12 2 10 10
9 2 9 5 6 12 5 0 2 10
10 14 12 5 3 9 5 2 10 10
8 1 3 9 4 0 3 14 10 10
12 0 4 6 9 6 12 5 6 10
11 12 3 9 6 9 5 3 9 6
8 5 6 8 3 12 7 10 10 11
12 3 13 6 12 3 9 6 12 2
13 4 5 5 5 6 12 5 5 2
1
"""
        let input = CoordinatesSolverBenchmark.readTillParsed(inputValue)

        solver = Solver(input!)
    }

    struct Coordinate: Equatable {
        let x: Int
        let y: Int
    }

    struct Field {
        let x: Int
        let y: Int
        let value: Int8
        func northWall() -> Bool {
            return value & 1 != 0
        }

        func eastWall() -> Bool {
            return value & 2 != 0
        }

        func southWall() -> Bool {
            return value & 4 != 0
        }

        func westWall() -> Bool {
            return value & 8 != 0
        }

        func hasObject() -> Bool {
            return value & 16 != 0
        }
    }

    struct Input {
        let labyrinth: Labyrinth
        let nObjects: Int
    }

    class Labyrinth {
        let width: Int
        let height: Int
        let fields: [Field]
        init(_ width: Int, _ height: Int, _ fields: Array<Field>) {
            self.width = width
            self.height = height
            self.fields = fields
        }
        func getField(_ x: Int, _ y: Int) -> Field {
            return fields[x + y * width]
        }
    }

    struct Output {
        let steps: [Coordinate?]
    }

    class InputParser {
        private var rows : [[Field]] = []
        private var numObjects: Int = 0

        private var input: Input {
            get {
                let width = rows[0].count
                var fields = [Field?](repeating: nil, count: width * rows.count)

                for y in rows.indices {
                    let row = rows[y]
                    for p in (y*width)..<(y*width + width) {
                        fields[p] = row[p-y*width]
                    }
                }

                let labyrinth = Labyrinth(width, rows.count, fields.filter { $0 != nil } as! Array<CoordinatesSolverBenchmark.Field>)

                return Input(labyrinth: labyrinth, nObjects: numObjects)
            }
        }

        func feedLine(line: String) -> Input? {
            let items = line.split(separator: " ").filter { $0 != "" }

            if (items.count == 1) {
                numObjects = Int(items[0]) ?? 0
                return input
            } else if (items.count > 0) {
                let rowNum = rows.count
                var row = [Field?](repeating: nil, count: items.count)

                for col in items.indices {
                    row[col] = Field(x: rowNum, y: col, value: Int8(items[col]) ?? 0)
                }

                rows.append(row as! [CoordinatesSolverBenchmark.Field])
            }

            return nil
        }
    }


    class Solver {
        private let input: Input
        init(_ input: Input) {
            self.input = input
            objects = []
            for f in input.labyrinth.fields {
                if (f.hasObject()) {
                    objects.append(Coordinate(x: f.x, y: f.y))
                }
            }

            width = input.labyrinth.width
            height = input.labyrinth.height
            maze_end = Coordinate(x: width - 1, y: height - 1)
        }
        private var objects: [Coordinate]

        private let width: Int
        private let height: Int
        private let maze_end: Coordinate

        private var counter: Int64 = 0

        func solve() -> Output {
            var steps = [Coordinate]()

            for o in objects.indices {
                var limit = input.labyrinth.width + input.labyrinth.height - 2

                var ss: [Coordinate]? = nil
                while (ss == nil) {
                    if (o == 0) {
                        ss = solveWithLimit(limit, Solver.MAZE_START) { $0[$0.count - 1] == objects[0] }
                    } else {
                        ss = solveWithLimit(limit, objects[o - 1]) { $0[$0.count - 1] == objects[o] }
                    }

                    if (ss != nil) {
                        steps.append(contentsOf: ss!)
                    }

                    limit += 1
                }
            }

            var limit = input.labyrinth.width + input.labyrinth.height - 2

            var ss: [Coordinate]? = nil
            while (ss == nil) {
                ss = solveWithLimit(limit, objects[objects.count - 1]) { $0[$0.count - 1] == maze_end }

                if (ss != nil) {
                    steps.append(contentsOf: ss!)
                }

                limit += 1
            }

            return createOutput(steps: steps)
        }

        private func createOutput(steps: [Coordinate]) -> Output {
            var objects : [Coordinate] = self.objects
            var outSteps : [Coordinate?] = []

            for step in steps {
                outSteps.append(step)

                if (objects.contains(step)) {
                    outSteps.append(nil)
                    objects = objects.filter { $0 != step }
                }
            }

            return Output(steps: outSteps)
        }

        private func isValid(steps: [Coordinate]) -> Bool {
            counter += 1
            let coords = steps[steps.count - 1]
            let x = coords.x
            let y = coords.y
            return (!(x == input.labyrinth.width - 1 && y == input.labyrinth.height - 1)) ? false : objects.filter{ steps.contains($0) == false }.count == 0
        }

        private func getPossibleSteps(_ now: Coordinate, _ previous: Coordinate?) -> [Coordinate] {
            let field = input.labyrinth.getField(now.x, now.y)

            var possibleSteps: [Coordinate] = []

            if (now.x != width - 1 && !field.eastWall()) {
                possibleSteps.append(Coordinate(x: now.x + 1, y: now.y))
            }
            if (now.x != 0 && !field.westWall()) {
                possibleSteps.append(Coordinate(x: now.x - 1, y: now.y))
            }
            if (now.y != 0 && !field.northWall()) {
                possibleSteps.append(Coordinate(x: now.x, y: now.y - 1))
            }
            if (now.y != height - 1 && !field.southWall()) {
                possibleSteps.append(Coordinate(x: now.x, y: now.y + 1))
            }

            if (!field.hasObject() && previous != nil) {
                possibleSteps = possibleSteps.filter { $0 != previous }
            }

            return possibleSteps
        }

        private func solveWithLimit(_ limit: Int, _ start: Coordinate, _ validFn: ([Coordinate]) -> Bool) -> [Coordinate]? {
            var steps: [Coordinate]? = findFirstLegitSteps(nil, start, limit)

            while (steps != nil && !validFn(steps!)) {
                steps = alter(start, nil, &steps!)
            }

            return steps
        }

        private func findFirstLegitSteps(_ startPrev: Coordinate?, _ start: Coordinate, _ num: Int) -> [Coordinate]? {
            var steps: [Coordinate]? = []
            
            var i = 0
            while (i < num) {
                let prev: Coordinate?
                let state: Coordinate

                if (i == 0) {
                    state = start
                    prev = startPrev
                } else if (i == 1) {
                    state = steps![i - 1]
                    prev = startPrev
                } else {
                    state = steps![i - 1]
                    prev = steps![i - 2]
                }

                let possibleSteps = getPossibleSteps(state, prev)

                if (possibleSteps.count == 0) {
                    if (steps!.count == 0) {
                        return nil
                    }

                    steps = alter(start, startPrev, &steps!)
                    if (steps == nil) {
                        return nil
                    }

                    i -= 1
                    i += 1
                    continue
                }

                let newStep = possibleSteps[0]
                steps!.append(newStep)
                i += 1
            }

            return steps
        }

        private func alter(_ start: Coordinate, _ startPrev: Coordinate?, _ steps: inout [Coordinate]) -> [Coordinate]? {
            let size = steps.count

            var i = size - 1
            while (i >= 0) {
                let current = steps[i]
                let prev = i == 0 ? start : steps[i - 1]
                let prevprev: Coordinate?
                if (i > 1) {
                    prevprev = steps[i - 2]
                } else if (i == 1) {
                    prevprev = start
                } else {
                    prevprev = startPrev
                }

                let alternatives = getPossibleSteps(prev, prevprev)
                let index = alternatives.firstIndex(of: current)

                if (index != nil && index != alternatives.count - 1) {
                    let newItem = alternatives[index! + 1]
                    steps[i] = newItem

                    let remainder = findFirstLegitSteps(prev, newItem, size - i - 1)
                    if (remainder == nil) {
                        i += 1
                        i -= 1
                        continue
                    }

                    Solver.removeAfterIndexExclusive(&steps, i)
                    steps.append(contentsOf: remainder ?? [])

                    return steps
                } else {
                    if (i == 0) {
                        return nil
                    }
                }
                i -= 1
            }

            return steps
        }

        
        private static let MAZE_START = Coordinate(x: 0, y: 0)
        private static func removeAfterIndexExclusive(_ list: inout [Coordinate], _ index: Int) {
            let rnum = list.count - 1 - index

            for _ in 0..<rnum {
                list.remove(at: list.count - 1)
            }
        }
        
    }

    private static func readTillParsed(_ inputValue: String) -> Input? {
        let parser = InputParser()
        var input: Input? = nil
        inputValue.split(separator: "\n").forEach { line in
            input = parser.feedLine(line: String(line))
        }

        return input
    }

    func solve() {
        let output = solver.solve()

        for c in output.steps {
            let value = (c == nil) ? "felvesz" : "${c.x} ${c.y}"
        }
    }
}
