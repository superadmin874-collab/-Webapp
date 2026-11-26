package com.example.data.repository

import android.util.Log
import com.example.data.model.QuizDifficulty
import com.example.data.model.QuizQuestion
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

data class GeneratedQuizQuestion(
    val questionText: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
    val sourceNote: String,
    val confidenceLevel: String
)

class AiQuizRepository {
    private val quizResponseSchema = Schema.obj(
        properties = mapOf(
            "questions" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "questionText" to Schema.string(description = "Clear educational multiple-choice GK question."),
                        "options" to Schema.array(
                            items = Schema.string(),
                            description = "Exactly four distinct answer options."
                        ),
                        "correctIndex" to Schema.integer(description = "0-based index (0, 1, 2, or 3) of the single correct answer."),
                        "explanation" to Schema.string(description = "Concise, accurate educational explanation of why the answer is correct."),
                        "sourceNote" to Schema.string(description = "Authoritative reference source and year/date for verification, e.g., NCERT / Constitution of India / ISRO (2025)."),
                        "confidenceLevel" to Schema.enumeration(
                            values = listOf("VERIFIED", "UNCERTAIN"),
                            description = "Mark VERIFIED only for well-established facts; mark UNCERTAIN if time-sensitive or potentially disputed."
                        )
                    )
                ),
                description = "List of generated educational GK questions."
            )
        )
    )

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = quizResponseSchema
                temperature = 0.4f
            }
        )
    }

    suspend fun generateGkQuestions(
        category: String,
        difficulty: QuizDifficulty,
        count: Int,
        avoidQuestions: List<String> = emptyList(),
        bilingualBengaliHint: Boolean = false
    ): Result<List<GeneratedQuizQuestion>> {
        val safeCount = count.coerceIn(1, 20)
        val avoidBlock = if (avoidQuestions.isNotEmpty()) {
            "\nDo NOT repeat or closely paraphrase any of these previously asked questions:\n" +
                avoidQuestions.takeLast(25).joinToString("\n") { "- $it" }
        } else {
            ""
        }
        val languageInstruction = if (bilingualBengaliHint) {
            "Write questions primarily in clear English, and include helpful Bengali terms in parentheses where appropriate for West Bengal/Indian students."
        } else {
            "Write questions in clear, student-friendly English."
        }

        val prompt = """
            You are an expert academic quiz author for the educational platform দরবার (Dorbar).
            Generate $safeCount unique, factually verified multiple-choice General Knowledge questions.
            Category: $category
            Difficulty: ${difficulty.label}
            $languageInstruction
            Rules:
            1. Each question must have exactly 4 distinct options and 1 unambiguously correct answer (correctIndex 0..3).
            2. Provide a helpful, accurate educational explanation.
            3. Provide a specific sourceNote with the reference name and date/year (especially for Current Affairs).
            4. Never invent uncertain facts as verified: if a recent event or statistic could be uncertain, mark confidenceLevel as "UNCERTAIN" or choose an established verified fact marked "VERIFIED".
            $avoidBlock
        """.trimIndent()

        return try {
            val response = withTimeout(60_000L) {
                model.generateContent(prompt)
            }
            val jsonText = response.text
            if (jsonText.isNullOrBlank()) {
                return Result.failure(IllegalStateException("Gemini returned an empty quiz response."))
            }
            val root = JSONObject(jsonText)
            val arr = root.getJSONArray("questions")
            val parsed = mutableListOf<GeneratedQuizQuestion>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val optionsArr = obj.getJSONArray("options")
                val options = (0 until optionsArr.length()).map { idx -> optionsArr.getString(idx).trim() }
                if (options.size == 4) {
                    val correctIdx = obj.optInt("correctIndex", 0).coerceIn(0, 3)
                    parsed.add(
                        GeneratedQuizQuestion(
                            questionText = obj.getString("questionText").trim(),
                            options = options,
                            correctIndex = correctIdx,
                            explanation = obj.optString("explanation", "").trim(),
                            sourceNote = obj.optString("sourceNote", "Educational Reference").trim(),
                            confidenceLevel = obj.optString("confidenceLevel", "VERIFIED").trim()
                        )
                    )
                }
            }
            if (parsed.isEmpty()) {
                Result.failure(IllegalStateException("No valid multiple-choice questions were generated."))
            } else {
                Result.success(parsed)
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI Quiz generation timed out", e)
            Result.failure(IllegalStateException("AI question generation timed out after 60 seconds. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI Quiz generation failed", e)
            Result.failure(e)
        }
    }

    fun toQuizQuestionModel(
        generated: GeneratedQuizQuestion,
        category: String,
        difficulty: QuizDifficulty,
        creatorUid: String,
        adminUids: List<String>,
        approved: Boolean
    ): QuizQuestion {
        val verifiedSuffix = if (generated.confidenceLevel == "UNCERTAIN") {
            " [Needs Admin Verification]"
        } else {
            ""
        }
        return QuizQuestion(
            category = category,
            difficulty = difficulty.name,
            questionText = generated.questionText,
            options = generated.options,
            correctIndex = generated.correctIndex.toLong(),
            explanation = generated.explanation,
            sourceNote = (generated.sourceNote + verifiedSuffix).take(290),
            aiGenerated = true,
            approved = approved,
            createdByUid = creatorUid,
            adminUids = adminUids
        )
    }

    private companion object {
        const val TAG = "AiQuizRepository"
    }
}
