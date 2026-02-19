import Testing
import ExternalTypes
import Foundation

@Test
func testSavingNSDate() throws {
    let date = NSDate()

    store_nsdate = date

    try #require(store_nsdate == date)
}

@Test
func testModifyingNSDate() throws {
    let date = NSDate()

    let future = addingOneHour(date)

    try #require(future.timeIntervalSince(date as Date) == 60*60)
}
