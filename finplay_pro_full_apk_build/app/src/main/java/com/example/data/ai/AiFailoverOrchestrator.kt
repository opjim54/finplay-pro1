package com.example.data.ai

import com.example.data.local.AiChatMessageEntity
import com.example.data.local.AiProviderEntity
import com.example.data.local.KeepNoteEntity
import java.io.IOException
import java.net.SocketTimeoutException

data class AiFailoverResult(
    val text: String,
    val providerId: Long,
    val providerName: String,
    val wasAutoSwitched: Boolean
)

enum class NoteAiToolAction(val title: String, val subtitle: String) {
    GENERATE_NOTE("Generate Note", "Draft structured content from topic"),
    SUMMARIZE("Summarize", "Condense key points into quick bullets"),
    REWRITE("Rewrite & Polish", "Improve clarity, flow, and tone"),
    FIX_GRAMMAR("Fix Grammar", "Correct spelling and punctuation"),
    TRANSLATE("Translate", "Convert between English and Bangla"),
    GENERATE_TITLE("Generate Title", "Suggest a crisp descriptive headline"),
    SMART_TAGS("Smart Tags", "Extract relevant category labels"),
    EXTRACT_TASKS("Extract Tasks", "Turn action items into Good Tasks"),
    GENERATE_CHECKLIST("Generate Checklist", "Create tickable checklist steps"),
    ASK_YOUR_NOTES("Ask Your Notes", "Q&A across your saved notes"),
    FIND_DUPLICATES("Duplicate Detection", "Spot overlapping or similar notes"),
    SMART_ORGANIZE("Smart Organization", "Recommend pins, archives & labels")
}

object AiFailoverOrchestrator {
    fun isSwitchableFailure(err: Throwable): Boolean {
        if (err is SocketTimeoutException || err is IOException) return true
        val msg = (err.message ?: "").lowercase()
        val rateOrQuotaSignals = listOf(
            "429", "402", "403", "500", "502", "503", "504",
            "rate limit", "rate_limit", "quota", "insufficient_quota",
            "exceeded", "limit reached", "billing", "credit", "overloaded",
            "resource_exhausted", "too many requests", "timeout", "timed out"
        )
        return rateOrQuotaSignals.any { msg.contains(it) }
    }

    suspend fun sendWithFailover(
        preferredProvider: AiProviderEntity,
        allProviders: List<AiProviderEntity>,
        autoFailoverEnabled: Boolean,
        history: List<AiChatMessageEntity>,
        promptText: String,
        imageBase64: String?
    ): Result<AiFailoverResult> {
        val candidates = if (!autoFailoverEnabled || allProviders.size <= 1) {
            listOf(preferredProvider)
        } else {
            val others = allProviders.filter { it.id != preferredProvider.id && it.apiKey.isNotBlank() }
            val orderedOthers = if (imageBase64 != null) {
                others.sortedByDescending { it.supportsVision }
            } else {
                others
            }
            listOf(preferredProvider) + orderedOthers
        }

        var lastError: Throwable? = null
        candidates.forEachIndexed { index, candidate ->
            val attemptHistory = if (index == 0) history else emptyList()
            val effectiveImage = if (candidate.supportsVision) imageBase64 else null
            val res = AiChatClient.sendChatCompletion(
                provider = candidate,
                history = attemptHistory,
                promptText = promptText,
                imageBase64 = effectiveImage
            )
            if (res.isSuccess) {
                return Result.success(
                    AiFailoverResult(
                        text = res.getOrThrow(),
                        providerId = candidate.id,
                        providerName = "${candidate.name} (${candidate.modelName})",
                        wasAutoSwitched = index > 0
                    )
                )
            } else {
                val err = res.exceptionOrNull() ?: IllegalStateException("Provider error")
                lastError = err
                if (!autoFailoverEnabled || !isSwitchableFailure(err)) {
                    return Result.failure(err)
                }
            }
        }
        return Result.failure(lastError ?: IllegalStateException("All configured AI providers failed."))
    }
}

