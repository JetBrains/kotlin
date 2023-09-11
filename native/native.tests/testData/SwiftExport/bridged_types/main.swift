import Foundation

let any1 = kotlin.Object()
let any2 = kotlin.Object()

precondition(any1 !== any2, "Different Any objects should have different identity")

precondition(identity(obj: any1) == any1)
precondition(equals(any1: any1, any2: any1), "Any instance should be equal to itself")
precondition(refEquals(any1: any1, any2: any1), "Single Any instance should be the same reference")

precondition(!equals(any1: any1, any2: any2), "Two any instances should be different")
precondition(!refEquals(any1: any1, any2: any2), "Two any instances should be different references")
