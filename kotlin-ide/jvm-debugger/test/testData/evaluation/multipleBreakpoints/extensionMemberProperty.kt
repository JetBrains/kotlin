package extensionMemberProperty

fun main(args: Array<String>) {
    MemberClass().testMember(ExtClass())
    MemberClass.testCompanion(ExtClass())
}

class MemberClass {
    fun testMember(extClass: ExtClass) {
        // EXPRESSION: extClass.testPublic
        // RESULT: 1: I
        //Breakpoint!
        extClass.testPublic

        with(extClass) {
            // EXPRESSION: testPublic
            // RESULT: 1: I
            //Breakpoint!
            testPublic
        }

        // EXPRESSION: extClass.testPrivate
        // RESULT: 1: I
        //Breakpoint!
        extClass.testPrivate

        with(extClass) {
            // EXPRESSION: testPrivate
            // RESULT: 1: I
            //Breakpoint!
            testPrivate
        }

        extClass.testExtMember()
    }

    fun ExtClass.testExtMember() {
        // EXPRESSION: testPublic
        // RESULT: 1: I
        //Breakpoint!
        testPublic

        // EXPRESSION: this.testPublic
        // RESULT: 1: I
        //Breakpoint!
        this.testPublic

        // EXPRESSION: testPrivate
        // RESULT: 1: I
        //Breakpoint!
        testPrivate

        // EXPRESSION: this.testPrivate
        // RESULT: 1: I
        //Breakpoint!
        this.testPrivate
    }

    public val ExtClass.testPublic: Int
        get() = a
    private val ExtClass.testPrivate: Int
        get() = a

    companion object {
        public val ExtClass.testCompPublic: Int
            get() = a
        private val ExtClass.testCompPrivate: Int
            get() = a

        fun testCompanion(extClass: ExtClass) {
            // EXPRESSION: extClass.testCompPublic
            // RESULT: 1: I
            //Breakpoint!
            extClass.testCompPublic

            with(extClass) {
                // EXPRESSION: testCompPublic
                // RESULT: 1: I
                //Breakpoint!
                testCompPublic
            }

            // EXPRESSION: extClass.testCompPrivate
            // RESULT: 1: I
            //Breakpoint!
            extClass.testCompPrivate

            with(extClass) {
                // EXPRESSION: testCompPrivate
                // RESULT: 1: I
                //Breakpoint!
                testCompPrivate
            }

            extClass.testExtCompanion()
        }

        fun ExtClass.testExtCompanion() {
            // EXPRESSION: testCompPublic
            // RESULT: 1: I
            //Breakpoint!
            testCompPublic

            // EXPRESSION: this.testCompPublic
            // RESULT: 1: I
            //Breakpoint!
            this.testCompPublic

            // EXPRESSION: testCompPrivate
            // RESULT: 1: I
            //Breakpoint!
            testCompPrivate

            // EXPRESSION: this.testCompPrivate
            // RESULT: 1: I
            //Breakpoint!
            this.testCompPrivate
        }
    }
}

class ExtClass {
    val a = 1
}