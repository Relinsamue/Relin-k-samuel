package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.RetrofitClient
import com.example.data.db.AppDatabase
import com.example.data.db.SavedCreation
import com.example.data.db.SavedCreationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

enum class ToolType {
    HOME, CAPTION, SCRIPT, POST, THUMBNAIL, BLOG, HISTORY, INFO, ANALYTICS, ASSISTANT
}

data class ChatMessage(
    val sender: String, // "user" or "assistant"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

sealed class GenerationState {
    object Idle : GenerationState()
    object Loading : GenerationState()
    data class Success(val content: String, val savedId: Long? = null) : GenerationState()
    data class Error(val message: String) : GenerationState()
}

// Data class to store parsed specification for live thumbnail drawing
data class ThumbnailSpec(
    val bgColor1: String = "#0F172A", // Default dark slate
    val bgColor2: String = "#1E293B",
    val textColor: String = "#F8FAFC",
    val focalIcon: String = "LIGHTBULB",
    val layout: String = "LEFT"
)

class MainMenuViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SavedCreationRepository
    
    init {
        val database = AppDatabase.getDatabase(application)
        repository = SavedCreationRepository(database.savedCreationDao())
    }

    // Navigation & Screen selection state
    private val _currentTool = MutableStateFlow(ToolType.HOME)
    val currentTool: StateFlow<ToolType> = _currentTool.asStateFlow()

    // Flow of history items from database
    private val _historyItems = MutableStateFlow<List<SavedCreation>>(emptyList())
    val historyItems: StateFlow<List<SavedCreation>> = _historyItems.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Generation States
    private val _generationState = MutableStateFlow<GenerationState>(GenerationState.Idle)
    val generationState: StateFlow<GenerationState> = _generationState.asStateFlow()

    // 1. Caption Form Inputs
    var captionTopic = MutableStateFlow("")
    var captionPlatform = MutableStateFlow("Instagram")
    var captionTone = MutableStateFlow("Inspirational")
    var captionHashtagsCount = MutableStateFlow("5")
    var captionIncludeEmojis = MutableStateFlow(true)

    // 2. YouTube Script Form Inputs
    var scriptTopic = MutableStateFlow("")
    var scriptPacing = MutableStateFlow("Story-driven")
    var scriptDuration = MutableStateFlow("Shorts (~60s)")

    // 3. Social Post Form Inputs
    var postIdeas = MutableStateFlow("")
    var postPlatform = MutableStateFlow("X / Twitter")
    var postObjective = MutableStateFlow("Provide value & educate")

    // 4. Thumbnail Concept Form Inputs
    var thumbTopic = MutableStateFlow("")
    var thumbOverlayText = MutableStateFlow("")
    var thumbTheme = MutableStateFlow("Tech Minimal")
    var thumbMood = MutableStateFlow("Energetic / Modern")
    
    // Live derived preview configuration parsed from generation response
    private val _thumbnailSpec = MutableStateFlow(ThumbnailSpec())
    val thumbnailSpec: StateFlow<ThumbnailSpec> = _thumbnailSpec.asStateFlow()

    // 5. AI Blog Form Inputs
    var blogTopic = MutableStateFlow("")
    var blogSections = MutableStateFlow("")
    var blogKeywords = MutableStateFlow("")
    var blogWordCount = MutableStateFlow("Medium (~800 words)")

    init {
        observeHistory()
    }

    fun selectTool(tool: ToolType) {
        _currentTool.value = tool
        // Reset generation state upon selecting active tools
        if (tool != ToolType.HISTORY) {
            _generationState.value = GenerationState.Idle
        }
    }

    private fun observeHistory() {
        viewModelScope.launch {
            repository.allCreations
                .catch { _historyItems.value = emptyList() }
                .collect { items ->
                    _historyItems.value = items
                }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            if (query.isBlank()) {
                repository.allCreations.collect { _historyItems.value = it }
            } else {
                repository.search(query).collect { _historyItems.value = it }
            }
        }
    }

    // Triggers custom generation based on selected tool type
    fun generateContent() {
        viewModelScope.launch {
            _generationState.value = GenerationState.Loading
            
            val prompt: String
            val systemInstruction: String
            val title: String
            val inputJsonObj = JSONObject()

            when (_currentTool.value) {
                ToolType.CAPTION -> {
                    val topic = captionTopic.value
                    if (topic.isBlank()) {
                        _generationState.value = GenerationState.Error("Topic cannot be empty!")
                        return@launch
                    }
                    val platform = captionPlatform.value
                    val tone = captionTone.value
                    val hashtags = captionHashtagsCount.value
                    val emojis = if (captionIncludeEmojis.value) "Yes" else "No"

                    title = "Caption: $topic"
                    inputJsonObj.put("platform", platform)
                    inputJsonObj.put("tone", tone)
                    inputJsonObj.put("hashtags", hashtags)
                    inputJsonObj.put("emojis", emojis)

                    systemInstruction = "You are an expert Social Media Copywriter specializing in dynamic engagement."
                    prompt = """
                        Generate high-converting, magnetic captions for $platform.
                        Topic: $topic
                        Tone of Voice: $tone
                        Target Hashtags Count: $hashtags
                        Include Emojis: $emojis
                        
                        Structure the response beautifully with:
                        - **🔥 Viral Option** (high hooks, readable formatting)
                        - **💼 Professional Option** (corporate, value-first)
                        - **💬 Interactive Option** (ends with powerful question to drive comments)
                    """.trimIndent()
                }

                ToolType.SCRIPT -> {
                    val topic = scriptTopic.value
                    if (topic.isBlank()) {
                        _generationState.value = GenerationState.Error("Topic/Title cannot be empty!")
                        return@launch
                    }
                    val pacing = scriptPacing.value
                    val duration = scriptDuration.value

                    title = "Script: $topic"
                    inputJsonObj.put("pacing", pacing)
                    inputJsonObj.put("duration", duration)

                    systemInstruction = "You are an elite YouTube Producer and Storytelling Consultant."
                    prompt = """
                        Write a fully crafted YouTube video script on:
                        Title/Topic: $topic
                        Vibe and Pacing style: $pacing
                        Expected Duration: $duration
                        
                        Format the output cleanly into labeled script sections:
                        - **[HOOK / FIRST 5 SECS]** (Instant hook, high energy)
                        - **[INTRO]** (Setup the promise and core thesis)
                        - **[BODY CONTENT - MULTIPLE SEGMENTS]** (Key takeaways, b-roll recommendations in parentheses)
                        - **[OUTRO & CALL TO ACTION]** (Powerful sign-off, subscribing action, leading to next video)
                    """.trimIndent()
                }

                ToolType.POST -> {
                    val ideas = postIdeas.value
                    if (ideas.isBlank()) {
                        _generationState.value = GenerationState.Error("Main idea details cannot be empty!")
                        return@launch
                    }
                    val platform = postPlatform.value
                    val objective = postObjective.value

                    title = "Social Post: ${ideas.take(25)}..."
                    inputJsonObj.put("platform", platform)
                    inputJsonObj.put("objective", objective)

                    systemInstruction = "You are a top-tier digital content strategist and copywriter."
                    prompt = """
                        Draft a high-impact post formatted perfectly for $platform.
                        Core Theme / Key Details: $ideas
                        Goal/Objective: $objective
                        
                        Provide 2 distinct formats depending on typical platform trends:
                        1. **Format A**: High Hook with structured bullet points and dynamic whitespace spacing.
                        2. **Format B**: Narrative/Story-driven option with a strong takeaway advice summary.
                    """.trimIndent()
                }

                ToolType.THUMBNAIL -> {
                    val topic = thumbTopic.value
                    val overlay = thumbOverlayText.value
                    if (topic.isBlank() || overlay.isBlank()) {
                        _generationState.value = GenerationState.Error("Topic and overlay text are required!")
                        return@launch
                    }
                    val theme = thumbTheme.value
                    val mood = thumbMood.value

                    title = "Thumbnail: $overlay"
                    inputJsonObj.put("topic", topic)
                    inputJsonObj.put("overlayText", overlay)
                    inputJsonObj.put("theme", theme)
                    inputJsonObj.put("mood", mood)

                    systemInstruction = "You are an expert YouTube Thumbnail Artist & Graphic Designer."
                    prompt = """
                        Create a thumbnail blueprint layout spec and styling instructions:
                        Topic: $topic
                        Headline Text: $overlay
                        Theme: $theme
                        Mood & Vibe: $mood
                        
                        You MUST start your response with a strict SPECIFICATION block exactly matching this pattern:
                        [SPEC_START]
                        BG_GRADIENT: #Hex1, #Hex2
                        TEXT_COLOR: #HexCode
                        FOCAL_ICON: (choose only one from: FIRE, SPARK, LIGHTBULB, STAR, IDEA, ROCKET, GRAPH, CAMERA, CODING)
                        LAYOUT: (LEFT or RIGHT or CENTER)
                        [SPEC_END]
                        
                        Example Spec header:
                        [SPEC_START]
                        BG_GRADIENT: #1E1B4B, #4F46E5
                        TEXT_COLOR: #FACC15
                        FOCAL_ICON: ROCKET
                        LAYOUT: LEFT
                        [SPEC_END]
                        
                        After [SPEC_END], provide:
                        - **Background element graphics recommendations**
                        - **Color theory & styling notes for higher CTR**
                        - **Facial expressions or human asset ideas to add in overlays**
                    """.trimIndent()
                }

                ToolType.BLOG -> {
                    val topic = blogTopic.value
                    if (topic.isBlank()) {
                        _generationState.value = GenerationState.Error("Blog topic cannot be empty!")
                        return@launch
                    }
                    val sections = blogSections.value
                    val keywords = blogKeywords.value
                    val wordCount = blogWordCount.value

                    title = "Blog: $topic"
                    inputJsonObj.put("sections", sections)
                    inputJsonObj.put("keywords", keywords)
                    inputJsonObj.put("wordCount", wordCount)

                    systemInstruction = "You are a professional SEO Specialist, Content Copywriter, and Blog Editor."
                    prompt = """
                        Write a fully formed, high-quality, engaging blog post.
                        Blog Title/Topic: $topic
                        Sections/Outlines to include (optional): $sections
                        Target SEO Focus Keywords: $keywords (integrate them naturally)
                        Target Article Length: $wordCount
                        
                        Format the response using clear headings, subheadings, bullet points, and include:
                        1. **Suggested Meta Title & Meta Description**
                        2. **Detailed Article Content**
                        3. **3 Actionable SEO Optimization Recommendations**
                    """.trimIndent()
                }
                else -> return@launch
            }

            // Perform REST api call via Retrofit Client
            val result = withContext(Dispatchers.IO) {
                RetrofitClient.generate(prompt = prompt, systemInstructionText = systemInstruction)
            }

            if (result == "ERR_API_KEY_MISSING") {
                _generationState.value = GenerationState.Error(
                    "API Key Missing! Please configure your GEMINI_API_KEY inside the Secrets panel of AI Studio to proceed."
                )
                return@launch
            }

            if (result.startsWith("Error:")) {
                _generationState.value = GenerationState.Error(result)
                return@launch
            }

            // Parse thumbnail spec if selected
            if (_currentTool.value == ToolType.THUMBNAIL) {
                parseThumbnailSpec(result)
            }

            // Auto-persist to Room local database for historic backup!
            val newCreation = SavedCreation(
                title = title,
                toolType = _currentTool.value.name,
                inputs = inputJsonObj.toString(),
                outputContent = result
            )
            val savedDbId = withContext(Dispatchers.IO) {
                repository.insert(newCreation)
            }

            _generationState.value = GenerationState.Success(result, savedDbId)
        }
    }

    private fun parseThumbnailSpec(responseText: String) {
        try {
            val specStartIdx = responseText.indexOf("[SPEC_START]")
            val specEndIdx = responseText.indexOf("[SPEC_END]")
            if (specStartIdx != -1 && specEndIdx != -1 && specEndIdx > specStartIdx) {
                val specBlock = responseText.substring(specStartIdx + 12, specEndIdx).trim()
                var bg1 = "#0F172A"
                var bg2 = "#1E293B"
                var txt = "#F8FAFC"
                var icon = "LIGHTBULB"
                var lay = "LEFT"

                specBlock.lines().forEach { line ->
                    val cleanLine = line.trim()
                    when {
                        cleanLine.startsWith("BG_GRADIENT:") -> {
                            val colors = cleanLine.substringAfter("BG_GRADIENT:").trim().split(",")
                            if (colors.size >= 2) {
                                bg1 = colors[0].trim()
                                bg2 = colors[1].trim()
                            } else if (colors.isNotEmpty()) {
                                bg1 = colors[0].trim()
                                bg2 = colors[0].trim()
                            }
                        }
                        cleanLine.startsWith("TEXT_COLOR:") -> {
                            txt = cleanLine.substringAfter("TEXT_COLOR:").trim()
                        }
                        cleanLine.startsWith("FOCAL_ICON:") -> {
                            icon = cleanLine.substringAfter("FOCAL_ICON:").trim().uppercase()
                        }
                        cleanLine.startsWith("LAYOUT:") -> {
                            lay = cleanLine.substringAfter("LAYOUT:").trim().uppercase()
                        }
                    }
                }
                
                // Validate parsed hex codes before applying to prevent crashes
                fun isValidHex(hex: String): Boolean {
                    return hex.startsWith("#") && (hex.length == 7 || hex.length == 9)
                }

                _thumbnailSpec.value = ThumbnailSpec(
                    bgColor1 = if (isValidHex(bg1)) bg1 else "#0F172A",
                    bgColor2 = if (isValidHex(bg2)) bg2 else "#1E293B",
                    textColor = if (isValidHex(txt)) txt else "#F8FAFC",
                    focalIcon = icon,
                    layout = lay
                )
            }
        } catch (e: Exception) {
            // Safe fallback
        }
    }

    // Secondary parsing for loading historic thumbnail specs
    fun reloadThumbnailSpec(outputContent: String) {
        parseThumbnailSpec(outputContent)
    }

    // Toggle Favorite Action
    fun toggleFavorite(creation: SavedCreation) {
        viewModelScope.launch {
            val updated = creation.copy(isFavorite = !creation.isFavorite)
            withContext(Dispatchers.IO) {
                repository.update(updated)
            }
            // Update current list status
            updateSearchQuery(_searchQuery.value)
        }
    }

    // Delete Action
    fun deleteCreation(id: Int) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.deleteById(id)
            }
            updateSearchQuery(_searchQuery.value)
        }
    }

    // AI Assistant Chat Messages State
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(listOf(
        ChatMessage("assistant", "Hello, Relin! 👋 I'm your CreatorPro AI Assistant. Ask me anything—from viral hooks and script structures to hashtag strategy, visual content ideas, or how to grow your audience!")
    ))
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    fun sendChatMessage(text: String) {
        if (text.isBlank()) return
        val userMsg = ChatMessage("user", text)
        _chatMessages.value = _chatMessages.value + userMsg
        _isChatLoading.value = true

        viewModelScope.launch {
            val contextPrompt = buildString {
                append("You are the official AI Assistant for CreatorPro, a premium creator tools suite. You are helping the creator Relin to brainstorm, edit, and plan awesome digital content.\n\n")
                val lastMsgs = _chatMessages.value.takeLast(10)
                for (msg in lastMsgs) {
                    if (msg.sender == "user") {
                        append("User: ${msg.text}\n")
                    } else {
                        append("Assistant: ${msg.text}\n")
                    }
                }
                append("Assistant:")
            }

            val result = withContext(Dispatchers.IO) {
                RetrofitClient.generate(
                    prompt = contextPrompt,
                    systemInstructionText = "You are a friendly, highly professional, and creative social media content strategist AI assistant. Keep responses actionable, concise, well-formatted, and direct."
                )
            }

            _isChatLoading.value = false
            if (result == "ERR_API_KEY_MISSING") {
                _chatMessages.value = _chatMessages.value + ChatMessage(
                    "assistant",
                    "It looks like your GEMINI_API_KEY is not configured yet. Please open the Secrets panel in Google AI Studio, add `GEMINI_API_KEY`, and try again so we can chat!"
                )
            } else if (result.startsWith("Error:")) {
                _chatMessages.value = _chatMessages.value + ChatMessage("assistant", "Sorry, I ran into a network error: $result. Let's try again in a bit!")
            } else {
                _chatMessages.value = _chatMessages.value + ChatMessage("assistant", result)
            }
        }
    }

    fun clearChat() {
        _chatMessages.value = listOf(
            ChatMessage("assistant", "Hello, Relin! 👋 I'm your CreatorPro AI Assistant. Ask me anything—from viral hooks and script structures to hashtag strategy, visual content ideas, or how to grow your audience!")
        )
    }
}
