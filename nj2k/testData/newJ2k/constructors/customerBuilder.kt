package org.test.customer

internal class Customer(first: String, last: String) {
    val firstName: String
    val lastName: String

    private fun doSmthBefore() {}
    private fun doSmthAfter() {}

    init {
        doSmthBefore()
        firstName = first
        lastName = last
        doSmthAfter()
    }
}

internal class CustomerBuilder {
    var _firstName = "Homer"
    var _lastName = "Simpson"
    fun WithFirstName(firstName: String): CustomerBuilder {
        _firstName = firstName
        return this
    }

    fun WithLastName(lastName: String): CustomerBuilder {
        _lastName = lastName
        return this
    }

    fun Build(): Customer {
        return Customer(_firstName, _lastName)
    }
}

object User {
    fun main() {
        val customer = CustomerBuilder()
                .WithFirstName("Homer")
                .WithLastName("Simpson")
                .Build()
        println(customer.firstName)
        println(customer.lastName)
    }
}