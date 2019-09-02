class Planet(val name: String, val diameter: Double)

expect val propertyWithInferredType1: Int
expect val propertyWithInferredType2: String
expect val propertyWithInferredType3: String
expect val propertyWithInferredType4: Nothing?
expect val propertyWithInferredType5: Planet

typealias C = Planet

expect val property1: Int
expect val property2: String
expect val property3: Planet
expect val property6: Planet
expect val property7: C
