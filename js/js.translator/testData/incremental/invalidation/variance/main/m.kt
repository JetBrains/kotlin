class BoxProducer: Producer<String> {
    override fun produce() = "test"
}

class BoxConsumer: Consumer<String> {
    override fun consume(s: String) = s.length
}

fun test(producer: Producer<String>, consumer: Consumer<String>) = consumer.consume(producer.produce())

fun box(): String {
    if (test(BoxProducer(), BoxConsumer()) != 4) return "Fail"
    return "OK"
}
