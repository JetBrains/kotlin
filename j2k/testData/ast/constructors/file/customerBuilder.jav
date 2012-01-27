package org.test.customer

class Customer {
  public final String _firstName;
  public final String _lastName;

  Customer(String first, String last) {
    doSmthBefore();
    _firstName = first;
    _lastName = last;
    doSmthAfter();
  }

  public String getFirstName() {
    return _firstName;
  }

  public String getLastName() {
    return _lastName;
  }

  private void doSmthBefore() {}
  private void doSmthAfter() {}
}

class CustomerBuilder {
  public String _firstName = "Homer";
  public String _lastName = "Simpson";

  public CustomerBuilder WithFirstName(String firstName) {
    _firstName = firstName;
    return this;
  }

  public CustomerBuilder WithLastName(String lastName) {
    _lastName = lastName;
    return this;
  }

  public Customer Build() {
    return new Customer(_firstName, _lastName);
  }
}

public class User {
  public static void main(Array[String] args) {
     Customer customer = new CustomerBuilder()
       .WithFirstName("Homer")
       .WithLastName("Simpson")
       .Build();
     System.out.println(customer.getFirstName());
     System.out.println(customer.getLastName());
  }
}