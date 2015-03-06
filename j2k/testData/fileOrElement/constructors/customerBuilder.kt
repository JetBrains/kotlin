package org.test.customer

class Customer(public val firstName: String, public val lastName: String) {

    {
        doSmthBefore()
        doSmthAfter()
    }

    private fun doSmthBefore() {
    }

    private fun doSmthAfter() {
    }
}

class CustomerBuilder {
    public var _firstName: String = "Homer"
    public var _lastName: String = "Simpson"

    public fun WithFirstName(firstName: String): CustomerBuilder {
        _firstName = firstName
        return this
    }

    public fun WithLastName(lastName: String): CustomerBuilder {
        _lastName = lastName
        return this
    }

    public fun Build(): Customer {
        return Customer(_firstName, _lastName)
    }
}

public class User {
    default object {
        public fun main() {
            val customer = CustomerBuilder().WithFirstName("Homer").WithLastName("Simpson").Build()
            System.out.println(customer.firstName)
            System.out.println(customer.lastName)
        }
    }
}