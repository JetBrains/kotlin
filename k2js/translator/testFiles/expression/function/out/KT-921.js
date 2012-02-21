var classes = function(){
  var tmp$0 = Kotlin.Class.create({initialize:function(){
    this.$items = new Kotlin.ArrayList;
  }
  , get_items:function(){
    return this.$items;
  }
  , add:function(item){
    {
      this.get_items().add(item);
    }
  }
  , remove:function(item){
    {
      this.get_items().remove(item);
    }
  }
  , view:function(lifetime, viewer){
    {
      var tmp$0;
      {
        tmp$0 = this.get_items().iterator();
        while (tmp$0.hasNext()) {
          var item = tmp$0.next();
          {
            viewer(lifetime, item);
          }
        }
      }
    }
  }
  });
  var tmp$1 = Kotlin.Class.create({initialize:function(){
    this.$attached = new Kotlin.ArrayList;
  }
  , get_attached:function(){
    return this.$attached;
  }
  , attach:function(action){
    {
      this.get_attached().add(action);
    }
  }
  , close_0:function(){
    {
      var tmp$0;
      {
        tmp$0 = this.get_attached().iterator();
        while (tmp$0.hasNext()) {
          var x = tmp$0.next();
          {
            x();
          }
        }
      }
      this.get_attached().clear();
    }
  }
  });
  return {Lifetime:tmp$1, Viewable:tmp$0};
}
();
var Anonymous = Kotlin.Namespace.create({initialize:function(){
}
, lifetime:function(body){
  {
    var l = new Anonymous.Lifetime;
    body(l);
    l.close_0();
  }
}
, Dump:function(items){
  {
    var tmp$0;
    {
      tmp$0 = items.iterator();
      while (tmp$0.hasNext()) {
        var item = tmp$0.next();
        {
          Kotlin.print(jet.toString(item) + ', ');
        }
      }
    }
    Kotlin.println();
  }
}
, main:function(args){
  {
    var v = new Anonymous.Viewable;
    var x = new Kotlin.ArrayList;
    v.add(1);
    v.add(2);
    Anonymous.lifetime(function(it){
      {
        v.view(it, function(itemLifetime, item){
          {
            x.add(item);
            Anonymous.Dump(x);
            itemLifetime.attach(function(){
              {
                x.remove(item);
                Anonymous.Dump(x);
              }
            }
            );
          }
        }
        );
      }
    }
    );
  }
}
}, {Lifetime:classes.Lifetime, Viewable:classes.Viewable});
Anonymous.initialize();
