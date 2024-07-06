package com.example.chatgptschoolproject

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.widget.Button
import android.widget.ImageView
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

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = result.data?.data
            if (uri != null) {
                handleSelectedImage(uri)
            }
        }
    }

    private val chatgptservice: ChatGptService by lazy {
        ChatGptService(this)
    }


    private lateinit var buttonQuestion: Button
    private lateinit var textQuestion: TextView
    private lateinit var imageView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        buttonQuestion = findViewById(R.id.buttonQuestion)
        textQuestion = findViewById(R.id.textQuestion)
        imageView = findViewById(R.id.imageView)



        buttonQuestion.setOnClickListener {
            openGallery()
            }

        }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    private fun handleSelectedImage(selectedImageUri: Uri) {
        try {
            // Uri'den file path'i çek
            val selectedImagePath: String? = getPathFromUri(this,selectedImageUri)


            if (selectedImagePath != null) {
                // Resimi base64 formatına getir
                val imageBase64: String = encodeImageToBase64(selectedImagePath)

                val bitmap = BitmapFactory.decodeFile(selectedImagePath)
                imageView.setImageBitmap(bitmap)

                chatgptservice.sendMessageToChatGPT(imageBase64){ response ->
                    Log.d("ChatGPTResponse", response ?: "Error")
                    runOnUiThread {

                        val jsonResponse = response?: "error"
                        val assistantReply = parseAssistantReply(jsonResponse)
                        textQuestion.text = assistantReply
                    }
                }

            } else {
                // file path çekilemediğinde yazdır
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
            base64Image = Base64.encodeToString(buffer, Base64.DEFAULT).trim()

            Log.d("Base64Encoding", "Base64 Image: $base64Image")
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

                // Response'den gelen content'i işle ve okunabilir hale getir
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
    

