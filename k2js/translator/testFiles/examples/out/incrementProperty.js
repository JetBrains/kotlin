var classes = function(){
  var tmp$0 = Kotlin.Class.create({initialize:function(){
    this.$vitality = 10000;
  }
  , get_vitality:function(){
    return this.$vitality;
  }
  , set_vitality:function(tmp$0){
    this.$vitality = tmp$0;
  }
  , increaseVitality:function(delta){
    {
      this.set_vitality(this.get_vitality() + delta);
      if (this.get_vitality() > 65535)
        this.set_vitality(65535);
    }
  }
  });
  return {Slot:tmp$0};
}
();
var Anonymous = Kotlin.Namespace.create({initialize:function(){
}
, box:function(){
  {
    var s = new Anonymous.Slot;
    s.increaseVitality(1000);
    if (s.get_vitality() == 11000)
      return 'OK';
    else 
      return 'fail';
  }
}
}, {Slot:classes.Slot});
Anonymous.initialize();
