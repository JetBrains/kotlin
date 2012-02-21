var classes = function(){
  return {};
}
();
var Anonymous = Kotlin.Namespace.create({initialize:function(){
}
, main:function(args){
  {
    if (Anonymous.isEmpty(args)) {
      Anonymous.printBottles(99);
    }
     else {
      var bottles = Kotlin.parseInt(args[0]);
      if (bottles != null) {
        Anonymous.printBottles(bottles);
      }
       else {
        Kotlin.println("You have passed '" + args[0] + "' as a number of bottles, " + 'but it is not a valid integral number');
      }
    }
  }
}
, printBottles:function(bottleCount){
  {
    if (bottleCount <= 0) {
      Kotlin.println('No bottles - no song');
      return;
    }
    Kotlin.println('The ' + '"' + Anonymous.bottlesOfBeer_0(bottleCount) + '"' + ' song' + '\n');
    var bottles = bottleCount;
    while (bottles > 0) {
      var bottlesOfBeer = Anonymous.bottlesOfBeer_0(bottles);
      Kotlin.print(bottlesOfBeer + ' on the wall, ' + bottlesOfBeer + '.' + '\n' + 'Take one down, pass it around, ');
      bottles--;
      Kotlin.println(Anonymous.bottlesOfBeer_0(bottles) + ' on the wall.' + '\n');
    }
    Kotlin.println('No more bottles of beer on the wall, no more bottles of beer.\n' + ('Go to the store and buy some more, ' + Anonymous.bottlesOfBeer_0(bottleCount) + ' on the wall.'));
  }
}
, bottlesOfBeer_0:function(count){
  var tmp$1;
  var tmp$0;
  for (tmp$0 = 0; tmp$0 < 3; ++tmp$0) {
    if (tmp$0 == 0)
      if (count == 0) {
        tmp$1 = 'no more bottles';
        break;
      }
    if (tmp$0 == 1)
      if (count == 1) {
        tmp$1 = '1 bottle';
        break;
      }
    if (tmp$0 == 2)
      tmp$1 = count + ' bottles';
  }
  {
    return tmp$1 + ' of beer';
  }
}
, isEmpty:function(receiver){
  {
    return receiver.length == 0;
  }
}
}, {});
Anonymous.initialize();
