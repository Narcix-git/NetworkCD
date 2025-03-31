package ci.miage.mob.networkCD

import android.graphics.Color

class Graph {

    val nodes = mutableListOf<Node>()
    val connections = mutableListOf<Connection>()

    data class Node(var x: Float, var y: Float, var label: String = "", var color: Int = Color.RED)

    data class Connection(
        val from: Node,
        val to: Node,
        var label: String = "",
        var color: Int = Color.BLACK,
        var thickness: Float = 15f
    )

    fun addNode(x: Float, y: Float, label: String = "", color: Int = Color.RED): Node {
        val newNode = Node(x, y, label, color)
        nodes.add(newNode)
        return newNode
    }

    fun addConnection(from: Node, to: Node, label: String = "", color: Int = Color.BLACK, thickness: Float = 15f) {
        connections.add(Connection(from, to, label, color, thickness))
    }

    fun resetConnections() {
        connections.clear()
    }

    fun resetGraph() {
        connections.clear()
        nodes.clear()
    }

}
