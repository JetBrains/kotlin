package example

import greeteer.Greeter

class ParticularClock(private val clockDay: ClockDays) : Clock() {

    /**
     * Rings bell [times]
     */
    fun ringBell(times: Int) {}

    /**
     * Uses provider [greeter]
     */
    fun useGreeter(greeter: Greeter) {

    }

    /**
     * Day of the week
     */
    override fun getDayOfTheWeek() = clockDay.name
}

/**
 * A sample extension function
 * When $a \ne 0$, there are two solutions to \(ax^2 + bx + c = 0\) and they are $$x = {-b \pm \sqrt{b^2-4ac} \over 2a}.$$
 * @usesMathJax
 */
fun Clock.extensionFun() {

}