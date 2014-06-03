class C private(arg1: Int, arg2: Int, arg3: Int) {
    class object {

        private fun init(arg1: Int, arg2: Int): C {
            val __ = C(arg1, arg2, 0)
            return __
        }

        public fun init(arg1: Int): C {
            val __ = C(arg1, 0, 0)
            return __
        }
    }
}