object NoteAiAssistantEngine {
    suspend fun execute(
        action: NoteAiToolAction,
        noteTitle: String,
        noteContent: String,
        userPromptOrLang: String,
        allNotes: List<KeepNoteEntity>,
        preferredProvider: AiProviderEntity?,
        allProviders: List<AiProviderEntity>,
        autoFailoverEnabled: Boolean
    ): Pair<String, String?> {
        // Handle local instant analysis for Duplicate Detection & Smart Organization or enrich with AI if configured
        val prompt = when (action) {
            NoteAiToolAction.GENERATE_NOTE ->
                "Write a well-structured, practical note on: ${userPromptOrLang.ifBlank { noteTitle.ifBlank { "Daily Productivity & Budget Plan" } }}"
            NoteAiToolAction.SUMMARIZE ->
                "Summarize this note into 3-5 concise bullet points:\n\nTitle: $noteTitle\nContent:\n$noteContent"
            NoteAiToolAction.REWRITE ->
                "Rewrite and polish this note for clarity and executive readability:\n\n$noteContent"
            NoteAiToolAction.FIX_GRAMMAR ->
                "Fix grammar, spelling, and punctuation errors in this text. Return only the corrected text:\n\n$noteContent"
            NoteAiToolAction.TRANSLATE ->
                "Translate the following note cleanly to ${userPromptOrLang.ifBlank { "Bangla" }} preserving meaning:\n\n$noteTitle\n$noteContent"
            NoteAiToolAction.GENERATE_TITLE ->
                "Suggest 1 short, crisp headline title (under 8 words) for this note:\n\n$noteContent"
            NoteAiToolAction.SMART_TAGS ->
                "Return 2 to 4 comma-separated single-word tags (e.g. Work,Study,Ideas,Personal) for this note:\n\n$noteTitle\n$noteContent"
            NoteAiToolAction.EXTRACT_TASKS ->
                "Extract actionable tasks from this note, one task per line with no bullets or numbers:\n\n$noteTitle\n$noteContent"
            NoteAiToolAction.GENERATE_CHECKLIST ->
                "Create a 5-step practical checklist for '${noteTitle.ifBlank { userPromptOrLang }} ${noteContent.take(200)}', one item per line with no bullets:"
            NoteAiToolAction.ASK_YOUR_NOTES -> {
                val corpus = allNotes.take(25).joinToString("\n---\n") { n ->
                    "Title: ${n.title}\nTags: ${n.tagsCsv}\nContent: ${n.content.take(400)}"
                }
                "Answer this question strictly using the saved notes below.\nQuestion: $userPromptOrLang\n\nSaved Notes:\n$corpus"
            }
            NoteAiToolAction.FIND_DUPLICATES -> {
                val report = detectDuplicatesLocally(allNotes)
                if (preferredProvider == null) return report to null
                "Review these potential duplicate or overlapping notes and suggest which to merge:\n\n$report"
            }
            NoteAiToolAction.SMART_ORGANIZE -> {
                val localPlan = buildSmartOrganizationLocally(allNotes)
                if (preferredProvider == null) return localPlan to null
                "Provide smart organization advice (pin, archive, label) for this workspace summary:\n\n$localPlan"
            }
        }

        if (preferredProvider != null) {
            val aiRes = AiFailoverOrchestrator.sendWithFailover(
                preferredProvider = preferredProvider,
                allProviders = allProviders,
                autoFailoverEnabled = autoFailoverEnabled,
                history = emptyList(),
                promptText = prompt,
                imageBase64 = null
            )
            if (aiRes.isSuccess) {
                val ok = aiRes.getOrThrow()
                val badge = if (ok.wasAutoSwitched) "Auto-switched → ${ok.providerName}" else ok.providerName
                return ok.text.trim() to badge
            }
        }

        // Intelligent On-Device Fallback so every AI note tool stays 100% functional offline or before API key entry
        val fallback = when (action) {
            NoteAiToolAction.GENERATE_NOTE -> {
                val topic = userPromptOrLang.ifBlank { noteTitle.ifBlank { "Smart Action Plan" } }
                "## $topic\n\n### Key Objectives\n- Define measurable milestones\n- Allocate time & budget cleanly\n\n### Next Action Steps\n- Break down into subtasks and set reminder."
            }
            NoteAiToolAction.SUMMARIZE -> {
                val sentences = noteContent.split(Regex("[.!?।\n]+")).map { it.trim() }.filter { it.isNotEmpty() }
                if (sentences.isEmpty()) "Note has no text to summarize."
                else sentences.take(4).joinToString("\n") { "• $it" }
            }
            NoteAiToolAction.REWRITE -> {
                noteContent.trim()
                    .lines()
                    .map { it.trim().replace(Regex("\\s+"), " ") }
                    .filter { it.isNotEmpty() }
                    .joinToString("\n\n") { line ->
                        line.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                    }.ifBlank { noteTitle }
            }
            NoteAiToolAction.FIX_GRAMMAR -> {
                noteContent.trim()
                    .replace(Regex("[ \\t]+"), " ")
                    .replace(" i ", " I ")
                    .replace("dont", "don't")
                    .replace("cant", "can't")
                    .replace("wont", "won't")
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
            NoteAiToolAction.TRANSLATE ->
                "Add an AI Provider (Gemini / xKiro / OpenAI) to translate between languages automatically, or keep writing natively in Bangla/English."
            NoteAiToolAction.GENERATE_TITLE -> {
                val firstLine = noteContent.lines().firstOrNull { it.isNotBlank() }?.trim().orEmpty()
                firstLine.split(Regex("\\s+")).take(6).joinToString(" ").ifBlank { "Quick Smart Note" }
            }
            NoteAiToolAction.SMART_TAGS -> inferTagsLocally("$noteTitle $noteContent")
            NoteAiToolAction.EXTRACT_TASKS -> {
                val lines = noteContent.lines()
                    .map { it.trim().removePrefix("- ").removePrefix("* ").removePrefix("• ") }
                    .filter { it.length > 3 }
                if (lines.isNotEmpty()) lines.take(6).joinToString("\n")
                else "Review ${noteTitle.ifBlank { "Note" }}\nComplete action follow-up"
            }
            NoteAiToolAction.GENERATE_CHECKLIST -> {
                val topic = noteTitle.ifBlank { userPromptOrLang.ifBlank { "Task" } }
                "Gather materials for $topic\nOutline main steps\nExecute core work\nReview & verify quality\nMark complete & archive"
            }
            NoteAiToolAction.ASK_YOUR_NOTES -> {
                val qWords = userPromptOrLang.lowercase().split(Regex("\\s+")).filter { it.length > 2 }
                val matches = allNotes.map { n ->
                    val hay = "${n.title} ${n.content} ${n.tagsCsv}".lowercase()
                    val hits = qWords.count { hay.contains(it) }
                    n to hits
                }.filter { it.second > 0 }.sortedByDescending { it.second }.take(3)
                if (matches.isEmpty()) "No matching facts found in your saved notes for \"$userPromptOrLang\"."
                else matches.joinToString("\n\n") { (n, _) -> "• ${n.title.ifBlank { "Untitled" }}: ${n.content.take(180)}" }
            }
            NoteAiToolAction.FIND_DUPLICATES -> detectDuplicatesLocally(allNotes)
            NoteAiToolAction.SMART_ORGANIZE -> buildSmartOrganizationLocally(allNotes)
        }
        return fallback to "On-Device Smart Engine"
    }

    private fun inferTagsLocally(text: String): String {
        val lower = text.lowercase()
        val picked = mutableListOf<String>()
        if (lower.contains("exam") || lower.contains("class") || lower.contains("study") || lower.contains("assignment")) picked.add("Study")
        if (lower.contains("client") || lower.contains("meeting") || lower.contains("project") || lower.contains("deadline")) picked.add("Work")
        if (lower.contains("idea") || lower.contains("startup") || lower.contains("plan") || lower.contains("feature")) picked.add("Ideas")
        if (lower.contains("budget") || lower.contains("money") || lower.contains("cost") || lower.contains("bill")) picked.add("Finance")
        if (picked.isEmpty()) picked.add("Personal")
        return picked.distinct().joinToString(",")
    }

    private fun detectDuplicatesLocally(notes: List<KeepNoteEntity>): String {
        if (notes.size < 2) return "No duplicates detected (${notes.size} active note)."
        val pairs = mutableListOf<String>()
        for (i in notes.indices) {
            val a = notes[i]
            val tokensA = ("${a.title} ${a.content}").lowercase().split(Regex("\\W+")).filter { it.length > 2 }.toSet()
            if (tokensA.isEmpty()) continue
            for (j in i + 1 until notes.size) {
                val b = notes[j]
                val tokensB = ("${b.title} ${b.content}").lowercase().split(Regex("\\W+")).filter { it.length > 2 }.toSet()
                if (tokensB.isEmpty()) continue
                val overlap = tokensA.intersect(tokensB).size.toDouble() / maxOf(1, minOf(tokensA.size, tokensB.size))
                val sameTitle = a.title.isNotBlank() && a.title.equals(b.title, ignoreCase = true)
                if (sameTitle || overlap >= 0.55) {
                    val pct = (overlap * 100).toInt().coerceIn(55, 100)
                    pairs.add("• \"${a.title.ifBlank { "Untitled" }}\" ↔ \"${b.title.ifBlank { "Untitled" }}\" (~$pct% overlap)")
                }
            }
        }
        return if (pairs.isEmpty()) "All ${notes.size} notes look unique — zero duplicates found!"
        else "Potential Duplicates Found (${pairs.size}):\n" + pairs.take(6).joinToString("\n")
    }

    private fun buildSmartOrganizationLocally(notes: List<KeepNoteEntity>): String {
        val untagged = notes.count { it.tagsCsv.isBlank() }
        val staleOld = notes.count { !it.isPinned && (System.currentTimeMillis() - it.updatedAt) > 30L * 86_400_000L }
        val checklists = notes.count { it.checklistJson != "[]" && it.checklistJson.isNotBlank() }
        val locked = notes.count { it.isLocked }
        return buildString {
            appendLine("Smart Workspace Audit (${notes.size} active notes):")
            appendLine("• Untagged notes needing labels: $untagged")
            appendLine("• Notes untouched >30 days (archive candidates): $staleOld")
            appendLine("• Active checklists: $checklists | Locked private notes: $locked")
            if (untagged > 0) appendLine("Tip: Open any untagged note and run 'Smart Tags' in AI Tools.")
        }.trim()
    }
}
