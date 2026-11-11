import KotlinRuntime
import KotlinRuntimeSupport
import StarProjectionBounds
import Testing

@Test
func starProjectionOverNotNullBound() throws {
    #expect(describeNotNull(x: NotNull(value: "a")) == "not-null:a")
    #expect(describeNotNull(x: NotNull(value: Int32(1))) == "not-null:1")
    #expect(describeOptional(x: NotNull(value: "a")) == "optional:a")
    #expect(describeOptional(x: Optional<NotNull<String>>.none) == "optional:null")
    #expect(NotNull(value: "a").describeAsReceiver() == "receiver:a")
    #expect(Describer().describe(x: NotNull(value: "a")) == "member:a")
}

@Test
func starProjectionOverClassBound() throws {
    #expect(describeOfAnimal(x: OfAnimal(value: Dog(name: "Rex"))) == "animal:Rex")
    #expect(describeOut(x: OfAnimal(value: Dog(name: "Rex"))) == "out:Rex")
    #expect(describeOfNullableAnimal(x: OfNullableAnimal(value: Optional.some(Dog(name: "Rex")))) == "nullable-animal:Rex")
    #expect(describeOfNullableAnimal(x: OfNullableAnimal(value: Optional<Dog>.none)) == "nullable-animal:null")
}

@Test
func starProjectionOverTwoParameters() throws {
    let two = Two(first: "a", second: Dog(name: "Rex"))
    #expect(describeBothStars(x: two) == "stars:a/Rex")
    #expect(describeTwo(x: Two(first: "a", second: Animal(name: "Tom"))) == "two:a/Tom")
}

@Test
func starProjectionInReturnPosition() throws {
    #expect(describeNotNull(x: starNotNull()) == "not-null:made in Kotlin")
    #expect(starOfAnimal().value.name == "Rex")
    #expect(describeOfAnimal(x: starOfAnimal()) == "animal:Rex")
}
