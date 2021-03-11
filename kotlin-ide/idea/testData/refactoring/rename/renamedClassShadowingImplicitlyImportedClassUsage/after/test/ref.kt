package ref

import lib.LibType

class String {}

class Referrer1 { fun method(p1a: String, p1b: kotlin.String) {} }
class Referrer2 { fun method(p2a: String, p2b: LibType) {} }