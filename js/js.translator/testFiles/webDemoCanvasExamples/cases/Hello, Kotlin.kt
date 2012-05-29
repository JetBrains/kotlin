/*
  This example is just simple text floating around. If u are using chrome, there is a bug that spoil the visuals.
*/
package hello

import js.*
import html5.*
import jquery.*

val context = getContext()
val height = getCanvas().height
val width = getCanvas().width

// class representing a floating text
class HelloKotlin() {
  var relX = 0.2 + 0.2 * Math.random()
  var relY = 0.4 + 0.2 * Math.random()

  val absX : Double
    get() = (relX * width)
  val absY : Double
    get() = (relY * height)

  var relXVelocity = randomVelocity()
  var relYVelocity = randomVelocity()


  val message = "Hello, Kotlin!"
  val textHeightInPixels = 20
  {
    context.font = "bold ${textHeightInPixels}px Georgia, serif"
  }
  val textWidthInPixels = context.measureText(message).width

  fun draw() {
    context.save()
    move()
    // if you using chrome chances are good you wont see the shadow
    context.shadowColor = "#000000"
    context.shadowBlur = 5.0
    context.shadowOffsetX = -4.0
    context.shadowOffsetY = 4.0
    context.fillStyle = "rgb(242,160,110)"
    context.fillText(message, absX.toInt(), absY.toInt())
    context.restore()
  }

  fun move() {
    val relTextWidth = textWidthInPixels / width
    if (relX > (1.0 - relTextWidth - relXVelocity.abs) || relX <  relXVelocity.abs) {
      relXVelocity *= -1
    }
    val relTextHeight = textHeightInPixels / height
    if (relY > (1.0 - relYVelocity.abs) || relY < relYVelocity.abs + relTextHeight) {
      relYVelocity *= -1
    }
    relX += relXVelocity
    relY += relYVelocity
  }

  fun randomVelocity() = 0.03 * Math.random() * (if (Math.random() < 0.5) 1 else -1)


  val Double.abs : Double
  get() = if (this > 0) this else -this
}

fun renderBackground() {
  context.save()
  context.fillStyle = "#5C7EED"
  context.fillRect(0, 0, width, height)
  context.restore()
}

fun main(args : Array<String>) {
  val interval = 50
  // we pass a literal that constructs a new HelloKotlin object
  val logos = Array(3) {
    HelloKotlin()
  }
  jq {
    setInterval({
      renderBackground()
      for (logo in logos) {
        logo.draw()
      }
    }, interval)
  }
}


