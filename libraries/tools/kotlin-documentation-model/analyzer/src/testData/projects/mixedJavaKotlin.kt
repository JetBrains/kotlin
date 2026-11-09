// MODULE: main
// FILE: example/JavaRepository.java
package example;

public interface JavaRepository<T> {
    T find(String name);
    void save(T value);
}

// FILE: example/JavaBean.java
package example;

public class JavaBean {
    public static final int DEFAULT_COUNT = 3;
    public int count;
    private String name;

    public JavaBean(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

// FILE: example/KotlinApi.kt
package example

class KotlinRecord(val name: String)

open class KotlinBase {
    open fun describe(record: KotlinRecord): String = record.name
}

class KotlinRepository : JavaRepository<KotlinRecord> {
    override fun find(name: String): KotlinRecord = KotlinRecord(name)

    override fun save(value: KotlinRecord) {}
}

class KotlinBean(name: String) : JavaBean(name) {
    fun rename(value: String): String {
        this.name = value
        count = JavaBean.DEFAULT_COUNT
        return this.name
    }
}

fun JavaBean.toRecord(): KotlinRecord = KotlinRecord(name)

// FILE: example/JavaConsumer.java
package example;

public class JavaConsumer extends KotlinBase {
    public KotlinRecord load(JavaRepository<KotlinRecord> repository) {
        KotlinRecord record = repository.find("sample");
        repository.save(record);
        return record;
    }

    @Override
    public String describe(KotlinRecord record) {
        return record.getName();
    }

    public KotlinRecord convert(JavaBean bean) {
        return KotlinApiKt.toRecord(bean);
    }
}
