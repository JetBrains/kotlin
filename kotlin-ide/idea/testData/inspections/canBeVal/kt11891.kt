fun main(args: Array<String?>) {
    var a: String?

    if (args.size == 1) {
        a = args[0]
    }
    else {
        a  = args.toString()
    }

    if (a != null && a.equals("cde")) return
}
