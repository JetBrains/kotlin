package consumer;

import producer.ProducerJvmKt;

public final class Consumer {
    private Consumer() {}

    public static String consume() {
        return ProducerJvmKt.jvmGreeting();
    }
}
