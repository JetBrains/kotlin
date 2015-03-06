class Test {
    default object {
        public fun foo(args: Array<String>): Int {
            return args.size
        }
    }
}