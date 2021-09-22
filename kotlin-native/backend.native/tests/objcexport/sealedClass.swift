import Kt

private func test1() throws {
    let user = Person.User(id: 1)
    let employee = Person.WorkerEmployee(id: 2)
    let contractor = Person.WorkerContractor(id: 3)
    let userAsPerson: Person = user
    let employeeAsWorker: Person.Worker = employee
    let contractorAsWorker: Person.Worker = contractor

    try assertEquals(actual: user.id, expected: 1)
    try assertEquals(actual: (userAsPerson as! Person.User).id, expected: 1)
    try assertEquals(actual: employee.id, expected: 2)
    try assertEquals(actual: (employeeAsWorker as! Person.WorkerEmployee).id, expected: 2)
    try assertEquals(actual: contractor.id, expected: 3)
    try assertEquals(actual: (contractorAsWorker as! Person.WorkerContractor).id, expected: 3)
}

class SealedClassTests : SimpleTestProvider {
    override init() {
        super.init()

        test("Test1", test1)
    }
}