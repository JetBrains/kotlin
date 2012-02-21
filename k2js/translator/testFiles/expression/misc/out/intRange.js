var classes = function(){
  var tmp$0 = Kotlin.Class.create({initialize:function(start, size, reversed){
    this.$start = start;
    this.$size = size;
    this.$reversed = reversed;
  }
  , get_start:function(){
    return this.$start;
  }
  , get_size:function(){
    return this.$size;
  }
  , get_reversed:function(){
    return this.$reversed;
  }
  , get_end:function(){
    var tmp$0;
    if (this.get_reversed())
      tmp$0 = this.get_start() - this.get_size() + 1;
    else 
      tmp$0 = this.get_start() + this.get_size() - 1;
    {
      return tmp$0;
    }
  }
  , contains:function(number){
    {
      if (this.get_reversed()) {
        return number <= this.get_start() && number > this.get_start() - this.get_size();
      }
       else {
        return number >= this.get_start() && number < this.get_start() + this.get_size();
      }
    }
  }
  , iterator:function(){
    {
      return new foo.RangeIterator(this.get_start(), this.get_size(), this.get_reversed());
    }
  }
  });
  var tmp$1 = Kotlin.Class.create({initialize:function(start, count, reversed){
    this.$start = start;
    this.$count = count;
    this.$reversed = reversed;
    this.$i = this.get_start();
  }
  , get_start:function(){
    return this.$start;
  }
  , get_count:function(){
    return this.$count;
  }
  , set_count:function(tmp$0){
    this.$count = tmp$0;
  }
  , get_reversed:function(){
    return this.$reversed;
  }
  , get_i:function(){
    return this.$i;
  }
  , set_i:function(tmp$0){
    this.$i = tmp$0;
  }
  , next:function(){
    {
      this.set_count(this.get_count() - 1);
      if (this.get_reversed()) {
        this.set_i(this.get_i() - 1);
        return this.get_i() + 1;
      }
       else {
        this.set_i(this.get_i() + 1);
        return this.get_i() - 1;
      }
    }
  }
  , hasNext:function(){
    {
      return this.get_count() > 0;
    }
  }
  });
  return {RangeIterator:tmp$1, NumberRange:tmp$0};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
}
, box:function(){
  {
    return foo.testRange() && foo.testReversedRange();
  }
}
, testRange:function(){
  {
    var tmp$1;
    var tmp$0;
    var oneToFive = new foo.NumberRange(1, 4, false);
    if (oneToFive.contains(5))
      return false;
    if (oneToFive.contains(0))
      return false;
    if (oneToFive.contains(-100))
      return false;
    if (oneToFive.contains(10))
      return false;
    if (!oneToFive.contains(1))
      return false;
    if (!oneToFive.contains(2))
      return false;
    if (!oneToFive.contains(3))
      return false;
    if (!oneToFive.contains(4))
      return false;
    if (!(oneToFive.get_start() == 1))
      return false;
    if (!(oneToFive.get_size() == 4))
      return false;
    if (!(oneToFive.get_end() == 4))
      return false;
    var sum = 0;
    {
      tmp$0 = oneToFive.iterator();
      while (tmp$0.hasNext()) {
        var i = tmp$0.next();
        {
          sum += i;
        }
      }
    }
    {
      tmp$1 = oneToFive.iterator();
      while (tmp$1.hasNext()) {
        var i$0 = tmp$1.next();
        {
          Kotlin.print(i$0);
        }
      }
    }
    if (sum != 10)
      return false;
    return true;
  }
}
, testReversedRange:function(){
  {
    var tmp$1;
    var tmp$0;
    Kotlin.println('Testing reversed range.');
    var tenToFive = new foo.NumberRange(10, 5, true);
    if (tenToFive.contains(5))
      return false;
    if (tenToFive.contains(11))
      return false;
    if (tenToFive.contains(-100))
      return false;
    if (tenToFive.contains(1000))
      return false;
    if (!tenToFive.contains(6))
      return false;
    if (!tenToFive.contains(7))
      return false;
    if (!tenToFive.contains(8))
      return false;
    if (!tenToFive.contains(9))
      return false;
    if (!tenToFive.contains(10))
      return false;
    if (!(tenToFive.get_start() == 10))
      return false;
    if (!(tenToFive.get_size() == 5))
      return false;
    if (!(tenToFive.get_end() == 6))
      return false;
    {
      tmp$0 = tenToFive.iterator();
      while (tmp$0.hasNext()) {
        var i = tmp$0.next();
        {
          Kotlin.println(i);
        }
      }
    }
    var sum = 0;
    {
      tmp$1 = tenToFive.iterator();
      while (tmp$1.hasNext()) {
        var i$0 = tmp$1.next();
        {
          sum += i$0;
        }
      }
    }
    if (sum != 40) {
      return false;
    }
    return true;
  }
}
, main:function(args){
  {
    Kotlin.println(foo.box());
  }
}
}, {RangeIterator:classes.RangeIterator, NumberRange:classes.NumberRange});
foo.initialize();
