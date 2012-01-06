import java.util.*

class Lifetime() {
  val attached = ArrayList< Function0<Unit> >()
  
  public fun attach(action : ()->Unit)
  {
    attached.add(action)
  }
  
  fun close()
  {
    for(x in attached) x()
    attached.clear()
  }
}

public class Viewable<T>()
{
  val items = ArrayList<T>()
  
  fun add(item:T)
  {
    items.add(item)
  }
  
  fun remove(item:T)
  {
    items.remove(item)
  }
  
  fun view(lifetime: Lifetime, viewer: (itemLifetime:Lifetime, item:T) -> Unit)
  {
    for(item in items)
      viewer(lifetime, item)
  }
}

fun lifetime(body: (Lifetime)->Unit)
{
  val l = Lifetime()
  body(l)
  l.close()
}

fun<T> Dump(items:ArrayList<T>)
{
  for(item in items)
   System.out?.print(item.toString() + ", ")
  System.out?.println()
}

fun main(args:Array<String>)
{
  val v = Viewable<Int>()
  val x = ArrayList<Int>()
  v.add(1)
  v.add(2)
  lifetime( 
  {
    v.view(it, {(itemLifetime, item)->
      x.add(item)
      Dump(x)          
      itemLifetime.attach { x.remove(item); Dump(x) }
    })
  })
}