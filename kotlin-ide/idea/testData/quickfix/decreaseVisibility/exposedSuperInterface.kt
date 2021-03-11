// "Make 'Derived' internal" "true"
// ACTION: Make 'Outer' public

import Outer.Base

internal class Outer {
    interface Base
}

class Container {
    interface Derived : <caret>Base
}