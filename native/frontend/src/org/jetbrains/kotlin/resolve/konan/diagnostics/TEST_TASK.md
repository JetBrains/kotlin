Кажется, обработал все пять кейсов из последнего параграфа https://kotlinlang.org/docs/reference/native/concurrency.html
Если этого маловато, то буду думать еще

2) ThreadLocal
3) SharedImuutable(USELESS_SHARED_IMMUTABLE)
    - только глобальные объявления
    - `val` или `var с делегатом
4) Singleton(MUTABLE_SINGLETON):
    - содержит мутабельное свойство
    - нет делегата
    - не помечен @ThreadLocal
    - backing field не присваивается в сеттере
5) Enum(MUTABLE_ENUM):
    - содержит мутабельное свойство
    - нет делегата
    - ThreadLocal не рассматриваем, enum frozen всегда
    - backing field не присваивается в сеттере    

## Могу сделать, если внесененных изменений маловато:

### Присваивание backing field в сеттере enum не обрабатывается MUTABLE_ENUM, хотя приведет к крашу
Код, проверяющий наличие записи в `field`, находится в `SetterBackingFieldAssignmentInspection`. Я не уверен, как переиспользовать этот код в native, не правя при этом конфиг градла. Копировать не хочется
```
enum class EnumWithSetter {
    ONE;

    var fieldWithSetter: Int = 0
        set(value) {
            field = value
        }
}
```

### lazy values allowed, unless cyclic frozen structures were attempted to be created
Показалось сложным, отложил в сторону. Вероятно, я бы рекурсивно двигался по вызовам в lazy property body и искал бы цикл


