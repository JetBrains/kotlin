Я сосредоточился на анализе потенциально опасных объявлений, потому как анализ тел функций мне показался трудоемким и сложным

Кажется, обработал все пять кейсов из последнего параграфа https://kotlinlang.org/docs/reference/native/concurrency.html
Если этого маловато, то буду думать еще

2) ThreadLocal (USELESS_THREAD_LOCAL)
    - только глобальные объявления
    - `val` или `var` с делегатом
3) SharedImuutable (USELESS_SHARED_IMMUTABLE)
    - только глобальные объявления
    - `val` или `var` с делегатом
4) Singleton (MUTABLE_SINGLETON):
    - содержит мутабельное свойство
    - нет делегата
    - не помечен @ThreadLocal
    - есть backing field
5) Enum (MUTABLE_ENUM):
    - содержит мутабельное свойство
    - нет делегата
    - ThreadLocal не рассматриваем, enum frozen всегда
    - есть backing field 

## Могу подумать, как сделать, если внесененных изменений маловато:
### Мутирование объекта после вызова у него `freeze()`
Не очень понятно, как быть с ветвлением кода, но, возможно, можно написать инспекцию, которая проверяет отсутствие мутаций объекта после вызова у него `freeze()`

### Присваивание backing field в сеттере enum не обрабатывается MUTABLE_ENUM, хотя приведет к крашу
Код, проверяющий отсутствие наличие записи в `field`, находится в `SetterBackingFieldAssignmentInspection`. Условие нужно инвертировать. 
Я не уверен, как переиспользовать этот код в native, не правя при этом конфиг градла. Можно просто скопировать, но я бы уточнил этот момент
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
Показалось сложным, отложил в сторону. Вероятно, я бы рекурсивно двигался по инструкциям в lazy property body и искал бы цикл ссылок

P.S. Спасибо, за интересное задание :)
