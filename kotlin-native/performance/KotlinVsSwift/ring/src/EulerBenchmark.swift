/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import Foundation


struct FibonacciGenerator: Sequence, IteratorProtocol {
    var a = 0
    var b = 1
    var current = 1
    mutating func next() -> Int? {
        defer {
            current = a + b
            a = b
            b = current
        }
        return current
    }
}

extension Int64 {
    func isPalindrome() -> Bool {
        return String(self) == String(String(self).reversed())
    }
}

extension Range where Bound == Int {
    @inlinable func sum(_ predicate: (Int) -> Bool) -> Int {
        var sum = 0
        for i in self {
            if (predicate(i)) {
                sum += i
            }
        }
        return sum
    }
}

extension Sequence where Element == Int {
    @inlinable func sum(_ predicate: (Int) -> Bool) -> Int {
        var sum = 0
        for i in self {
            if (predicate(i)) {
                sum += i
            }
        }
        return sum
    }
}

extension Sequence {
    func takeWhile(condition: (Element) -> Bool) -> [Element] {
        var result: [Element] = []
        for x in self {
            guard condition(x) else { break }
            result.append(x)
        }
        return result
    }
}

class EulerBenchmark {
    func problem1bySequence() -> Int {
        return AnySequence<Int>(1...Constants.BENCHMARK_SIZE).lazy.sum( { $0 % 3 == 0 || $0 % 5 == 0} )
    }
    
    func problem1() -> Int {
        return (1...Constants.BENCHMARK_SIZE).sum( { $0 % 3 == 0 || $0 % 5 == 0} )
    }

    func problem2() -> Int {
        return FibonacciGenerator().takeWhile { $0 < Constants.BENCHMARK_SIZE }.sum { $0 % 2 == 0 }
    }
    
    func problem4() -> Int64 {
        let s: Int64 = Int64(Constants.BENCHMARK_SIZE)
        let maxLimit = (s-1)*(s-1)
        let minLimit = (s/10)*(s/10)
        let maxDiv = Constants.BENCHMARK_SIZE-1
        let minDiv = Constants.BENCHMARK_SIZE/10
        for i in stride(from: maxLimit, through: minLimit, by: -1) {
            if (!i.isPalindrome())  {
                continue
            }
            for j in minDiv...maxDiv {
                if (i % Int64(j) == 0) {
                    let res = i / Int64(j)
                    if ((Int64(minDiv)...Int64(maxDiv)).contains(res)) {
                        return i
                    }
                }
            }
        }
        return -1
    }
    
    private let veryLongNumber = """
    73167176531330624919225119674426574742355349194934
    96983520312774506326239578318016984801869478851843
    85861560789112949495459501737958331952853208805511
    12540698747158523863050715693290963295227443043557
    66896648950445244523161731856403098711121722383113
    62229893423380308135336276614282806444486645238749
    30358907296290491560440772390713810515859307960866
    70172427121883998797908792274921901699720888093776
    65727333001053367881220235421809751254540594752243
    52584907711670556013604839586446706324415722155397
    53697817977846174064955149290862569321978468622482
    83972241375657056057490261407972968652414535100474
    82166370484403199890008895243450658541227588666881
    16427171479924442928230863465674813919123162824586
    17866458359124566529476545682848912883142607690042
    24219022671055626321111109370544217506941658960408
    07198403850962455444362981230987879927244284909188
    84580156166097919133875499200524063689912560717606
    05886116467109405077541002256983155200055935729725
    71636269561882670428252483600823257530420752963450
    """
    
    func problem8() -> Int64 {
        var productSize = 13
        if ((1...10).contains(Constants.BENCHMARK_SIZE)) {
            productSize = 4
        } else if ((11...1000).contains(Constants.BENCHMARK_SIZE)) {
            productSize = 8
        }
        var digits: [Int] = []
        for digit in veryLongNumber {
            if (("0"..."9").contains(digit)) {
                if let number = Int(String(digit)) {
                    if let secNumber = Int(String("0")) {
                        digits.append(number - secNumber)
                    }
                }
            }
        }
        var largest: Int64 = 0
        for i in 0...digits.count - productSize - 1 {
            var product: Int64 = 1
            for j in 0...productSize-1 {
                product *= Int64(digits[i+j])
            }
            if (product > largest) {
                largest = product
            }
        }
        return largest
    }
    
    func problem9() -> Int64 {
        for c in Constants.BENCHMARK_SIZE/3...Constants.BENCHMARK_SIZE-3 {
            let c2 = Int64(c) * Int64(c)
            for b in (Constants.BENCHMARK_SIZE-c)/2..<c {
                if (b+c >= Constants.BENCHMARK_SIZE) {
                    break
                }
                let a = Constants.BENCHMARK_SIZE - b - c
                if (a >= b) {
                    continue
                }
                let b2 = Int64(b) * Int64(b)
                let a2 = Int64(a) * Int64(a)
                if (c2 == b2 + a2) {
                    return Int64(a) * Int64(b) * Int64(c)
                }
            }
        }
        return -1
    }
    
    class Children {
        let left: Int
        let right: Int
        init(_ left: Int, _ right: Int) {
            self.left = left
            self.right = right
        }
    }
    
    func problem14() -> [Int] {
        // Build a tree
        // index is produced from first & second
        let tree = (0..<Constants.BENCHMARK_SIZE).map { i in Children(i*2, (i>4 && (i+2) % 6 == 0) ? (i-1)/3 : 0)}
        // Find longest chain by DFS
        func dfs(_ begin: Int) -> [Int] {
            if (begin == 0 || begin >= Constants.BENCHMARK_SIZE) {
                return []
            }
            let left = dfs(tree[begin].left)
            let right = dfs(tree[begin].right)
            return [begin] + ((left.count > right.count) ? left : right)
        }
        return dfs(1)
    }
    
    class Way {
        let length: Int
        let next: Int
        init(_ length: Int, _ next: Int) {
            self.length = length
            self.next = next
        }
    }
    
    func problem14full() -> [Int] {
        var map: [Int: Way] = [:]
        // Starting point
        map[1] = Way(0, 0)
        // Check all other numbers
        var bestNum = 0
        var bestLen = 0
        func go(_ begin: Int) -> Way {
            let res = map[begin]
            if (res != nil) {
                return res!
            }
            let next = (begin % 2 == 0) ? begin/2 : 3*begin+1
            let childRes = go(next)
            let myRes = Way(childRes.length + 1, next)
            map[begin] = myRes
            return myRes
        }
        for i in 2...Constants.BENCHMARK_SIZE-1 {
            let res = go(i)
            if (res.length > bestLen) {
                bestLen = res.length
                bestNum = i
            }
        }
        func unroll(_ begin: Int) -> [Int] {
            if (begin == 0) {
                return []
            }
            let next = map[begin]?.next ?? 0
            return [begin] + unroll(next)
        }
        return unroll(bestNum)
    }
}
