package com.traverse.android.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

@Serializable
private data class GeminiRequest(
    val contents: List<GeminiContent>,
    val systemInstruction: GeminiContent? = null,
    val generationConfig: GeminiGenerationConfig? = null
)

@Serializable
private data class GeminiContent(
    val parts: List<GeminiPart>,
    val role: String? = null
)

@Serializable
private data class GeminiPart(
    val text: String
)

@Serializable
private data class GeminiGenerationConfig(
    val temperature: Double = 0.3,
    val maxOutputTokens: Int = 300
)

@Serializable
private data class GeminiResponse(
    val candidates: List<GeminiCandidate>? = null,
    val error: GeminiErrorResponse? = null
)

@Serializable
private data class GeminiCandidate(
    val content: GeminiContent? = null
)

@Serializable
private data class GeminiErrorResponse(
    val message: String? = null,
    val code: Int? = null
)

/**
 * Direct Gemini API Client for AI Revision Coach & Mentorship.
 */
class GeminiService(private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(20, TimeUnit.SECONDS)
    .build()
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    companion object {
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent"
        
        private const val SYSTEM_PROMPT = """
        You are a concise DSA & LeetCode AI mentor in the Traverse app. Analyze code attempts and error patterns for revision problems.
        RULES:
        1. Output ONLY 1 to 3 plain conversational sentences of technical hint or comparison.
        2. NEVER output JSON, code blocks (```), backticks, or preamble phrases (such as "Here is your hint:").
        3. Ignore typos, syntax slips, and silly formatting errors. Never use emojis.
        """
        
        @Volatile
        private var instance: GeminiService? = null
        
        fun getInstance(): GeminiService {
            return instance ?: synchronized(this) {
                instance ?: GeminiService().also { instance = it }
            }
        }
    }

    suspend fun generateRevisionHintForPending(
        apiKey: String,
        problemTitle: String,
        difficulty: String,
        attempts: List<CodeAttempt>,
        mistakeTags: List<String>?,
        aiAnalysis: String?
    ): Result<String> = withContext(Dispatchers.IO) {
        var cleanSummary = aiAnalysis ?: "None"
        if (cleanSummary.contains("```") || cleanSummary.contains("{")) {
            cleanSummary = cleanOutput(cleanSummary)
        }

        val contextBuilder = StringBuilder()
        contextBuilder.append("Problem: $problemTitle (${difficulty.replaceFirstChar { it.uppercase() }})\n")
        contextBuilder.append("Mistake Tags: ${(mistakeTags ?: emptyList()).joinToString(", ")}\n")
        contextBuilder.append("Previous Analysis: $cleanSummary\n\n")
        contextBuilder.append("Historical Code Iterations (failed attempts & final correct):\n")

        if (attempts.isEmpty()) {
            contextBuilder.append("(No raw attempt code stored; rely on mistake tags and summary)\n")
        } else {
            attempts.forEachIndexed { idx, att ->
                val status = if (att.successful == true) "Accepted" else "Failed"
                val snippet = (att.code ?: "").take(800)
                contextBuilder.append("Attempt #${idx + 1} [${att.type ?: "code"}, $status]:\n$snippet\n\n")
            }
        }

        contextBuilder.append(
            "\nINSTRUCTION:\nState the main algorithmic/logic mistake made in previous attempts and give a 2-sentence actionable hint to avoid repeating it today. Output ONLY plain text sentences. No JSON. No code fences. No preamble."
        )

        callGemini(apiKey, contextBuilder.toString())
    }

    suspend fun generateRevisionComparisonForCompleted(
        apiKey: String,
        problemTitle: String,
        difficulty: String,
        previousAttempts: List<CodeAttempt>,
        todayAttempts: List<CodeAttempt>,
        mistakeTags: List<String>?
    ): Result<String> = withContext(Dispatchers.IO) {
        val contextBuilder = StringBuilder()
        contextBuilder.append("Problem: $problemTitle (${difficulty.replaceFirstChar { it.uppercase() }})\n")
        contextBuilder.append("Mistake Tags: ${(mistakeTags ?: emptyList()).joinToString(", ")}\n\n")
        contextBuilder.append("Previous Historical Attempts:\n")

        if (previousAttempts.isEmpty()) {
            contextBuilder.append("(No historical attempt code stored)\n")
        } else {
            previousAttempts.forEachIndexed { idx, att ->
                val snippet = (att.code ?: "").take(800)
                contextBuilder.append("Prev #${idx + 1}:\n$snippet\n\n")
            }
        }

        contextBuilder.append("\nToday's Attempt Code:\n")
        if (todayAttempts.isEmpty()) {
            contextBuilder.append("(Completed today)\n")
        } else {
            todayAttempts.forEachIndexed { idx, att ->
                val snippet = (att.code ?: "").take(800)
                val status = if (att.successful == true) "Accepted" else "Failed"
                contextBuilder.append("Today #${idx + 1} [$status]:\n$snippet\n\n")
            }
        }

        contextBuilder.append(
            "\nINSTRUCTION:\nCompare today's code against previous attempt history. Output ONLY 2 plain text sentences answering if the user repeated the same mistake or solved it cleanly. No JSON. No code fences. No preamble."
        )

        callGemini(apiKey, contextBuilder.toString())
    }

    private fun callGemini(apiKey: String, prompt: String): Result<String> {
        try {
            val url = "$BASE_URL?key=$apiKey"
            val requestPayload = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        parts = listOf(GeminiPart(text = prompt)),
                        role = "user"
                    )
                ),
                systemInstruction = GeminiContent(
                    parts = listOf(GeminiPart(text = SYSTEM_PROMPT))
                ),
                generationConfig = GeminiGenerationConfig(
                    temperature = 0.3,
                    maxOutputTokens = 300
                )
            )

            val jsonBody = json.encodeToString(GeminiRequest.serializer(), requestPayload)
            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toRequestBody(mediaType))
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = try {
                    val err = json.decodeFromString<GeminiResponse>(responseBody)
                    err.error?.message ?: "Gemini API error: HTTP ${response.code}"
                } catch (e: Exception) {
                    "Gemini API HTTP ${response.code}"
                }
                return Result.failure(Exception(errorMsg))
            }

            val geminiResponse = json.decodeFromString<GeminiResponse>(responseBody)
            val text = geminiResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "Watch out for edge cases and boundary conditions when solving this problem."

            return Result.success(cleanOutput(text))
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    private fun cleanOutput(raw: String): String {
        var text = raw.trim()
        if (text.startsWith("```")) {
            text = text.substringAfter("\n").substringBeforeLast("```").trim()
        }
        text = text.replace("`", "")
        if (text.startsWith("{") && text.endsWith("}")) {
            text = text.replace(Regex("[\"{}]"), "").trim()
        }
        return text
    }
}