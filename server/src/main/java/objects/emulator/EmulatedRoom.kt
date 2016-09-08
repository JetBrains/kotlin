package objects.emulator

import org.w3c.dom.Node
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class EmulatedRoom() {
    val emulatedWalls = arrayListOf<EmulatedWall>()

    constructor(EmulatedWalls: Collection<EmulatedWall>) : this() {
        this.emulatedWalls.addAll(EmulatedWalls)
    }

    companion object {
        fun EmulatedRoomFromFile(pathToFile: String): EmulatedRoom? {

            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(File(pathToFile))
            val root = doc.getDocumentElement()
            val rootChild = root.childNodes
            var walls: Node? = null
            for (i in 0..rootChild.length - 1) {
                val node = rootChild.item(i)
                if (node.nodeType == Node.ELEMENT_NODE) {
                    walls = node
                    break
                }
            }
            walls ?: return null
            val wallsChild = walls.childNodes
            val result = EmulatedRoom()
            for (i in 0..wallsChild.length - 1) {
                val wall = wallsChild.item(i)
                if (wall.nodeType != Node.ELEMENT_NODE) {
                    continue
                }
                val EmulatedWall = EmulatedWall.wallFromXml(wall) ?: return null
                result.emulatedWalls.add(EmulatedWall)
            }
            return result
        }
    }
}
