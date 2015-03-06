class Test {
    default object {
        public fun toFileSystemSafeName(name: String): String {
            val size = name.length()
            return name
        }
    }
}