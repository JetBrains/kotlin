import Foundation
import Kt

let g = Gamma()
if g.alphaVal() != "alpha" || g.betaVal() != "beta" || g.gammaVal() != "gamma" {
    fatalError("Failed multi-level cache test")
}

print("OK")
