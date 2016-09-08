package objects.emulator

import algorithm.geometry.Line
import org.w3c.dom.Node

class EmulatedWall constructor(val line: Line, val xFrom: Int, val xTo: Int, val yFrom: Int, val yTo: Int) {

    companion object {
        fun wallFromXml(wall: Node): EmulatedWall? {
            val points = wall.attributes
            val startX: Int
            val endX: Int
            val endY: Int
            val startY: Int
            try {
                startX = points.getNamedItem("startX").nodeValue.toInt()
                startY = points.getNamedItem("startY").nodeValue.toInt()
                endX = points.getNamedItem("endX").nodeValue.toInt()
                endY = points.getNamedItem("endY").nodeValue.toInt()
            } catch (e: NumberFormatException) {
                return null
            }
            val line = Line((startY - endY).toDouble(), (endX - startX).toDouble(), (startX * endY - startY * endX).toDouble())
            return EmulatedWall(line, startX, endX, startY, endY)
        }
    }
}