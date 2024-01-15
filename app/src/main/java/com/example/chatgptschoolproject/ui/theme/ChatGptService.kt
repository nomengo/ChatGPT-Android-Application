package com.example.chatgptschoolproject.ui.theme

import android.content.Context
import android.util.Base64
import android.util.Log
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.IOException

class ChatGptService(context: Context) {
    private val TAG="ChatGptService"
    private val apiKey = "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" // Replace with your actual API key
    private val apiUrl = "https://api.openai.com/v1/chat/completions"
    private val requestQueue: RequestQueue = Volley.newRequestQueue(context)

    fun sendMessageToChatGPT(imagePath: String, onResponse: (String?) -> Unit) {

        //val imageBase64 = encodeImageToBase64(imagePath)
        val messagesArray = createImageCompletionRequest(imagePath)

        val requestBody = JSONObject().apply {
            put("model", "gpt-4-vision-preview")
            put("messages", messagesArray)
            put("max_tokens",1024)
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
        request.retryPolicy = DefaultRetryPolicy(
            10000,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )

        requestQueue.add(request)
    }

    private fun encodeImageToBase64(imagePath: String): String {
        var base64Image = ""
        try {
            val file = File(imagePath)
            val buffer = ByteArray(file.length().toInt())
            val inputStream = FileInputStream(file)
            inputStream.read(buffer)
            inputStream.close()
            base64Image = Base64.encodeToString(buffer, Base64.DEFAULT)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return base64Image
    }

    private fun createImageCompletionRequest(imageUrl: String): JSONArray {
        return JSONArray().apply {
            // User message
            put(JSONObject().apply {
                put("role", "user")

                val userContent = JSONArray()

                // Text part
                val textPart = JSONObject()
                textPart.put("type", "text")
                textPart.put("text", "Provide information about the structure in this image.")
                userContent.put(textPart)

                // Image URL part
                val imageUrlPart = JSONObject()
                imageUrlPart.put("type", "image_url")
                val imageUrlObject = JSONObject()
                imageUrlObject.put("url", imageUrl)
                imageUrlPart.put("image_url", imageUrlObject)
                userContent.put(imageUrlPart)

                put("content", userContent)
            })
        }
    }
}