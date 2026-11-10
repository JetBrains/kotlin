import Inheritance
import Testing

@Test
func kotlinClassInheritingInterfaceDefault() throws {
    #expect(speak(s: KotlinSpeakerImpl()) == "default")
    #expect(KotlinSpeakerImpl().speak() == "default")
}

@Test
func swiftSubclassInheritsKotlinInheritedDefault() throws {
    class SwiftImpl: KotlinSpeakerImpl {}

    #expect(speak(s: SwiftImpl()) == "default")
    #expect(SwiftImpl().speak() == "default")
    #expect((SwiftImpl() as any Speaker).speak() == "default")
}

@Test
func swiftSubclassOverridesKotlinInheritedDefault() throws {
    class SwiftOverride: KotlinSpeakerImpl {
        override func speak() -> String { "swift" }
    }

    #expect(speak(s: SwiftOverride()) == "swift")
    #expect(SwiftOverride().speak() == "swift")
    #expect((SwiftOverride() as any Speaker).speak() == "swift")
}

@Test
func swiftSubclassOfKotlinOverrideOfInheritedDefault() throws {
    class SwiftImpl: KotlinOverridingSpeaker {}
    class SwiftOverride: KotlinOverridingSpeaker {
        override func speak() -> String { "swift" }
    }

    #expect(speak(s: KotlinOverridingSpeaker()) == "kotlin-override")
    #expect(speak(s: SwiftImpl()) == "kotlin-override")
    #expect(speak(s: SwiftOverride()) == "swift")
}

@Test
func swiftSubclassInheritsDefaultOfSuperInterface() throws {
    class SwiftImpl: LoudSpeakerImpl {}
    class SwiftOverride: LoudSpeakerImpl {
        override func speak() -> String { "swift" }
    }

    #expect(speak(s: SwiftImpl()) == "default")
    #expect(speak(s: SwiftOverride()) == "swift")
}
