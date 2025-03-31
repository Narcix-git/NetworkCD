package ci.miage.mob.networkCD

import android.app.AlertDialog
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import com.google.gson.Gson



// Enumération des modes de fonctionnement de l'application
enum class Mode {
    ADD_NODE, ADD_CONNECTION, MODIFY
}

class GraphView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val graph = Graph()
    var currentMode: Mode = Mode.ADD_NODE

    private val nodeRadius = 40f

    // Paints pour dessiner nœuds, connexions et texte
    private val nodePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val connectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 15f
        style = Paint.Style.STROKE
        color = Color.BLACK
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 30f
    }



    private var tempLine: Path? = null
    private var selectedNode: Graph.Node? = null


    // Sérialise l'objet graph en JSON.
    fun serializeGraph(): String {
        val gson = Gson()
        return gson.toJson(graph)
    }

    // Désérialise le JSON pour mettre à jour le graph.
    fun deserializeGraph(json: String) {
        val gson = Gson()
        val newGraph = gson.fromJson(json, Graph::class.java)
        // Mise à jour du graphe courant
        graph.nodes.clear()
        graph.nodes.addAll(newGraph.nodes)
        graph.connections.clear()
        graph.connections.addAll(newGraph.connections)
        invalidate()
    }


    // Détecteur de geste pour gérer les long–click
    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener(){
        override fun onLongPress(e: MotionEvent) {
            e.let {
                when (currentMode) {
                    Mode.ADD_NODE -> {
                        // Long–click en mode ajout de nœud : on demande l'étiquette et on ajoute le nœud à la position pressée.
                        val x = e.x
                        val y = e.y
                        promptForNodeLabel { label ->
                            graph.addNode(x, y, label)
                            invalidate()
                        }
                    }
                    Mode.MODIFY -> {
                        // En mode modification, un long–click sur un nœud ou sur une connexion permet de lancer le menu de modification.
                        val node = findNodeAt(e.x, e.y)
                        if (node != null) {
                            showNodeOptions(node)
                        } else {
                            val connection = findConnectionLabelAt(e.x, e.y)
                            connection?.let {
                                showConnectionOptions(it)
                            }
                        }
                    }
                    else -> {
                        // Autres modes : on ne fait rien.
                    }
                }
            }
        }
    })

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Dessin des connexions
        for (connection in graph.connections) {
            connectionPaint.color = connection.color
            connectionPaint.strokeWidth = connection.thickness
            canvas.drawLine(connection.from.x, connection.from.y, connection.to.x, connection.to.y, connectionPaint)
            // Dessin de l'étiquette au milieu de la connexion (avec un léger décalage)
            val midX = (connection.from.x + connection.to.x) / 2
            val midY = (connection.from.y + connection.to.y) / 2
            canvas.drawText(connection.label, midX + 10, midY - 10, textPaint)
        }
        // Dessin des nœuds
        for (node in graph.nodes) {
            nodePaint.color = node.color
            canvas.drawCircle(node.x, node.y, nodeRadius, nodePaint)
            // Dessin de l'étiquette à côté du nœud
            canvas.drawText(node.label, node.x + nodeRadius + 5, node.y, textPaint)
        }
        // Dessin de la ligne temporaire pour la création d'une connexion
        tempLine?.let { canvas.drawPath(it, connectionPaint) }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)

        when (currentMode) {
            Mode.ADD_CONNECTION -> {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        val node = findNodeAt(event.x, event.y)
                        if (node != null) {
                            selectedNode = node
                            tempLine = Path().apply { moveTo(node.x, node.y) }
                        }
                    }
                    MotionEvent.ACTION_MOVE -> {
                        tempLine?.let {
                            it.reset()
                            it.moveTo(selectedNode!!.x, selectedNode!!.y)
                            it.lineTo(event.x, event.y)
                            invalidate()
                        }
                    }
                    MotionEvent.ACTION_UP -> {
                        val targetNode = findNodeAt(event.x, event.y)
                        val startNode = selectedNode  // on sauvegarde la valeur de selectedNode
                        if (targetNode != null && startNode != null && targetNode != startNode) {
                            promptForConnectionLabel { label ->
                                graph.addConnection(startNode, targetNode, label)
                                invalidate()
                            }
                        }
                        tempLine = null
                        selectedNode = null
                        invalidate()
                    }

                }
            }
            else -> {
                // En mode MODIFY (ou en mode par défaut), on autorise le déplacement des nœuds par glisser.
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        val node = findNodeAt(event.x, event.y)
                        if (node != null && currentMode == Mode.MODIFY) {
                            selectedNode = node
                        }
                    }
                    MotionEvent.ACTION_MOVE -> {
                        selectedNode?.let {
                            it.x = event.x
                            it.y = event.y
                            invalidate()
                        }
                    }
                    MotionEvent.ACTION_UP -> {
                        selectedNode = null
                        invalidate()
                    }
                }
            }
        }
        return true
    }

    // Détecte un nœud à proximité d'une position donnée
    private fun findNodeAt(x: Float, y: Float): Graph.Node? {
        return graph.nodes.find { Math.hypot((x - it.x).toDouble(), (y - it.y).toDouble()) <= nodeRadius }
    }

    // Recherche une connexion si le long–click se situe près du point médian de la ligne.
    private fun findConnectionLabelAt(x: Float, y: Float): Graph.Connection? {
        return graph.connections.find { connection ->
            val midX = (connection.from.x + connection.to.x) / 2
            val midY = (connection.from.y + connection.to.y) / 2
            Math.hypot((x - midX).toDouble(), (y - midY).toDouble()) < 50
        }
    }

    // Affiche une boîte de dialogue pour saisir l'étiquette du nœud.
    fun promptForNodeLabel(onLabelProvided: (String) -> Unit) {
        val editText = EditText(context)
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.add_node_label))
            .setView(editText)
            .setPositiveButton(context.getString(R.string.ok)) { dialog, which ->
                val label = editText.text.toString()
                onLabelProvided(label)
            }
            .setNegativeButton(context.getString(R.string.cancel), null)
            .show()
    }

    // Affiche une boîte de dialogue pour saisir l'étiquette de la connexion.
    fun promptForConnectionLabel(onLabelProvided: (String) -> Unit) {
        val editText = EditText(context)
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.add_connection_label))
            .setView(editText)
            .setPositiveButton(context.getString(R.string.ok)) { dialog, which ->
                val label = editText.text.toString()
                onLabelProvided(label)
            }
            .setNegativeButton(context.getString(R.string.cancel), null)
            .show()
    }

    // Affiche un menu d'options pour un nœud (supprimer ou modifier)
    fun showNodeOptions(node: Graph.Node) {
        val options = arrayOf(
            context.getString(R.string.delete),
            context.getString(R.string.modify)
        )
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.node_options))
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> { // Supprimer
                        graph.nodes.remove(node)
                        graph.connections.removeAll { it.from == node || it.to == node }
                        invalidate()
                    }
                    1 -> { // Modifier
                        promptForNodeModification(node)
                    }
                }
            }
            .show()
    }

    // Permet de modifier l'étiquette et la couleur d'un nœud.
    fun promptForNodeModification(node: Graph.Node) {
        val editText = EditText(context)
        editText.setText(node.label)
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.modify_node_label))
            .setView(editText)
            .setPositiveButton(context.getString(R.string.ok)) { dialog, which ->
                node.label = editText.text.toString()
                // Demander la sélection de couleur parmi un choix limité.
                promptForColor(context, node.color) { selectedColor ->
                    node.color = selectedColor
                    invalidate()
                }
            }
            .setNegativeButton(context.getString(R.string.cancel), null)
            .show()
    }
