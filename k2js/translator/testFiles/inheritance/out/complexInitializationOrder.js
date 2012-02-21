var classes = function(){
  var tmp$0 = Kotlin.Class.create({initialize:function(){
    this.$order = '';
    {
      this.set_order(this.get_order() + 'A');
    }
  }
  , get_order:function(){
    return this.$order;
  }
  , set_order:function(tmp$0){
    this.$order = tmp$0;
  }
  });
  var tmp$1 = Kotlin.Class.create(tmp$0, {initialize:function(){
    this.super_init();
    {
      this.set_order(this.get_order() + 'E');
    }
  }
  });
  var tmp$2 = Kotlin.Class.create(tmp$0, {initialize:function(){
    this.super_init();
    {
      this.set_order(this.get_order() + 'B');
    }
  }
  });
  var tmp$3 = Kotlin.Class.create(tmp$2, {initialize:function(){
    this.super_init();
    {
      this.set_order(this.get_order() + 'D');
    }
  }
  });
  var tmp$4 = Kotlin.Class.create(tmp$2, {initialize:function(){
    this.super_init();
    {
      this.set_order(this.get_order() + 'C');
    }
  }
  });
  return {E:tmp$1, B:tmp$2, D:tmp$3, C:tmp$4, A:tmp$0};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
}
, box:function(){
  {
    return (new foo.C).get_order() == 'ABC' && (new foo.D).get_order() == 'ABD' && (new foo.E).get_order() == 'AE';
  }
}
}, {A:classes.A, B:classes.B, C:classes.C, D:classes.D, E:classes.E});
foo.initialize();
