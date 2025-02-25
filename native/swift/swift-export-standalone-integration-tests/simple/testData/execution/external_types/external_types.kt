// KIND: STANDALONE
// MODULE: ExternalTypes
// FILE: external_types_smoke.kt
import platform.Foundation.dateByAddingTimeInterval

lateinit var store_nsdate: platform.Foundation.NSDate

fun platform.Foundation.NSDate.addingOneHour(): platform.Foundation.NSDate = this.dateByAddingTimeInterval(60.0*60)
