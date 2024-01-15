package com.example.chatgptschoolproject.ui.theme

import android.content.Context
import android.util.Log
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONObject

class ChatGptService(context: Context) {
    private val TAG="ChatGptService"
    private val apiKey = "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" // Replace with your actual API key
    private val apiUrl = "https://api.openai.com/v1/chat/completions"
    private val requestQueue: RequestQueue = Volley.newRequestQueue(context)

    fun sendMessageToChatGPT(text: String, onResponse: (String?) -> Unit) {

        val messagesArray = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "system")
                put("content", "You are a helpful assistant.")
            })
            put(JSONObject().apply {
                put("role", "user")
                put("content", text)
            })
        }

        val requestBody = JSONObject().apply {
            put("model", "gpt-3.5-turbo")
            put("messages", messagesArray)
        }

        val request = object: JsonObjectRequest(
            Request.Method.POST,
            apiUrl,
            requestBody,
            Response.Listener { response ->
                val jsonResponse = response.toString()
                Log.d(TAG, "Response: $jsonResponse")
                onResponse(jsonResponse)
            },
            Response.ErrorListener {error ->
                val errorMessage = "Volley Error: ${error.networkResponse?.statusCode ?: "Unknown"}"
                Log.e(TAG, errorMessage, error)
                val responseBody = String(error.networkResponse?.data ?: byteArrayOf())
                Log.e(TAG, "Response Body: $responseBody")
                onResponse(null)
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = "application/json"
                headers["Authorization"] = "Bearer $apiKey"
                return headers
            }

            override fun parseNetworkError(volleyError: com.android.volley.VolleyError?): VolleyError {
                return super.parseNetworkError(volleyError)
            }
        }

        requestQueue.add(request)
    }
}