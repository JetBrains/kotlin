class SomeServiceUsage {
    val service: SomeService
        get() = SomeService.getInstanceNotNull()
    val serviceNullable: SomeService
        get() = SomeService.getInstanceNullable()

    // elvis
    val serviceNotNullByDataFlow: SomeService
        get() {
            val s = SomeService.getInstanceNullable()
            return s ?: SomeService.getInstanceNotNull()
        }

    // nullable, bang-bang
    fun aString1(): String {
        return serviceNullable.nullableString()
    }

    // nullable
    fun aString2(): String {
        return service.nullableString()
    }

    // not nullable
    fun aString3(): String {
        return service.notNullString()
    }

    // nullable, no bang-bang
    fun aString4(): String {
        return serviceNotNullByDataFlow.nullableString()
    }

    // not nullable, no bang-bang
    fun aString5(): String {
        return serviceNotNullByDataFlow.notNullString()
    }

    // nullable, safe-call
    fun aString6(): String? {
        val s = serviceNullable
        return if (s != null) {
            s.nullableString()
        } else {
            null
        }
    }
}