package com.example.chatgptschoolproject

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.example.chatgptschoolproject.ui.theme.ChatGptService
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.IOException


class MainActivity : ComponentActivity() {

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // Handle the result, e.g., retrieve the image URI
                val data: Intent? = result.data
                val selectedImageUri = data?.data

                // Now, you can handle the selectedImageUri as needed
                if (selectedImageUri != null) {
                    handleSelectedImage(selectedImageUri)
                }
            }
        }

    private val chatgptservice: ChatGptService by lazy {
        ChatGptService(this)
    }


    private lateinit var buttonQuestion: Button
    private lateinit var textQuestion: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        //val editQuestion = findViewById<EditText>(R.id.editQuestion)
        buttonQuestion = findViewById(R.id.buttonQuestion)
        textQuestion = findViewById(R.id.textQuestion)



        buttonQuestion.setOnClickListener {
            //val userInput = editQuestion.text.toString()
            openGallery()
            //val imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/2/23/The_Great_Wall_of_China_at_Jinshanling-edit.jpg/250px-The_Great_Wall_of_China_at_Jinshanling-edit.jpg"
            }
            //val question = editQuestion.text.toString()
            //Toast.makeText(this,question,Toast.LENGTH_SHORT).show()

        }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    private fun handleSelectedImage(selectedImageUri: Uri) {
        try {
            // Rest of your code...
            // Retrieve the file path from the URI
            val selectedImagePath: String? = getPathFromUri(this,selectedImageUri)

            // Now, you can perform actions with the selected image
            if (selectedImagePath != null) {
                // Encode the image to base64 using the function from ImageUtils
                val imageBase64: String = encodeImageToBase64(selectedImagePath)

                chatgptservice.sendMessageToChatGPT(imageBase64){ response ->
                    Log.d("ChatGPTResponse", response ?: "Error")
                    runOnUiThread {

                        val jsonResponse = response?: "error"
                        val assistantReply = parseAssistantReply(jsonResponse)
                        textQuestion.text = assistantReply
                    }
                }

            } else {
                // Handle the case where the file path couldn't be retrieved
                Toast.makeText(this,"Failed to retrieve file path.",Toast.LENGTH_LONG).show()
            }

        } catch (e: Exception) {
            Log.e("MainActivity", "Error in handleSelectedImage", e)
            Toast.makeText(this, "An error occurred", Toast.LENGTH_LONG).show()
        }
    }

    fun getPathFromUri(context: Context, uri: Uri): String? {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        var cursor: Cursor? = null

        try {
            cursor = context.contentResolver.query(uri, projection, null, null, null)

            cursor?.let {
                val columnIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                if (it.moveToFirst()) {
                    return it.getString(columnIndex)
                }
            }
        } finally {
            cursor?.close()
        }

        return null
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
    

