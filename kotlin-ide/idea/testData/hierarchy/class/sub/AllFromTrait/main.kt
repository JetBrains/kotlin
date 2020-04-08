interface <caret>A

open class B: A

interface T: A

open class C: B()

interface U: T

object O1: A

object O2: B()

object O3: C()

object O4: T

object O5: U
