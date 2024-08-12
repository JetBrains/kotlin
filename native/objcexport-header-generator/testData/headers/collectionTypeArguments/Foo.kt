open class Foo<A>

class MapClass<Key, Value, KeyValueMap : MutableMap<Key, Value>>() : Foo<KeyValueMap>()
class ListClass<Item, ListParameter : List<Item>> : Foo<ListParameter>()
class SetClass<Item, SetParameter : Set<Item>> : Foo<SetParameter>()