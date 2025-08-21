@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_repeating_conformances
import KotlinRuntime
import KotlinRuntimeSupport

public extension ExportedKotlinPackages.repeating_conformances.Barable where Self : KotlinRuntimeSupport._KotlinBridged {
}
public extension ExportedKotlinPackages.repeating_conformances.Foeble where Self : KotlinRuntimeSupport._KotlinBridged {
}
public extension ExportedKotlinPackages.repeating_conformances {
    public protocol Barable: KotlinRuntime.KotlinBase, ExportedKotlinPackages.repeating_conformances.Foeble {
    }
    public protocol Foeble: KotlinRuntime.KotlinBase {
    }
    open class Child1: ExportedKotlinPackages.repeating_conformances.Parent1 {
        public override init() {
            let __kt = repeating_conformances_Child1_init_allocate()
            super.init(__externalRCRef: __kt)
            repeating_conformances_Child1_init_initialize__TypesOfArguments__Swift_UInt__(__kt)
        }
        package override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
    open class Child2: ExportedKotlinPackages.repeating_conformances.Parent2 {
        public override init() {
            let __kt = repeating_conformances_Child2_init_allocate()
            super.init(__externalRCRef: __kt)
            repeating_conformances_Child2_init_initialize__TypesOfArguments__Swift_UInt__(__kt)
        }
        package override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
    open class Child3: ExportedKotlinPackages.repeating_conformances.Parent3 {
        public override init() {
            let __kt = repeating_conformances_Child3_init_allocate()
            super.init(__externalRCRef: __kt)
            repeating_conformances_Child3_init_initialize__TypesOfArguments__Swift_UInt__(__kt)
        }
        package override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
    open class Child4: ExportedKotlinPackages.repeating_conformances.Parent4 {
        public override init() {
            let __kt = repeating_conformances_Child4_init_allocate()
            super.init(__externalRCRef: __kt)
            repeating_conformances_Child4_init_initialize__TypesOfArguments__Swift_UInt__(__kt)
        }
        package override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
    open class Child5: ExportedKotlinPackages.repeating_conformances.Parent5 {
        public override init() {
            let __kt = repeating_conformances_Child5_init_allocate()
            super.init(__externalRCRef: __kt)
            repeating_conformances_Child5_init_initialize__TypesOfArguments__Swift_UInt__(__kt)
        }
        package override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
    open class GrandChild1: ExportedKotlinPackages.repeating_conformances.Child1 {
        public override init() {
            let __kt = repeating_conformances_GrandChild1_init_allocate()
            super.init(__externalRCRef: __kt)
            repeating_conformances_GrandChild1_init_initialize__TypesOfArguments__Swift_UInt__(__kt)
        }
        package override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
    open class GrandChild2: ExportedKotlinPackages.repeating_conformances.Child2 {
        public override init() {
            let __kt = repeating_conformances_GrandChild2_init_allocate()
            super.init(__externalRCRef: __kt)
            repeating_conformances_GrandChild2_init_initialize__TypesOfArguments__Swift_UInt__(__kt)
        }
        package override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
    open class GrandChild3: ExportedKotlinPackages.repeating_conformances.Child3 {
        public override init() {
            let __kt = repeating_conformances_GrandChild3_init_allocate()
            super.init(__externalRCRef: __kt)
            repeating_conformances_GrandChild3_init_initialize__TypesOfArguments__Swift_UInt__(__kt)
        }
        package override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
    open class GrandChild4: ExportedKotlinPackages.repeating_conformances.Child4, ExportedKotlinPackages.repeating_conformances.Barable {
        public override init() {
            let __kt = repeating_conformances_GrandChild4_init_allocate()
            super.init(__externalRCRef: __kt)
            repeating_conformances_GrandChild4_init_initialize__TypesOfArguments__Swift_UInt__(__kt)
        }
        package override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
    open class GrandChild5: ExportedKotlinPackages.repeating_conformances.Child5, ExportedKotlinPackages.repeating_conformances.Barable, ExportedKotlinPackages.repeating_conformances.Foeble {
        public override init() {
            let __kt = repeating_conformances_GrandChild5_init_allocate()
            super.init(__externalRCRef: __kt)
            repeating_conformances_GrandChild5_init_initialize__TypesOfArguments__Swift_UInt__(__kt)
        }
        package override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
    open class Parent1: KotlinRuntime.KotlinBase, ExportedKotlinPackages.repeating_conformances.Foeble, KotlinRuntimeSupport._KotlinBridged {
        public override init() {
            let __kt = repeating_conformances_Parent1_init_allocate()
            super.init(__externalRCRef: __kt)
            repeating_conformances_Parent1_init_initialize__TypesOfArguments__Swift_UInt__(__kt)
        }
        package override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
    open class Parent2: KotlinRuntime.KotlinBase, ExportedKotlinPackages.repeating_conformances.Foeble, KotlinRuntimeSupport._KotlinBridged {
        public override init() {
            let __kt = repeating_conformances_Parent2_init_allocate()
            super.init(__externalRCRef: __kt)
            repeating_conformances_Parent2_init_initialize__TypesOfArguments__Swift_UInt__(__kt)
        }
        package override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
    open class Parent3: KotlinRuntime.KotlinBase, ExportedKotlinPackages.repeating_conformances.Barable, KotlinRuntimeSupport._KotlinBridged {
        public override init() {
            let __kt = repeating_conformances_Parent3_init_allocate()
            super.init(__externalRCRef: __kt)
            repeating_conformances_Parent3_init_initialize__TypesOfArguments__Swift_UInt__(__kt)
        }
        package override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
    open class Parent4: KotlinRuntime.KotlinBase, ExportedKotlinPackages.repeating_conformances.Foeble, KotlinRuntimeSupport._KotlinBridged {
        public override init() {
            let __kt = repeating_conformances_Parent4_init_allocate()
            super.init(__externalRCRef: __kt)
            repeating_conformances_Parent4_init_initialize__TypesOfArguments__Swift_UInt__(__kt)
        }
        package override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
    open class Parent5: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
        public override init() {
            let __kt = repeating_conformances_Parent5_init_allocate()
            super.init(__externalRCRef: __kt)
            repeating_conformances_Parent5_init_initialize__TypesOfArguments__Swift_UInt__(__kt)
        }
        package override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
}
