package ref

import lib.LibType

class LibType {}

class Referrer1 { fun method(p1a: ref.LibType, p1b: String) {} }
class Referrer2 { fun method(p2a: ref.LibType, p2b: LibType) {} }