package com.termux.studio.rooms

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.termux.R
import com.termux.studio.BaseRoomActivity
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject

/**
 * ApiBackendRoomActivity - REST API Testing Environment
 *
 * Features:
 * - Send HTTP requests (GET, POST, PUT, DELETE)
 * - View response headers and body
 * - Save request collections
 * - Environment variables for API keys
 * - JSON body editor with syntax highlighting
 * - Response history
 */
class ApiBackendRoomActivity : BaseRoomActivity() {

    override val roomName = "API/Backend"
    override val roomIcon = R.drawable.ic_api_backend

    // UI Components
    private lateinit var urlInput: EditText
    private lateinit var methodSpinner: Spinner
    private lateinit var headersContainer: LinearLayout
    private lateinit var bodyEditor: EditText
    private lateinit var responseView: TextView
    private lateinit var historyList: RecyclerView
    private lateinit var historyAdapter: HistoryAdapter

    // State
    private var currentMethod = "GET"
    private var headers = mutableMapOf<String, String>()
    private var requestHistory = mutableListOf<RequestHistoryItem>()
    private var environments = mutableMapOf<String, String>()

    override fun onRoomCreated() {
        setupRequestPanel()
        setupResponsePanel()
        setupHistory()
        setupEnvironments()
        loadSavedData()
    }

    private fun setupRequestPanel() {
        // Add request UI to main content
        val requestView = LayoutInflater.from(this)
            .inflate(R.layout.view_api_request, mainContent, false)
        mainContent.addView(requestView)

        // URL input
        urlInput = requestView.findViewById<EditText>(R.id.url_input).apply {
            hint = "https://api.example.com/endpoint"
        }

        // Method spinner
        methodSpinner = requestView.findViewById<Spinner>(R.id.method_spinner).apply {
            adapter = ArrayAdapter(
                this@ApiBackendRoomActivity,
                android.R.layout.simple_spinner_dropdown_item,
                arrayOf("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS")
            )
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    currentMethod = parent?.getItemAtPosition(position).toString()
                    updateBodyVisibility()
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }

        // Headers container
        headersContainer = requestView.findViewById(R.id.headers_container)
        addHeaderRow("Content-Type", "application/json")
        addHeaderRow("Accept", "application/json")

        // Add header button
        requestView.findViewById<Button>(R.id.btn_add_header).setOnClickListener {
            addHeaderRow("", "")
        }

        // Body editor
        bodyEditor = requestView.findViewById<EditText>(R.id.body_editor).apply {
            hint = "{\n  \"key\": \"value\"\n}"
        }

        // Send button
        requestView.findViewById<Button>(R.id.btn_send).setOnClickListener {
            sendRequest()
        }

        // Hide placeholder
        findViewById<TextView>(R.id.content_placeholder)?.visibility = View.GONE
    }

    private fun addHeaderRow(key: String, value: String) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val keyInput = EditText(this).apply {
            hint = "Header Name"
            setText(key)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val valueInput = EditText(this).apply {
            hint = "Header Value"
            setText(value)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val removeBtn = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_delete)
            setOnClickListener {
                headersContainer.removeView(row)
            }
        }

