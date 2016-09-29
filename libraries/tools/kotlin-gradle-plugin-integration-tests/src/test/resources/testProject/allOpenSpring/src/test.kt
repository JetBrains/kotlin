package test

import org.springframework.stereotype.Component

@Component
class OpenClass {
    fun method() {}
}

class ClosedClass {
    fun method() {}
}