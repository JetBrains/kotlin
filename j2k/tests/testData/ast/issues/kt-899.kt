class Test {
    public fun printNumbers(number: Int) {
        for (i in 2..Math.sqrt(number.toDouble()) + 1 - 1)
            System.out.println(i)
    }
}