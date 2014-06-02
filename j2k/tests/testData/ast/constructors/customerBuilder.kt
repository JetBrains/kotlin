package org.test.customer

open class Customer(public val _firstName: String?, public val _lastName: String?) {

    public open fun getFirstName(): String? {
        return _firstName
    }

    public open fun getLastName(): String? {
        return _lastName
    }

    private fun doSmthBefore() {
    }
    private fun doSmthAfter() {
    }

    {
        doSmthBefore()
        doSmthAfter()
    }
}

open class CustomerBuilder() {
    public var _firstName: String? = "Homer"
    public var _lastName: String? = "Simpson"

    public open fun WithFirstName(firstName: String?): CustomerBuilder? {
        _firstName = firstName
        return this
    }

    public open fun WithLastName(lastName: String?): CustomerBuilder? {
        _lastName = lastName
        return this
    }

    public open fun Build(): Customer? {
        return Customer(_firstName, _lastName)
    }
}

public open class User() {
    class object {
        public open fun main() {
            var customer: Customer? = CustomerBuilder().WithFirstName("Homer")?.WithLastName("Simpson")?.Build()
            System.out?.println(customer?.getFirstName())
            System.out?.println(customer?.getLastName())
        }
    }
}