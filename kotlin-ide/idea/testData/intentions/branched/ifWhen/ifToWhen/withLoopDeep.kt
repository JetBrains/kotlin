fun test() {
    loop@ while (true) {
        for (i in -10..10) {
            <caret>if (i < 0) {
                if (i < -5) {
                    break
                } else {
                    continue@loop
                }
            } else {
                if (i == 0) {
                    i.hashCode()
                    break
                } else if (i > 5) {
                    i.hashCode()
                } else {
                    continue
                }
            }
        }
    }
}