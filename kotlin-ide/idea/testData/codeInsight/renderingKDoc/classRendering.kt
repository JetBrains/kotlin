
/**
 * Hello darling
 * @author DarkWing Duck
 */
class testClass

/**
 * Hello darling outher
 * @author Morgana Macawber
 */
class outherClass {
    /**
     * Hello darling inner
     * @author Launchpad McQuack
     */
    inner class innerClass {

    }
}

// RENDER: <div class='content'><p>Hello darling</p></div><table class='sections'><tr><td valign='top' class='section'><p>Author:</td><td valign='top'>DarkWing Duck</td></table>
// RENDER: <div class='content'><p>Hello darling outher</p></div><table class='sections'><tr><td valign='top' class='section'><p>Author:</td><td valign='top'>Morgana Macawber</td></table>
// RENDER: <div class='content'><p>Hello darling inner</p></div><table class='sections'><tr><td valign='top' class='section'><p>Author:</td><td valign='top'>Launchpad McQuack</td></table>