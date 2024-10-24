package com.travis.inshape

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.travis.inshape.databinding.ActivityChatBotBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ChatBotActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatBotBinding
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var questions: List<QuestionAnswer>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBotBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Load and parse the questions.json file
        val gson = Gson()
        val jsonString = loadJSONFromAsset(this, "questions.json")
        val parsedQuestions: Questions = gson.fromJson(jsonString, Questions::class.java)
        questions = parsedQuestions.questions

        // Setup RecyclerView
        chatAdapter = ChatAdapter(mutableListOf())
        binding.chatRecyclerView.apply {
            adapter = chatAdapter
            layoutManager = LinearLayoutManager(this@ChatBotActivity)
        }

        // Set up send button click listener
        binding.sendButton.setOnClickListener {
            handleSendButtonClick()
        }
    }

    private fun handleSendButtonClick() {
        val userInput = binding.messageEditText.text.toString()

        if (userInput.isNotBlank()) {
            chatAdapter.addMessage(ChatMessage(userInput, isUser = true))

            // Show typing indicator
            chatAdapter.addMessage(ChatMessage("Chatbot is typing...", isUser = false))
            binding.chatRecyclerView.scrollToPosition(chatAdapter.itemCount - 1)

            // Simulate delay before the chatbot replies
            CoroutineScope(Dispatchers.Main).launch {
                delay(1500)
                val chatbotResponse = getAnswer(userInput, questions)

                // Remove typing indicator and add actual chatbot message
                chatAdapter.removeLastMessage()
                chatAdapter.addMessage(ChatMessage(chatbotResponse, isUser = false))
                binding.chatRecyclerView.scrollToPosition(chatAdapter.itemCount - 1)
            }

            binding.messageEditText.text?.clear()
        }
    }


    fun loadJSONFromAsset(context: Context, fileName: String): String? {
        return try {
            val inputStream = context.assets.open(fileName)
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            String(buffer, Charsets.UTF_8)
        } catch (ex: Exception) {
            ex.printStackTrace()
            null
        }
    }

    fun getAnswer(userInput: String, questions: List<QuestionAnswer>): String {
        val normalizedInput = normalizeInput(userInput)
        var bestMatch: QuestionAnswer? = null
        var minDistance = Int.MAX_VALUE

        // Split the user's input into keywords
        val inputKeywords = normalizedInput.split(" ")

        for (qa in questions) {
            val normalizedQuestion = normalizeInput(qa.question)

            // Calculate the Levenshtein distance between user input and question
            val distance = levenshteinDistance(normalizedInput, normalizedQuestion)

            // Check if the question contains key terms from the input
            val questionKeywords = normalizedQuestion.split(" ")
            val commonKeywords = inputKeywords.intersect(questionKeywords.toSet())

            // Prioritize keyword matching (e.g., more common keywords = better match)
            if (commonKeywords.size > 1) {
                return qa.answer
            }

            // Fallback to Levenshtein distance for close matches
            if (distance < minDistance) {
                minDistance = distance
                bestMatch = qa
            }
        }

        // Set a threshold to determine how close the match must be
        return if (minDistance <= 5 && bestMatch != null) {
            bestMatch.answer
        } else {
            "Sorry, I don't have an answer for that question."
        }
    }


    private fun normalizeInput(input: String): String {
        // Define stop words but avoid removing important words like 'abs' or 'exercises'
        val stopWords = listOf("how", "do", "i", "the", "is", "what", "and", "to", "for", "a", "in", "on", "of")
        val words = input.replace(Regex("[^a-zA-Z0-9 ]"), "").lowercase().split(" ")

        // Ensure important fitness-related terms are not filtered out
        return words.filter { it !in stopWords }.joinToString(" ")
    }



    fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }

        for (i in 0..s1.length) {
            for (j in 0..s2.length) {
                when {
                    i == 0 -> dp[i][j] = j
                    j == 0 -> dp[i][j] = i
                    s1[i - 1] == s2[j - 1] -> dp[i][j] = dp[i - 1][j - 1]
                    else -> dp[i][j] = 1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
                }
            }
        }

        return dp[s1.length][s2.length]
    }

}