        row.addView(keyInput)
        row.addView(valueInput)
        row.addView(removeBtn)
        headersContainer.addView(row)
    }

    private fun updateBodyVisibility() {
        val showBody = currentMethod in listOf("POST", "PUT", "PATCH")
        bodyEditor.visibility = if (showBody) View.VISIBLE else View.GONE
    }

    private fun setupResponsePanel() {
        responseView = consoleOutput
        clearConsole()
        appendToConsole("API/Backend Room Ready")
        appendToConsole("Enter a URL and click Send to make a request\n")
    }

    private fun setupHistory() {
        historyList = findViewById(R.id.file_tree)
        historyAdapter = HistoryAdapter { item ->
            loadHistoryItem(item)
        }
        historyList.layoutManager = LinearLayoutManager(this)
        historyList.adapter = historyAdapter

        // Change left panel label
        findViewById<TextView>(R.id.left_panel_title)?.text = "History"
    }

    private fun setupEnvironments() {
        // Load environment variables from shared prefs
        val prefs = getSharedPreferences("api_room_env", Context.MODE_PRIVATE)
        environments["BASE_URL"] = prefs.getString("BASE_URL", "") ?: ""
        environments["API_KEY"] = prefs.getString("API_KEY", "") ?: ""
        environments["AUTH_TOKEN"] = prefs.getString("AUTH_TOKEN", "") ?: ""

        // Add environment button in toolbar or drawer
        findViewById<View>(R.id.btn_environments)?.setOnClickListener {
            showEnvironmentsDialog()
        }
    }

    private fun loadSavedData() {
        // Load request history from SharedPreferences
        val prefs = getSharedPreferences("api_room_history", Context.MODE_PRIVATE)
        val historyJson = prefs.getString("history", "[]")
        try {
            val jsonArray = JSONArray(historyJson)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                requestHistory.add(RequestHistoryItem(
                    method = obj.getString("method"),
                    url = obj.getString("url"),
                    statusCode = obj.getInt("statusCode"),
                    timestamp = obj.getLong("timestamp")
                ))
            }
            historyAdapter.updateHistory(requestHistory)
        } catch (e: Exception) {
            // Ignore parsing errors
        }
    }

    private fun saveHistory() {
        val jsonArray = JSONArray()
        requestHistory.takeLast(50).forEach { item ->
            jsonArray.put(JSONObject().apply {
                put("method", item.method)
                put("url", item.url)
                put("statusCode", item.statusCode)
                put("timestamp", item.timestamp)
            })
        }

        val prefs = getSharedPreferences("api_room_history", Context.MODE_PRIVATE)
        prefs.edit().putString("history", jsonArray.toString()).apply()
    }

    // === Request Handling ===

    private fun sendRequest() {
        var url = urlInput.text.toString().trim()
        if (url.isBlank()) {
            showToast("Please enter a URL")
            return
        }

        // Replace environment variables
        url = replaceEnvironmentVariables(url)

        // Collect headers
        collectHeaders()

        // Get body for POST/PUT/PATCH
        val body = if (currentMethod in listOf("POST", "PUT", "PATCH")) {
            replaceEnvironmentVariables(bodyEditor.text.toString())
        } else null

        appendToConsole("\n=== Request ===")
        appendToConsole("$currentMethod $url")
        headers.forEach { (k, v) ->
            appendToConsole("$k: $v")
        }
        if (body != null) {
            appendToConsole("\nBody:")
            appendToConsole(body)
        }
        appendToConsole("\nSending...")

        executeRequest(url, currentMethod, headers, body)
    }

    private fun collectHeaders() {
        headers.clear()
        for (i in 0 until headersContainer.childCount) {
            val row = headersContainer.getChildAt(i) as? LinearLayout ?: continue
            val keyInput = row.getChildAt(0) as? EditText ?: continue
            val valueInput = row.getChildAt(1) as? EditText ?: continue

            val key = keyInput.text.toString().trim()
            val value = valueInput.text.toString().trim()

            if (key.isNotBlank()) {
                headers[key] = replaceEnvironmentVariables(value)
            }
        }
    }

    private fun replaceEnvironmentVariables(input: String): String {
        var result = input
        environments.forEach { (key, value) ->
            result = result.replace("{{$key}}", value)
            result = result.replace("\${$key}", value)
        }
        return result
    }

    private fun executeRequest(
        urlString: String,
        method: String,
        headers: Map<String, String>,
        body: String?
    ) {
        Thread {
            try {
                val startTime = System.currentTimeMillis()
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection

                connection.requestMethod = method
                connection.connectTimeout = 30000
                connection.readTimeout = 30000

                // Set headers
                headers.forEach { (key, value) ->
                    connection.setRequestProperty(key, value)
                }

                // Send body
                if (body != null && method in listOf("POST", "PUT", "PATCH")) {
                    connection.doOutput = true
                    connection.outputStream.bufferedWriter().use { it.write(body) }
                }

                // Get response
                val responseCode = connection.responseCode
                val responseMessage = connection.responseMessage
                val responseHeaders = connection.headerFields

                val responseBody = try {
                    if (responseCode in 200..299) {
                        connection.inputStream.bufferedReader().readText()
                    } else {
                        connection.errorStream?.bufferedReader()?.readText() ?: ""
                    }
                } catch (e: Exception) {
                    "Error reading response: ${e.message}"
                }

                val duration = System.currentTimeMillis() - startTime

                connection.disconnect()

                // Add to history
                val historyItem = RequestHistoryItem(
                    method = method,
                    url = urlString,
                    statusCode = responseCode,
                    timestamp = System.currentTimeMillis()
                )
                requestHistory.add(0, historyItem)

                runOnUiThread {
                    displayResponse(responseCode, responseMessage, responseHeaders, responseBody, duration)
                    historyAdapter.updateHistory(requestHistory)
                    saveHistory()
                }

            } catch (e: Exception) {
                runOnUiThread {
                    appendToConsole("\n=== Error ===")
                    appendToConsole("${e.javaClass.simpleName}: ${e.message}")
                }
            }
        }.start()
    }

    private fun displayResponse(
        code: Int,
        message: String,
        headers: Map<String, List<String>>?,
        body: String,
        duration: Long
    ) {
        val statusColor = when {
            code in 200..299 -> "SUCCESS"
            code in 300..399 -> "REDIRECT"
            code in 400..499 -> "CLIENT ERROR"
            code in 500..599 -> "SERVER ERROR"
            else -> "UNKNOWN"
        }

        appendToConsole("\n=== Response ===")
        appendToConsole("Status: $code $message [$statusColor]")
        appendToConsole("Time: ${duration}ms")

        appendToConsole("\n--- Headers ---")
        headers?.forEach { (key, values) ->
            if (key != null) {
                appendToConsole("$key: ${values.joinToString(", ")}")
            }
        }

        appendToConsole("\n--- Body ---")
        // Try to format JSON
        try {
            val formatted = if (body.trim().startsWith("{")) {
                JSONObject(body).toString(2)
            } else if (body.trim().startsWith("[")) {
                JSONArray(body).toString(2)
            } else {
                body
            }
            appendToConsole(formatted)
        } catch (e: Exception) {
            appendToConsole(body)
        }

        appendToConsole("\n")
    }

    private fun loadHistoryItem(item: RequestHistoryItem) {
        urlInput.setText(item.url)
        val position = arrayOf("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS")
            .indexOf(item.method)
        if (position >= 0) {
            methodSpinner.setSelection(position)
        }
        appendToConsole("Loaded from history: ${item.method} ${item.url}")
    }

    private fun showEnvironmentsDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_environments, null)

        val baseUrlInput = view.findViewById<EditText>(R.id.input_base_url)
        val apiKeyInput = view.findViewById<EditText>(R.id.input_api_key)
        val authTokenInput = view.findViewById<EditText>(R.id.input_auth_token)

        baseUrlInput?.setText(environments["BASE_URL"])
        apiKeyInput?.setText(environments["API_KEY"])
        authTokenInput?.setText(environments["AUTH_TOKEN"])

        AlertDialog.Builder(this)
            .setTitle("Environment Variables")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                environments["BASE_URL"] = baseUrlInput?.text?.toString() ?: ""
                environments["API_KEY"] = apiKeyInput?.text?.toString() ?: ""
                environments["AUTH_TOKEN"] = authTokenInput?.text?.toString() ?: ""

                val prefs = getSharedPreferences("api_room_env", Context.MODE_PRIVATE)
                prefs.edit()
                    .putString("BASE_URL", environments["BASE_URL"])
                    .putString("API_KEY", environments["API_KEY"])
                    .putString("AUTH_TOKEN", environments["AUTH_TOKEN"])
                    .apply()

                showToast("Environment saved")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onRunClicked() {
        sendRequest()
    }

    override fun onSaveClicked() {
        showSaveCollectionDialog()
    }

    private fun showSaveCollectionDialog() {
        val input = EditText(this)
        input.hint = "Collection name"

        AlertDialog.Builder(this)
            .setTitle("Save to Collection")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val name = input.text.toString()
                saveToCollection(name)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveToCollection(name: String) {
        // Save current request to a collection file
        val collectionsDir = File(filesDir.parentFile, "home/api_collections")
        collectionsDir.mkdirs()

        val collectionFile = File(collectionsDir, "$name.json")

        collectHeaders()
        val request = JSONObject().apply {
            put("method", currentMethod)
            put("url", urlInput.text.toString())
            put("headers", JSONObject(headers as Map<*, *>))
            put("body", bodyEditor.text.toString())
        }

        collectionFile.writeText(request.toString(2))
        showToast("Saved to collection: $name")
    }

    // === Data Classes ===

    data class RequestHistoryItem(
        val method: String,
        val url: String,
        val statusCode: Int,
        val timestamp: Long
    )

    // === History Adapter ===

    inner class HistoryAdapter(
        private val onItemClick: (RequestHistoryItem) -> Unit
    ) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

        private var items = listOf<RequestHistoryItem>()

        fun updateHistory(newItems: List<RequestHistoryItem>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_file_tree, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount() = items.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val icon: ImageView = itemView.findViewById(R.id.file_icon)
            private val name: TextView = itemView.findViewById(R.id.file_name)

            fun bind(item: RequestHistoryItem) {
                val statusColor = when {
                    item.statusCode in 200..299 -> android.R.color.holo_green_dark
                    item.statusCode in 400..599 -> android.R.color.holo_red_dark
                    else -> android.R.color.darker_gray
                }

                name.text = "${item.method} ${item.url.take(40)}..."
                name.setTextColor(resources.getColor(statusColor, null))

                // Method icon
                icon.setImageResource(R.drawable.ic_api_backend)

                itemView.setOnClickListener {
                    onItemClick(item)
                }
            }
        }
    }
}
