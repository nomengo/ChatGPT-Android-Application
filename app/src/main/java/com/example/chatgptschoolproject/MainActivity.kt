package com.example.chatgptschoolproject

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.example.chatgptschoolproject.ui.theme.ChatGptService
import okhttp3.OkHttpClient
import org.json.JSONException
import org.json.JSONObject


class MainActivity : ComponentActivity() {
    private val client = OkHttpClient()

    private val READ_STORAGE_PERMISSION_CODE = 123
    private val PICK_IMAGE_REQUEST = 1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val chatgptservice = ChatGptService(this)
        val editQuestion = findViewById<EditText>(R.id.editQuestion)
        val buttonQuestion = findViewById<Button>(R.id.buttonQuestion)
        val textQuestion = findViewById<TextView>(R.id.textQuestion)



        buttonQuestion.setOnClickListener {
            //val userInput = editQuestion.text.toString()
            val imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/2/23/The_Great_Wall_of_China_at_Jinshanling-edit.jpg/250px-The_Great_Wall_of_China_at_Jinshanling-edit.jpg"
            chatgptservice.sendMessageToChatGPT(imageUrl){ response ->
                Log.d("ChatGPTResponse", response ?: "Error")
                runOnUiThread {

                    val jsonResponse = response?: "error"
                    val assistantReply = parseAssistantReply(jsonResponse)
                    textQuestion.text = assistantReply
                }
                }
            }
            //val question = editQuestion.text.toString()
            //Toast.makeText(this,question,Toast.LENGTH_SHORT).show()

        }

    private fun parseAssistantReply(jsonResponse: String): String {
        return try {
            val jsonObject = JSONObject(jsonResponse)
            val choices = jsonObject.getJSONArray("choices")

            if (choices.length() > 0) {
                val assistantReply = choices.getJSONObject(0).getString("message") ?: "Error"
                val cleanedReply = assistantReply.replace("[,;./\"]".toRegex(), "")

                // Extract text after "content:" until the last character before a closing bracket
                val startIndex = cleanedReply.indexOf("content:") + "content:".length
                val endIndex = cleanedReply.lastIndexOf('}')
                val extractedText = cleanedReply.substring(startIndex, endIndex).trim()

                extractedText
            } else {
                "No valid response"
            }
        } catch (e: JSONException) {
            "Error parsing response"
        }
    }

}
    

