package ref

import lib.LibType

class /*rename*/CustomType {}

class Referrer1 { fun method(p1a: CustomType, p1b: String) {} }
class Referrer2 { fun method(p2a: CustomType, p2b: LibType) {} }