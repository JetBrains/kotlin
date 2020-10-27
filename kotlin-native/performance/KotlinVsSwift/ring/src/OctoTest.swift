/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import Foundation

struct UnsupportedOperationException: Error {
    let message: String
    
}

class Leaf<T : Equatable> : NodeEntity<T>, Equatable {
    static func == (lhs: Leaf<T>, rhs: Leaf<T>) -> Bool {
        return lhs.value == rhs.value
    }
    
    var value: T
    
    init(_ value: T) {
        self.value = value
    }

    override func toString() -> String  {
        return "L{$value}"
    }
}

class NodeEntity<T: Equatable>: Node {
    func toString() -> String {
        return ""
    }
    
    func set(x: Int, y: Int, z: Int, value: T, depth: Int) throws -> Bool {
        throw UnsupportedOperationException(message: "set on Leaf element")
    }
}

private protocol Node {
    associatedtype T

    func set(x: Int, y: Int, z: Int, value: T, depth: Int) throws -> Bool
    func toString() -> String
}

class Branch<T: Equatable> : NodeEntity<T> {
    var nodes = [NodeEntity<T>?](repeating: nil, count: 8)
    
    override init() {}

    init(_ value: T, _ exclude: Int) {
        var i = 0
        while (i < 8) {
            if (i != exclude) {
                nodes[i] = Leaf(value)
            }
            i += 1
        }
    }

    private func canClusterize(_ value: T) -> Bool {
        var i = 0
        while (i < 8) {
            let w = nodes[i]
            if (w == nil || !(w is Leaf) || value != (w as? Leaf)?.value) {
                return false
            }
            i += 1
        }
        return true
    }

    override func set(x: Int, y: Int, z: Int, value: T, depth: Int) throws -> Bool {
        let branchIndex = OctoTree<T>.number(x, y, z, depth)
        let node = nodes[branchIndex]
        if (node == nil) {
            if (depth == 0) {
                nodes[branchIndex] = Leaf(value)
                return canClusterize(value)
            } else {
                nodes[branchIndex] = Branch<T>()
            }
        } else if let leaf = node as? Leaf<T> {
            if (leaf.value == value) {
                return false
            } else if (depth == 0) {
                leaf.value = value
                return canClusterize(value)
            }
            nodes[branchIndex] = Branch(leaf.value, OctoTree<T>.number(x, y, z, depth - 1))
        }

        if (try nodes[branchIndex]!.set(x: x, y: y, z: z, value: value, depth: depth - 1)) {
            nodes[branchIndex] = Leaf(value)
            return canClusterize(value)
        }
        return false
    }

    
    override func toString() -> String  {
        return nodes.map { $0?.toString() ?? "null" }.joined(separator: ",")
    }
}

class OctoTree<T: Equatable> {
    let depth: Int
    
    init(_ depth: Int) {
        self.depth = depth
    }

    private var root: NodeEntity<T>? = nil
    private var actual = false

    func get(_ x: Int, _ y: Int, _ z: Int) -> T? {
        var dep = depth
        var iter = root
        while (true) {
            if (iter == nil) {
                return nil
            } else if let leaf = iter as? Leaf<T> {
                return leaf.value
            }
            
            dep -= 1
            iter = (iter as! Branch<T>).nodes[OctoTree<T>.number(x, y, z, dep)]
        }
    }

    func set(x: Int, y: Int, z: Int, value: T) {
        if (root == nil) {
            root = Branch()
        }
        do {
            if (try root!.set(x: x, y: y, z: z, value: value, depth: depth - 1)) {
                root = Leaf(value)
            }
        } catch {
            print("Exception")
        }
        actual = false
    }

    func toString() -> String {
        return root?.toString() ?? ""
    }

    static func number(_ x: Int, _ y: Int, _ z: Int, _ depth: Int) -> Int {
        let mask = 1 << depth
        if (x & mask != 0) {
            if (y & mask != 0) {
                if (z & mask != 0) {
                    return 7
                }
                return 6
            }
            if (z & mask != 0) {
                return 5
            }
            return 4
        }
        if (y & mask != 0) {
            if (z & mask != 0) {
                return 3
            }
            return 2
        }
        if (z & mask != 0) {
            return 1
        }
        return 0
    }
    
}

func octoTest() {
    let tree = OctoTree<Bool>(4)
    let to = (2 << tree.depth)

    var x = 0
    var y = 0
    var z = 0

    while (x < to) {
        y = 0
        while (y < to) {
            z = 0
            while (z < to) {
                let c = (z + to * y + to * to * x) % 2 == 0

                tree.set(x: x, y: y, z: z, value: c)
                z += 1
            }
            y += 1
        }
        x += 1
    }

    x = 0
    y = 0
    z = 0
    while (x < to) {
        y = 0
        while (y < to) {
            z = 0
            while (z < to) {
                let c = (z + to * y + to * to * x) % 2 == 0

                let res = tree.get(x, y, z)

                assert(res == c)
                z += 1
            }
            y += 1
        }
        x += 1
    }
}
