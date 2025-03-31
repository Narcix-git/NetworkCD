package ci.miage.mob.networkCD.controller

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import ci.miage.mob.networkCD.GraphView
import ci.miage.mob.networkCD.Mode
import ci.miage.mob.networkCD.R
import ci.miage.mob.networkCD.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.title = "Network CoulibalyNarcisse/DouampoArmel"
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Mode par défaut : ajout de nœud
        binding.graphView.currentMode = Mode.ADD_NODE

        // Bouton pour réinitialiser le graphe
        binding.btnReinitialiserGraph.setOnClickListener {
            binding.graphView.resetConnections()
        }
        // Bouton pour passer en mode ajout de nœud
        binding.btnAjoutN.setOnClickListener {
            binding.graphView.currentMode = Mode.ADD_NODE
        }
        // Bouton pour passer en mode ajout de connexion
        binding.btnAjoutC.setOnClickListener {
            binding.graphView.currentMode = Mode.ADD_CONNECTION
        }
        // Bouton pour passer en mode modification (déplacement, modification/suppression)
        binding.btnModifier.setOnClickListener {
            binding.graphView.currentMode = Mode.MODIFY
        }
        // Bouton pour sauvegarder le réseau
        binding.btnSauvegarderReseau.setOnClickListener {
            saveGraph()
        }
        // Bouton pour afficher (charger) un réseau sauvegardé
        binding.btnAfficherReseau.setOnClickListener {
            loadGraph()
        }
    }

    private fun saveGraph() {
        try {
            val json = binding.graphView.serializeGraph()
            openFileOutput("graph.json", MODE_PRIVATE).use {
                it.write(json.toByteArray())
            }
            Toast.makeText(this, getString(R.string.save_network) + " OK", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Erreur lors de la sauvegarde", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadGraph() {
        try {
            val json = openFileInput("graph.json").bufferedReader().use { it.readText() }
            binding.graphView.deserializeGraph(json)
            Toast.makeText(this, getString(R.string.load_network) + " OK", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Erreur lors du chargement", Toast.LENGTH_SHORT).show()
        }
    }


}
