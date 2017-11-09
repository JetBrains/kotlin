package test

@javax.persistence.Entity
class Test(a: String, b: Int)

@javax.persistence.Embeddable
class Test2(a: String, b: Int)