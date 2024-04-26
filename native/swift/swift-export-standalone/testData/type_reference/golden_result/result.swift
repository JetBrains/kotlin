import KotlinRuntime

public class Class_without_package : KotlinRuntime.KotlinBase {
    public class INNER_CLASS : KotlinRuntime.KotlinBase {
        public override init() {
            fatalError()
        }
    }
    public class INNER_OBJECT : KotlinRuntime.KotlinBase {
        public static var shared: Swift.Int32 {
            get {
                fatalError()
            }
        }
        private override init() {
            fatalError()
        }
    }
    public override init() {
        fatalError()
    }
}
public class Demo : KotlinRuntime.KotlinBase {
    public class INNER_CLASS : KotlinRuntime.KotlinBase {
        public override init() {
            fatalError()
        }
    }
    public class INNER_OBJECT : KotlinRuntime.KotlinBase {
        public static var shared: Swift.Int32 {
            get {
                fatalError()
            }
        }
        private override init() {
            fatalError()
        }
    }
    public var arg1: main.Class_without_package {
        get {
            fatalError()
        }
    }
    public var arg2: main.namespace.deeper.Class_with_package {
        get {
            fatalError()
        }
    }
    public var arg3: main.Object_without_package {
        get {
            fatalError()
        }
    }
    public var arg4: main.namespace.deeper.Object_with_package {
        get {
            fatalError()
        }
    }
    public init(
        arg1: main.Class_without_package,
        arg2: main.namespace.deeper.Class_with_package,
        arg3: main.Object_without_package,
        arg4: main.namespace.deeper.Object_with_package
    ) {
        fatalError()
    }
    public func combine(
        arg1: main.Class_without_package,
        arg2: main.namespace.deeper.Class_with_package,
        arg3: main.Object_without_package,
        arg4: main.namespace.deeper.Object_with_package
    ) -> main.Demo {
        fatalError()
    }
    public func combine_inner_classses(
        arg1: main.Class_without_package.INNER_CLASS,
        arg2: main.namespace.deeper.Class_with_package.INNER_CLASS,
        arg3: main.Object_without_package.INNER_CLASS,
        arg4: main.namespace.deeper.Object_with_package.INNER_CLASS
    ) -> main.Demo.INNER_CLASS {
        fatalError()
    }
    public func combine_inner_objects(
        arg1: main.Class_without_package.INNER_OBJECT,
        arg2: main.namespace.deeper.Class_with_package.INNER_OBJECT,
        arg3: main.Object_without_package.INNER_OBJECT,
        arg4: main.namespace.deeper.Object_with_package.INNER_OBJECT
    ) -> main.Demo.INNER_OBJECT {
        fatalError()
    }
}
public class Object_without_package : KotlinRuntime.KotlinBase {
    public class INNER_CLASS : KotlinRuntime.KotlinBase {
        public override init() {
            fatalError()
        }
    }
    public class INNER_OBJECT : KotlinRuntime.KotlinBase {
        public static var shared: Swift.Int32 {
            get {
                fatalError()
            }
        }
        private override init() {
            fatalError()
        }
    }
    public static var shared: Swift.Int32 {
        get {
            fatalError()
        }
    }
    private override init() {
        fatalError()
    }
}
public func combine(
    arg1: main.Class_without_package,
    arg2: main.namespace.deeper.Class_with_package,
    arg3: main.Object_without_package,
    arg4: main.namespace.deeper.Object_with_package
) -> Swift.Void {
    fatalError()
}
public func produce_class() -> main.Class_without_package {
    fatalError()
}
public func produce_class_wp() -> main.namespace.deeper.Class_with_package {
    fatalError()
}
public func produce_object() -> main.Object_without_package {
    fatalError()
}
public func produce_object_wp() -> main.namespace.deeper.Object_with_package {
    fatalError()
}
public func recieve_class(
    arg: main.Class_without_package
) -> Swift.Void {
    fatalError()
}
public func recieve_class_wp(
    arg: main.namespace.deeper.Class_with_package
) -> Swift.Void {
    fatalError()
}
public func recieve_object(
    arg: main.Object_without_package
) -> Swift.Void {
    fatalError()
}
public func recieve_object_wp(
    arg: main.namespace.deeper.Object_with_package
) -> Swift.Void {
    fatalError()
}
public extension main.namespace.deeper {
    public class Class_with_package : KotlinRuntime.KotlinBase {
        public class INNER_CLASS : KotlinRuntime.KotlinBase {
            public override init() {
                fatalError()
            }
        }
        public class INNER_OBJECT : KotlinRuntime.KotlinBase {
            public static var shared: Swift.Int32 {
                get {
                    fatalError()
                }
            }
            private override init() {
                fatalError()
            }
        }
        public override init() {
            fatalError()
        }
    }
    public class Object_with_package : KotlinRuntime.KotlinBase {
        public class INNER_CLASS : KotlinRuntime.KotlinBase {
            public override init() {
                fatalError()
            }
        }
        public class INNER_OBJECT : KotlinRuntime.KotlinBase {
            public static var shared: Swift.Int32 {
                get {
                    fatalError()
                }
            }
            private override init() {
                fatalError()
            }
        }
        public static var shared: Swift.Int32 {
            get {
                fatalError()
            }
        }
        private override init() {
            fatalError()
        }
    }
}
public enum namespace {
    public enum deeper {
    }
}
