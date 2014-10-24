// ERROR: This type is final, so it cannot be inherited from
class Base(name: String)

class One(name: String, second: String) : Base(name)