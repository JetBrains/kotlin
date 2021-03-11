// INFO: {"checked": "true"}
interface X

// INFO: {"checked": "true"}
interface Y

// INFO: {"checked": "true"}
interface Z

open class <caret>A : X, Y, Z

class B : A(), Z