// Permet de réintialiser les connections
    fun resetConnections() {
        graph.resetConnections()
        invalidate()
    }

    // Affiche un menu pour modifier une connexion (supprimer, modifier étiquette, couleur, épaisseur)
    fun showConnectionOptions(connection: Graph.Connection) {
        val options = arrayOf(
            context.getString(R.string.delete),
            context.getString(R.string.modify_connection)
        )
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.connection_options))
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> { // Supprimer
                        graph.connections.remove(connection)
                        invalidate()
                    }
                    1 -> { // Modifier
                        promptForConnectionModification(connection)
                    }
                }
            }
            .show()
    }

    // Permet de modifier l'étiquette, la couleur et l'épaisseur d'une connexion.
    fun promptForConnectionModification(connection: Graph.Connection) {
        val editText = EditText(context)
        editText.setText(connection.label)
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.modify_connection_label))
            .setView(editText)
            .setPositiveButton(context.getString(R.string.ok)) { dialog, which ->
                connection.label = editText.text.toString()
                promptForColor(context, connection.color) { selectedColor ->
                    connection.color = selectedColor
                    promptForThickness(context, connection.thickness) { selectedThickness ->
                        connection.thickness = selectedThickness
                        invalidate()
                    }
                }
            }
            .setNegativeButton(context.getString(R.string.cancel), null)
            .show()
    }
}

// Fonctions d'aide pour la sélection de couleur et d'épaisseur.

fun promptForColor(context: Context, currentColor: Int, onColorSelected: (Int) -> Unit) {
    val colors = arrayOf("Rouge", "Vert", "Bleu", "Orange", "Cyan", "Magenta", "Noir")
    val colorValues = arrayOf(Color.RED, Color.GREEN, Color.BLUE, Color.parseColor("#FFA500"), Color.CYAN, Color.MAGENTA, Color.BLACK)
    AlertDialog.Builder(context)
        .setTitle(context.getString(R.string.select_color))
        .setItems(colors) { dialog, which ->
            onColorSelected(colorValues[which])
        }
        .show()
}

fun promptForThickness(context: Context, currentThickness: Float, onThicknessSelected: (Float) -> Unit) {
    // Pour simplifier, on propose quelques valeurs prédéfinies.
    val thicknessOptions = arrayOf("5", "10", "15", "20")
    AlertDialog.Builder(context)
        .setTitle(context.getString(R.string.select_thickness))
        .setItems(thicknessOptions) { dialog, which ->
            onThicknessSelected(thicknessOptions[which].toFloat())
        }
        .show()
}



