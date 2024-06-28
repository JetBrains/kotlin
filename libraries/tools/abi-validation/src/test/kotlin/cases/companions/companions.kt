package cases.companions


object PublicClasses {
    class PublicCompanion {
        companion object
    }

    class ProtectedCompanion {
        protected companion object
    }

    abstract class AbstractProtectedCompanion {
        protected companion object
    }

    class InternalCompanion {
        internal companion object
    }

    class PublishedApiCompanion {
        @PublishedApi
        internal companion object
    }

    class PrivateCompanion {
        private companion object
    }
}

object PublicInterfaces {
    interface PublicCompanion {
        companion object
    }

    interface PrivateCompanion {
        private companion object
    }
}

object InternalClasses {
    internal class PublicCompanion {
        companion object
    }

    internal class ProtectedCompanion {
        protected companion object
    }

    internal abstract class AbstractProtectedCompanion {
        protected companion object
    }

    internal class InternalCompanion {
        internal companion object
    }

    internal class PrivateCompanion {
        private companion object
    }
}

object PublishedApiClasses {
    @PublishedApi
    internal class PublicCompanion {
        companion object
    }

    @PublishedApi
    internal class ProtectedCompanion {
        protected companion object
    }

    @PublishedApi
    internal abstract class AbstractProtectedCompanion {
        protected companion object
    }

    @PublishedApi
    internal class InternalCompanion {
        internal companion object
    }

    @PublishedApi
    internal class PublishedApiCompanion {
        @PublishedApi
        internal companion object
    }

    @PublishedApi
    internal class PrivateCompanion {
        private companion object
    }
}

object InternalInterfaces {
    internal interface PublicCompanion {
        companion object
    }

    internal interface PrivateCompanion {
        private companion object
    }
}

object PrivateClasses {
    private class PublicCompanion {
        companion object
    }

    private class ProtectedCompanion {
        protected companion object
    }

    private abstract class AbstractProtectedCompanion {
        protected companion object
    }

    private class InternalCompanion {
        internal companion object
    }

    private class PublishedApiCompanion {
        @PublishedApi
        internal companion object
    }

    private class PrivateCompanion {
        private companion object
    }
}

object PrivateInterfaces {
    private interface PublicCompanion {
        companion object
    }

    private interface PrivateCompanion {
        private companion object
    }
}

@PrivateApi
annotation class PrivateApi


class FilteredCompanionObjectHolder private constructor() {
    @PrivateApi
    companion object {
        val F: Int = 42
    }
}

class FilteredNamedCompanionObjectHolder private constructor() {
    @PrivateApi
    companion object Named {
        val F: Int = 42
    }
}

class FilteredCompanionProperties private constructor() {
    companion object {
        public val F1: Int = 1
        public const val F2: Int = 2
        private val F3: Int = 3
        private const val F4: Int = 4
        @PrivateApi
        val F5: Int = 5
        @PrivateApi
        const val F6: Int = 6
        @PrivateApi
        const val F7: Int = 7
    }
}

class FilteredCompanionFunctions private constructor() {
    companion object {
        public fun f1(): Int = 1
        private fun f2(): Int = 2
        @PrivateApi
        public fun f3(): Int = 3
    }
}
