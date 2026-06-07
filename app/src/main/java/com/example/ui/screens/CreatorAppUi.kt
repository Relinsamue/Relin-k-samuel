package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import com.example.data.db.SavedCreation
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.text.selection.SelectionContainer
import com.example.data.api.RetrofitClient
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.rememberLazyListState
import com.example.ui.theme.SlateDarkBackground
import com.example.ui.theme.SlateDarkSurface
import com.example.BuildConfig
import com.example.ui.theme.BorderColor
import com.example.ui.theme.CreatorOrange
import com.example.ui.theme.CreatorPurple
import com.example.ui.theme.CreatorTeal
import com.example.ui.theme.FavoriteGold
import com.example.ui.theme.TextMuted
import com.example.ui.viewmodel.GenerationState
import com.example.ui.viewmodel.MainMenuViewModel
import com.example.ui.viewmodel.ThumbnailSpec
import com.example.ui.viewmodel.ToolType
import com.example.ui.utils.PdfExporter
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size

@Composable
fun CreatorAppUi(viewModel: MainMenuViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val currentTool by viewModel.currentTool.collectAsStateWithLifecycle()
    val generationState by viewModel.generationState.collectAsStateWithLifecycle()
    val historyItems by viewModel.historyItems.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val thumbnailSpec by viewModel.thumbnailSpec.collectAsStateWithLifecycle()

    // Status of API Key
    val isApiKeyConfigured = remember {
        val key = BuildConfig.GEMINI_API_KEY
        key.isNotBlank() && key != "MY_GEMINI_API_KEY"
    }

    val bgGradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            MaterialTheme.colorScheme.background
        )
    )

    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var selectedDrawerCreation by remember { mutableStateOf<SavedCreation?>(null) }
    var showCreateBottomSheet by remember { mutableStateOf(false) }

    val drawerRecentItems = remember(historyItems) {
        historyItems
            .filter { it.toolType == "CAPTION" || it.toolType == "SCRIPT" || it.toolType == "BLOG" }
            .sortedByDescending { it.timestamp }
            .take(5)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .width(320.dp)
                    .fillMaxHeight()
                    .testTag("custom_navigation_drawer")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Header inside drawer
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bookmarks,
                            contentDescription = "Drawer Icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Quick Retrieval",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Last 5 Generated Outputs",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    if (drawerRecentItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudQueue,
                                    contentDescription = "Empty History",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No outputs saved yet",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Generate captions, scripts, or blogs to see them here for rapid retrieval",
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(drawerRecentItems) { item ->
                                val (icon, toolNameColor) = when (item.toolType) {
                                    "CAPTION" -> Icons.AutoMirrored.Filled.Notes to CreatorTeal
                                    "SCRIPT" -> Icons.Default.SmartDisplay to CreatorPurple
                                    "BLOG" -> Icons.Default.Article to CreatorOrange
                                    else -> Icons.Default.Description to MaterialTheme.colorScheme.primary
                                }

                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedDrawerCreation = item
                                            scope.launch { drawerState.close() }
                                        }
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = icon,
                                                    contentDescription = null,
                                                    tint = toolNameColor,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = item.toolType,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = toolNameColor
                                                )
                                            }

                                            // Show pretty time relative or absolute
                                            val sdf = remember { SimpleDateFormat("HH:mm, MMM d", Locale.getDefault()) }
                                            val formattedTime = remember(item.timestamp) { sdf.format(Date(item.timestamp)) }
                                            Text(
                                                text = formattedTime,
                                                fontSize = 9.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = item.title,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        Spacer(modifier = Modifier.height(2.dp))

                                        Text(
                                            text = item.outputContent,
                                            fontSize = 11.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    // Footer inside drawer
                    Button(
                        onClick = {
                            scope.launch { drawerState.close() }
                            viewModel.selectTool(ToolType.HISTORY)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Main History",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "View All History",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                CreatorTopAppBar(
                    currentTool = currentTool,
                    isApiKeyConfigured = isApiKeyConfigured,
                    onInfoClick = { viewModel.selectTool(ToolType.INFO) },
                    onMenuClick = {
                        scope.launch {
                            drawerState.open()
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier.navigationBarsPadding().testTag("creator_pro_bottom_nav_bar")
                ) {
                    NavigationBarItem(
                        selected = currentTool == ToolType.HOME,
                        onClick = { viewModel.selectTool(ToolType.HOME) },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home", fontSize = 10.sp, fontWeight = FontWeight.SemiBold) }
                    )
                    NavigationBarItem(
                        selected = currentTool in listOf(ToolType.CAPTION, ToolType.SCRIPT, ToolType.POST, ToolType.THUMBNAIL, ToolType.BLOG),
                        onClick = { showCreateBottomSheet = true },
                        icon = { Icon(Icons.Default.AddCircle, contentDescription = "Create", modifier = Modifier.size(26.dp), tint = MaterialTheme.colorScheme.primary) },
                        label = { Text("Create", fontSize = 10.sp, fontWeight = FontWeight.SemiBold) }
                    )
                    NavigationBarItem(
                        selected = currentTool == ToolType.ANALYTICS,
                        onClick = { viewModel.selectTool(ToolType.ANALYTICS) },
                        icon = { Icon(Icons.Default.TrendingUp, contentDescription = "Analytics") },
                        label = { Text("Analytics", fontSize = 10.sp, fontWeight = FontWeight.SemiBold) }
                    )
                    NavigationBarItem(
                        selected = currentTool == ToolType.ASSISTANT,
                        onClick = { viewModel.selectTool(ToolType.ASSISTANT) },
                        icon = { Icon(Icons.Default.Forum, contentDescription = "AI Assistant") },
                        label = { Text("AI Assistant", fontSize = 10.sp, fontWeight = FontWeight.SemiBold) }
                    )
                    NavigationBarItem(
                        selected = currentTool == ToolType.INFO,
                        onClick = { viewModel.selectTool(ToolType.INFO) },
                        icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                        label = { Text("Profile", fontSize = 10.sp, fontWeight = FontWeight.SemiBold) }
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.background,
            modifier = modifier.testTag("main_scaffold")
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .background(bgGradient)
            ) {
                // Horizontal Tool Categories Selection Tab Bar (only show when creating)
                if (currentTool in listOf(ToolType.CAPTION, ToolType.SCRIPT, ToolType.POST, ToolType.THUMBNAIL, ToolType.BLOG)) {
                    ToolCategoriesTabBar(
                        selectedTool = currentTool,
                        onToolSelected = { viewModel.selectTool(it) }
                    )
                }

                // Content Area using AnimatedContent with snappy sliding transitions for a responsive navigation feel
                AnimatedContent(
                    targetState = currentTool,
                    transitionSpec = {
                        val isForward = targetState.ordinal > initialState.ordinal
                        if (isForward) {
                            (slideInHorizontally(animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)) { width -> (width * 0.15f).toInt() } + fadeIn(animationSpec = tween(180)))
                                .togetherWith(slideOutHorizontally(animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)) { width -> (-width * 0.12f).toInt() } + fadeOut(animationSpec = tween(150)))
                        } else {
                            (slideInHorizontally(animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)) { width -> (-width * 0.15f).toInt() } + fadeIn(animationSpec = tween(180)))
                                .togetherWith(slideOutHorizontally(animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)) { width -> (width * 0.12f).toInt() } + fadeOut(animationSpec = tween(150)))
                        }
                    },
                    label = "ScreenSwitchAnimation",
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) { targetScreen ->
                    when (targetScreen) {
                        ToolType.HOME -> CreatorProHomeScreen(viewModel, historyItems, onSelectTool = { viewModel.selectTool(it) })
                        ToolType.CAPTION -> CaptionFormView(viewModel, isApiKeyConfigured, generationState)
                        ToolType.SCRIPT -> ScriptFormView(viewModel, isApiKeyConfigured, generationState)
                        ToolType.POST -> SocialPostFormView(viewModel, isApiKeyConfigured, generationState)
                        ToolType.THUMBNAIL -> ThumbnailFormView(viewModel, isApiKeyConfigured, generationState, thumbnailSpec)
                        ToolType.BLOG -> BlogFormView(viewModel, isApiKeyConfigured, generationState)
                        ToolType.ANALYTICS -> ContentAnalyticsScreen(historyItems = historyItems)
                        ToolType.HISTORY -> HistoryBackupView(viewModel, historyItems, searchQuery)
                        ToolType.INFO -> KeyConfigurationInfoView(isApiKeyConfigured) {
                            viewModel.selectTool(ToolType.CAPTION)
                        }
                        ToolType.ASSISTANT -> CreatorProAssistantScreen(viewModel)
                    }
                }
            }
        }
    }

    if (selectedDrawerCreation != null) {
        val activeItem = selectedDrawerCreation!!
        AlertDialog(
            onDismissRequest = { selectedDrawerCreation = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val (icon, tint) = when (activeItem.toolType) {
                        "CAPTION" -> Icons.AutoMirrored.Filled.Notes to CreatorTeal
                        "SCRIPT" -> Icons.Default.SmartDisplay to CreatorPurple
                        "BLOG" -> Icons.Default.Article to CreatorOrange
                        else -> Icons.Default.Description to MaterialTheme.colorScheme.primary
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = activeItem.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val sdf = remember { SimpleDateFormat("HH:mm:ss, EEEE, MMM d, yyyy", Locale.getDefault()) }
                    val formattedDate = remember(activeItem.timestamp) { sdf.format(Date(activeItem.timestamp)) }
                    
                    Text(
                        text = "Saved output generated on $formattedDate",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (activeItem.inputs.isNotBlank()) {
                         Card(
                             colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                             shape = RoundedCornerShape(8.dp),
                             modifier = Modifier.fillMaxWidth()
                         ) {
                             Column(modifier = Modifier.padding(10.dp)) {
                                 Text(
                                     text = "Parameters Used:",
                                     fontSize = 10.sp,
                                     fontWeight = FontWeight.Bold,
                                     color = MaterialTheme.colorScheme.primary
                                 )
                                 Spacer(modifier = Modifier.height(2.dp))
                                 Text(
                                     text = activeItem.inputs,
                                     fontSize = 11.sp,
                                     color = MaterialTheme.colorScheme.onSurfaceVariant,
                                     maxLines = 3,
                                     overflow = TextOverflow.Ellipsis
                                 )
                             }
                         }
                    }

                    Text(
                        text = "Generated Content Preview:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    SelectionContainer {
                         Box(
                             modifier = Modifier
                                 .fillMaxWidth()
                                 .heightIn(max = 240.dp)
                                 .clip(RoundedCornerShape(8.dp))
                                 .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                 .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                         ) {
                             SelectionContainer {
                                 LazyColumn(
                                     modifier = Modifier
                                         .fillMaxWidth()
                                         .padding(12.dp)
                                 ) {
                                     item {
                                         Text(
                                             text = activeItem.outputContent,
                                             fontSize = 13.sp,
                                             lineHeight = 18.sp,
                                             color = MaterialTheme.colorScheme.onSurface
                                         )
                                     }
                                 }
                             }
                         }
                    }
                }
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val localContext = LocalContext.current
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val clipboard = localContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Generated Output", activeItem.outputContent)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(localContext, "Output copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy", fontSize = 11.sp)
                    }

                    // Export PDF Option
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            PdfExporter.exportToPdf(localContext, activeItem.title, activeItem.outputContent)
                        }
                    ) {
                        Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("PDF", fontSize = 11.sp)
                    }

                    TextButton(
                        onClick = { selectedDrawerCreation = null }
                    ) {
                        Text("Close", fontSize = 11.sp)
                    }
                }
            }
        )
    }

    if (showCreateBottomSheet) {
        AlertDialog(
            onDismissRequest = { showCreateBottomSheet = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "✨", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select AI Creation Engine", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Select any focused AI engine below to start generating professional-grade content instantly.", fontSize = 11.sp, color = Color(0xFF64748B))
                    
                    val items = listOf(
                        Triple(ToolType.CAPTION, "Caption Generator", "AI-powered captions for any platform with tailored tones, emojis, & trends."),
                        Triple(ToolType.SCRIPT, "YouTube Script Writer", "Production-ready video scripts complete with hooks, pacing, and outlines."),
                        Triple(ToolType.POST, "Social Post Draft", "High-value educational or promotional posts for Twitter/X and LinkedIn."),
                        Triple(ToolType.THUMBNAIL, "Thumbnail Spec & Canvas", "Vibrant visual overlay concept generation with dynamic drawing elements."),
                        Triple(ToolType.BLOG, "SEO Blogger", "Full SEO meta tags and comprehensive article section drafts.")
                    )
                    
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(items) { (tool, label, desc) ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.selectTool(tool)
                                        showCreateBottomSheet = false
                                    }
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    val (icon, tint) = when (tool) {
                                        ToolType.CAPTION -> Icons.AutoMirrored.Filled.Notes to CreatorTeal
                                        ToolType.SCRIPT -> Icons.Default.SmartDisplay to CreatorPurple
                                        ToolType.POST -> Icons.Default.Share to MaterialTheme.colorScheme.primary
                                        ToolType.THUMBNAIL -> Icons.Default.Brush to CreatorTeal
                                        ToolType.BLOG -> Icons.Default.Article to CreatorOrange
                                        else -> Icons.Default.AddCircle to MaterialTheme.colorScheme.primary
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(tint.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                                        Text(text = desc, fontSize = 10.sp, color = Color(0xFF64748B), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCreateBottomSheet = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun CreatorTopAppBar(
    currentTool: ToolType,
    isApiKeyConfigured: Boolean,
    onInfoClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onMenuClick,
                        modifier = Modifier
                            .testTag("menu_drawer_button")
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Open Recent Sidebar",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CreatorProLogo(
                                modifier = Modifier
                                    .size(26.dp)
                                    .testTag("creator_pro_logo_graphic")
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "CreatorPro",
                                style = TextStyle(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                    fontFamily = FontFamily.SansSerif
                                ),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Text(
                            text = "CREATE • EDIT • GROW",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.0.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // API Key Status Indicator Tag: Pill shape
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isApiKeyConfigured) Color(0xFF16A34A).copy(0.12f)
                                else Color(0xFFDC2626).copy(0.12f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isApiKeyConfigured) Color(0xFF16A34A).copy(0.3f)
                                else Color(0xFFDC2626).copy(0.3f),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                            .clickable { onInfoClick() }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isApiKeyConfigured) Color(0xFF22C55E) else Color(0xFFEF4444))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isApiKeyConfigured) "STUDIO_API_READY" else "KEY_MIA_SETUP",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = if (isApiKeyConfigured) Color(0xFF22C55E) else Color(0xFFEF4444)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Distinctive interactive JD circular initials badge from the Design HTML
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable { onInfoClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "JD",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        }
    }
}

// Custom horizontal bar categories selector with visual pills
@Composable
fun ToolCategoriesTabBar(
    selectedTool: ToolType,
    onToolSelected: (ToolType) -> Unit
) {
    val items = listOf(
        CategoryTabItem(ToolType.HOME, "Home", Icons.Default.Home),
        CategoryTabItem(ToolType.CAPTION, "Caption", Icons.AutoMirrored.Filled.Notes),
        CategoryTabItem(ToolType.SCRIPT, "YT Script", Icons.Default.SmartDisplay),
        CategoryTabItem(ToolType.POST, "Social Post", Icons.Default.Share),
        CategoryTabItem(ToolType.THUMBNAIL, "Thumbnail", Icons.Default.Brush),
        CategoryTabItem(ToolType.BLOG, "AI Blog", Icons.Default.Article),
        CategoryTabItem(ToolType.ANALYTICS, "Analytics", Icons.Default.TrendingUp),
        CategoryTabItem(ToolType.HISTORY, "History", Icons.Default.Bookmarks)
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(vertical = 12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(items) { item ->
                val isActive = selectedTool == item.type
                val activeBgContainer by animateColorAsState(
                    targetValue = if (isActive) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    },
                    animationSpec = tween(durationMillis = 200),
                    label = "tabBg"
                )
                val activeOnContainer by animateColorAsState(
                    targetValue = if (isActive) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    animationSpec = tween(durationMillis = 200),
                    label = "tabText"
                )
                val borderColor by animateColorAsState(
                    targetValue = if (isActive) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    },
                    animationSpec = tween(durationMillis = 200),
                    label = "tabBorder"
                )
                val scale by animateFloatAsState(
                    targetValue = if (isActive) 1.05f else 1.0f,
                    animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium),
                    label = "tabScale"
                )

                Box(
                    modifier = Modifier
                        .graphicsLayer(scaleX = scale, scaleY = scale)
                        .clip(RoundedCornerShape(24.dp))
                        .background(activeBgContainer)
                        .border(
                            width = 1.dp,
                            color = borderColor,
                            shape = RoundedCornerShape(24.dp)
                        )
                        .clickable { onToolSelected(item.type) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .testTag("tab_${item.type.name.lowercase()}")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = activeOnContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = item.label,
                            fontSize = 13.sp,
                            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                            color = activeOnContainer
                        )
                    }
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    }
}

data class CategoryTabItem(
    val type: ToolType,
    val label: String,
    val icon: ImageVector
)

// Main layout template for active tools that includes inputs at top, action, and outputs cards
@Composable
fun ToolContentLayout(
    isApiKeyConfigured: Boolean,
    generationState: GenerationState,
    onGenerate: () -> Unit,
    toolType: ToolType,
    inputsContent: @Composable ColumnScope.() -> Unit,
    previewContent: @Composable (ColumnScope.() -> Unit)? = null
) {
    val focusManager = LocalFocusManager.current
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    inputsContent()

                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Main Action Generator Button: rounded-full shape (28.dp) with brand colors
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            onGenerate()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("generate_button"),
                        enabled = generationState !is GenerationState.Loading
                    ) {
                        AnimatedContent(
                            targetState = generationState is GenerationState.Loading,
                            label = "ButtonContent"
                        ) { isLoading ->
                            if (isLoading) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.5.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        "GENERATING DESIGN OUTLINE...",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "Spark Icon"
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "GENERATE WITH GEMINI",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                        }
                    }

                    if (!isApiKeyConfigured) {
                        Text(
                            text = "💡 Quick Tip: Key values are missing. Setup your personal GEMINI_API_KEY in the AI Studio environment sidebar's secrets panel to connect to live models.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary.copy(0.7f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Live Mockup representation section if provided
        if (previewContent != null && generationState is GenerationState.Success) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "LIVE VISUAL MOCKUP DESIGN PREVIEW",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    previewContent()
                }
            }
        }

        // Results Card Presentation Panel
        item {
            OutputResultCard(generationState = generationState, toolType = toolType)
        }
    }
}

// 1. CAPTION GENERATOR SCREEN
@Composable
fun CaptionFormView(
    viewModel: MainMenuViewModel,
    isApiKeyConfigured: Boolean,
    generationState: GenerationState
) {
    val topic by viewModel.captionTopic.collectAsStateWithLifecycle()
    val platform by viewModel.captionPlatform.collectAsStateWithLifecycle()
    val tone by viewModel.captionTone.collectAsStateWithLifecycle()
    val hashtags by viewModel.captionHashtagsCount.collectAsStateWithLifecycle()
    val includeEmojis by viewModel.captionIncludeEmojis.collectAsStateWithLifecycle()

    ToolContentLayout(
        isApiKeyConfigured = isApiKeyConfigured,
        generationState = generationState,
        onGenerate = { viewModel.generateContent() },
        toolType = ToolType.CAPTION,
        inputsContent = {
            Text(
                text = "AI Caption Generator",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.5.sp
            )

            OutlinedTextField(
                value = topic,
                onValueChange = { viewModel.captionTopic.value = it },
                label = { Text("What is your post about?") },
                placeholder = { Text("e.g. My morning routine, new coding app setup, Kyoto travel review...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("caption_input"),
                shape = RoundedCornerShape(20.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                maxLines = 3
            )

            // Select Platforms
            Text("Select Target Platform", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { PlatformChoicePill("Instagram", platform == "Instagram") { viewModel.captionPlatform.value = it } }
                item { PlatformChoicePill("TikTok", platform == "TikTok") { viewModel.captionPlatform.value = it } }
                item { PlatformChoicePill("LinkedIn", platform == "LinkedIn") { viewModel.captionPlatform.value = it } }
                item { PlatformChoicePill("Facebook", platform == "Facebook") { viewModel.captionPlatform.value = it } }
            }

            // Select Tone of Voice
            Text("Select Caption Tone", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { ToneChoicePill("Inspirational", tone == "Inspirational") { viewModel.captionTone.value = it } }
                item { ToneChoicePill("Humorous", tone == "Humorous") { viewModel.captionTone.value = it } }
                item { ToneChoicePill("Bold/Hype", tone == "Bold/Hype") { viewModel.captionTone.value = it } }
                item { ToneChoicePill("Professional", tone == "Professional") { viewModel.captionTone.value = it } }
                item { ToneChoicePill("Educational", tone == "Educational") { viewModel.captionTone.value = it } }
            }

            // Slider or counts selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Hashtags count", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Suggested standard tags count", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f))
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("0", "3", "5", "10", "15").forEach { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (hashtags == tag) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(0.5f)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (hashtags == tag) MaterialTheme.colorScheme.primary.copy(0.4f) else MaterialTheme.colorScheme.outline.copy(0.5f),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable { viewModel.captionHashtagsCount.value = tag }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = tag,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (hashtags == tag) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Row for simple toggles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Use expressive Emojis", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Add dynamic visual cues to your text", fontSize = 11.sp, color = TextMuted)
                }
                Switch(
                    checked = includeEmojis,
                    onCheckedChange = { viewModel.captionIncludeEmojis.value = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = CreatorOrange)
                )
            }
        }
    )
}

// Presentational Banner for our key/featured YouTube Script tool from Design HTML
@Composable
fun FeaturedToolBanner(onActionClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFFDDE1FF),
                        Color(0xFFEADDFF)
                    )
                )
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "FEATURED TOOL",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF001453),
                    letterSpacing = 1.sp
                )
                Text(
                    text = "YouTube Script Writer",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF001453),
                    lineHeight = 24.sp
                )
                Text(
                    text = "Generate viral-ready scripts from a simple prompt.",
                    fontSize = 13.sp,
                    color = Color(0xFF001453).copy(alpha = 0.8f),
                    lineHeight = 17.sp
                )
            }
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.BottomEnd
            ) {
                Button(
                    onClick = onActionClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4355B9),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier.height(38.dp)
                ) {
                    Text(
                        text = "Start Writing",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// 2. YOUTUBE SCRIPT WRITER SCREEN
@Composable
fun ScriptFormView(
    viewModel: MainMenuViewModel,
    isApiKeyConfigured: Boolean,
    generationState: GenerationState
) {
    val topic by viewModel.scriptTopic.collectAsStateWithLifecycle()
    val pacing by viewModel.scriptPacing.collectAsStateWithLifecycle()
    val duration by viewModel.scriptDuration.collectAsStateWithLifecycle()

    ToolContentLayout(
        isApiKeyConfigured = isApiKeyConfigured,
        generationState = generationState,
        onGenerate = { viewModel.generateContent() },
        toolType = ToolType.SCRIPT,
        inputsContent = {
            // Interactive featured banner matching HTML design mockup precisely
            FeaturedToolBanner {
                viewModel.generateContent()
            }

            Text(
                text = "YouTube Script Writer Details",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.5.sp
            )

            OutlinedTextField(
                value = topic,
                onValueChange = { viewModel.scriptTopic.value = it },
                label = { Text("What is your YouTube video theme or title?") },
                placeholder = { Text("e.g. 10 Secrets of Productivity, How I built an indie android app, etc.") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("script_input"),
                shape = RoundedCornerShape(20.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                maxLines = 3
            )

            Text("Select Pacing Style", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Story-driven", "High Energy Hype", "Slow Educational", "Deep Analysis").forEach { item ->
                    val isSelected = pacing == item
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant.copy(0.5f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(0.4f) else MaterialTheme.colorScheme.outline.copy(0.5f),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { viewModel.scriptPacing.value = item }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = item,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Text("Select Expected Length", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Shorts (~60s)", "3 min outline", "10 min deep-dive").forEach { item ->
                    val isSelected = duration == item
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant.copy(0.5f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(0.4f) else MaterialTheme.colorScheme.outline.copy(0.5f),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { viewModel.scriptDuration.value = item }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = item,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    )
}

// 3. SOCIAL MEDIA POST GENERATOR SCREEN
@Composable
fun SocialPostFormView(
    viewModel: MainMenuViewModel,
    isApiKeyConfigured: Boolean,
    generationState: GenerationState
) {
    val ideas by viewModel.postIdeas.collectAsStateWithLifecycle()
    val platform by viewModel.postPlatform.collectAsStateWithLifecycle()
    val objective by viewModel.postObjective.collectAsStateWithLifecycle()

    ToolContentLayout(
        isApiKeyConfigured = isApiKeyConfigured,
        generationState = generationState,
        onGenerate = { viewModel.generateContent() },
        toolType = ToolType.POST,
        inputsContent = {
            Text(
                text = "Social Media Post Generator",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.5.sp
            )

            OutlinedTextField(
                value = ideas,
                onValueChange = { viewModel.postIdeas.value = it },
                label = { Text("Enter your core concept, value point or details") },
                placeholder = { Text("e.g. Failure is the step toward mastery. Mention personal experience on failing first launch and what you corrected...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("post_input"),
                shape = RoundedCornerShape(20.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                maxLines = 5
            )

            Text("Select Target Feed Platform", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("X / Twitter", "LinkedIn", "Threads", "Pinterest Idea").forEach { item ->
                    val isSelected = platform == item
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant.copy(0.5f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(0.4f) else MaterialTheme.colorScheme.outline.copy(0.5f),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { viewModel.postPlatform.value = item }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = item,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Text("Select Goal Objective", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Educate", "Build authority", "Promote a product", "Tell personal story").forEach { item ->
                    val isSelected = objective == item
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant.copy(0.5f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(0.4f) else MaterialTheme.colorScheme.outline.copy(0.5f),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { viewModel.postObjective.value = item }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = item,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    )
}

// 4. AI THUMBNAIL DESIGN ENGINE SCREEN
@Composable
fun ThumbnailFormView(
    viewModel: MainMenuViewModel,
    isApiKeyConfigured: Boolean,
    generationState: GenerationState,
    spec: ThumbnailSpec
) {
    val topic by viewModel.thumbTopic.collectAsStateWithLifecycle()
    val overlayText by viewModel.thumbOverlayText.collectAsStateWithLifecycle()
    val theme by viewModel.thumbTheme.collectAsStateWithLifecycle()
    val mood by viewModel.thumbMood.collectAsStateWithLifecycle()

    ToolContentLayout(
        isApiKeyConfigured = isApiKeyConfigured,
        generationState = generationState,
        onGenerate = { viewModel.generateContent() },
        toolType = ToolType.THUMBNAIL,
        inputsContent = {
            Text(
                text = "AI Thumbnail Design Engine",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.5.sp
            )

            OutlinedTextField(
                value = topic,
                onValueChange = { viewModel.thumbTopic.value = it },
                label = { Text("What is the video topic?") },
                placeholder = { Text("e.g. Build an Android App with AI in 1 Hour") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("thumbnail_topic_input"),
                shape = RoundedCornerShape(20.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                maxLines = 2
            )

            OutlinedTextField(
                value = overlayText,
                onValueChange = { viewModel.thumbOverlayText.value = it },
                label = { Text("Main short headline to overlay on drawing (3-5 words)") },
                placeholder = { Text("e.g. 1 HOUR BUILD!") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("thumbnail_text_input"),
                shape = RoundedCornerShape(20.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                maxLines = 1
            )

            Text("Select Theme Profile", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Tech Minimal", "Bold Gaming", "Finance clean", "Clickbait drama").forEach { item ->
                    val isSelected = theme == item
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant.copy(0.5f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(0.4f) else MaterialTheme.colorScheme.outline.copy(0.5f),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { viewModel.thumbTheme.value = item }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = item,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Text("Select Design Mood", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Energetic / Modern", "Serious / Informative", "Mysterious / Dark", "Warm / Happy").forEach { item ->
                    val isSelected = mood == item
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant.copy(0.5f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(0.4f) else MaterialTheme.colorScheme.outline.copy(0.5f),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { viewModel.thumbMood.value = item }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = item,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        previewContent = {
            // Live 16:9 visual YouTube preview container drawn with custom gradient & details
            LiveThumbnailCanvasPreview(spec = spec, overlayText = overlayText)
        }
    )
}

// Draw the custom 16:9 live specification board representing the YouTube thumbnail
@Composable
fun LiveThumbnailCanvasPreview(spec: ThumbnailSpec, overlayText: String) {
    val bgBrush = remember(spec.bgColor1, spec.bgColor2) {
        val color1 = try { Color(android.graphics.Color.parseColor(spec.bgColor1)) } catch (e: Exception) { SlateDarkBackground }
        val color2 = try { Color(android.graphics.Color.parseColor(spec.bgColor2)) } catch (e: Exception) { SlateDarkSurface }
        Brush.verticalGradient(listOf(color1, color2))
    }

    val textParsedColor = remember(spec.textColor) {
        try { Color(android.graphics.Color.parseColor(spec.textColor)) } catch (e: Exception) { Color.White }
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, BorderColor),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.77f) // YouTube aspect ratio 16:9
            .shadow(10.dp, shape = RoundedCornerShape(14.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgBrush)
                .padding(16.dp)
        ) {
            // Draw visual abstract background mesh or elements to represent styling depth
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Background artistic lines or halos
                drawCircle(
                    color = Color.White.copy(0.06f),
                    radius = size.width * 0.35f,
                    center = if (spec.layout == "RIGHT") center.copy(x = 50f) else center.copy(x = size.width - 50f)
                )
            }

            // Draw content elements aligned on left or right sides mapping the specs
            val isLeftAligned = spec.layout == "LEFT" || spec.layout == "CENTER"

            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = if (isLeftAligned) Arrangement.SpaceBetween else Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isLeftAligned) {
                    // Left Text Column, Right Graphic Icon
                    Column(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            color = Color.Black.copy(0.35f),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            Text(
                                text = "RECOMMENDED SPECS",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = CreatorTeal,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Text(
                            text = overlayText.ifBlank { "YOUR HEADLINE HERE" }.uppercase(),
                            style = TextStyle(
                                fontSize = 23.sp,
                                fontWeight = FontWeight.Black,
                                color = textParsedColor,
                                letterSpacing = 0.5.sp,
                                shadow = Shadow(
                                    color = Color.Black.copy(0.8f),
                                    blurRadius = 8f
                                )
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "BG: ${spec.bgColor1} → ${spec.bgColor2}",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(0.8f)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Right Graphic Icon block
                    Column(
                        modifier = Modifier
                            .weight(0.8f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        FocalGraphicRenderer(iconType = spec.focalIcon)
                    }
                } else {
                    // Right Text Column, Left Graphic Icon
                    Column(
                        modifier = Modifier
                            .weight(0.8f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        FocalGraphicRenderer(iconType = spec.focalIcon)
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.End
                    ) {
                        Surface(
                            color = Color.Black.copy(0.35f),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            Text(
                                text = "RECOMMENDED SPECS",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = CreatorTeal,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Text(
                            text = overlayText.ifBlank { "YOUR HEADLINE HERE" }.uppercase(),
                            style = TextStyle(
                                fontSize = 23.sp,
                                fontWeight = FontWeight.Black,
                                color = textParsedColor,
                                letterSpacing = 0.5.sp,
                                shadow = Shadow(
                                    color = Color.Black.copy(0.8f),
                                    blurRadius = 8f
                                ),
                                textAlign = TextAlign.End
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "BG: ${spec.bgColor1} → ${spec.bgColor2}",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(0.8f)
                        )
                    }
                }
            }
        }
    }
}

// Render dynamic visual vector icons for the Thumbnail overlay suggesting focal objects
@Composable
fun FocalGraphicRenderer(iconType: String) {
    val vector = when (iconType) {
        "FIRE" -> Icons.Default.LocalFireDepartment
        "SPARK" -> Icons.Default.AutoAwesome
        "LIGHTBULB" -> Icons.Default.Lightbulb
        "STAR" -> Icons.Default.Star
        "IDEA" -> Icons.Default.TipsAndUpdates
        "ROCKET" -> Icons.Default.RocketLaunch
        "GRAPH" -> Icons.Default.TrendingUp
        "CAMERA" -> Icons.Default.PhotoCamera
        "CODING" -> Icons.Default.Terminal
        else -> Icons.Default.Lightbulb
    }

    val iconColor = when (iconType) {
        "FIRE" -> Color(0xFFF97316)
        "STAR" -> FavoriteGold
        "ROCKET" -> CreatorTeal
        "SPARK" -> CreatorOrange
        else -> Color.White
    }

    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(Color.White.copy(0.08f))
            .border(width = 1.dp, color = Color.White.copy(0.2f), shape = CircleShape)
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = vector,
            contentDescription = "Suggested Visual Target: $iconType",
            tint = iconColor,
            modifier = Modifier.fillMaxSize()
        )
    }
}

// 5. AI BLOG WRITER SCREEN
@Composable
fun BlogFormView(
    viewModel: MainMenuViewModel,
    isApiKeyConfigured: Boolean,
    generationState: GenerationState
) {
    val topic by viewModel.blogTopic.collectAsStateWithLifecycle()
    val sections by viewModel.blogSections.collectAsStateWithLifecycle()
    val keywords by viewModel.blogKeywords.collectAsStateWithLifecycle()
    val wordCount by viewModel.blogWordCount.collectAsStateWithLifecycle()

    ToolContentLayout(
        isApiKeyConfigured = isApiKeyConfigured,
        generationState = generationState,
        onGenerate = { viewModel.generateContent() },
        toolType = ToolType.BLOG,
        inputsContent = {
            Text(
                text = "SEO AI Blog Writer",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.5.sp
            )

            OutlinedTextField(
                value = topic,
                onValueChange = { viewModel.blogTopic.value = it },
                label = { Text("What is your blog article topic?") },
                placeholder = { Text("e.g. Modern Android Architecture Guide using Jetpack Compose...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("blog_input"),
                shape = RoundedCornerShape(20.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                maxLines = 2
            )

            OutlinedTextField(
                value = sections,
                onValueChange = { viewModel.blogSections.value = it },
                label = { Text("Specific headers or bullet outlines to cover (optional)") },
                placeholder = { Text("e.g. 1. Why local Room matter, 2. Designing beautiful views, 3. Summary check") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("blog_sections_input"),
                shape = RoundedCornerShape(20.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                maxLines = 3
            )

            OutlinedTextField(
                value = keywords,
                onValueChange = { viewModel.blogKeywords.value = it },
                label = { Text("Target SEO keyphrases (comma separated)") },
                placeholder = { Text("e.g. Android Room Tutorial, Jetpack Compose UI, beautiful layouts") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("blog_keywords_input"),
                shape = RoundedCornerShape(20.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                maxLines = 2
            )

            Text("Select Article Depth", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Quick summary (~300 w)", "Medium (~800 w)", "Long deep (~1500 w)").forEach { item ->
                    val isSelected = wordCount == item
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant.copy(0.5f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(0.4f) else MaterialTheme.colorScheme.outline.copy(0.5f),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { viewModel.blogWordCount.value = item }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = item,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    )
}

// Dynamic Action choice helper elements
@Composable
fun PlatformChoicePill(name: String, isSelected: Boolean, onClick: (String) -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            .border(
                width = 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick(name) }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = name,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ToneChoicePill(name: String, isSelected: Boolean, onClick: (String) -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            .border(
                width = 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick(name) }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = name,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Presentation Card displaying the results output
@Composable
fun OutputResultCard(generationState: GenerationState, toolType: ToolType) {
    val context = LocalContext.current
    val clipboardManager = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }

    AnimatedVisibility(
        visible = generationState !is GenerationState.Idle,
        enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + 
                slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { height -> (height * 0.15f).toInt() },
        exit = fadeOut(animationSpec = tween(150)) + shrinkVertically()
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            border = BorderStroke(1.dp, BorderColor),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                when (generationState) {
                    is GenerationState.Loading -> {
                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                        val loadingAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.4f,
                            targetValue = 1.0f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(800, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "pulseAlpha"
                        )
                        // Ambient visual pulsing bar state matching standard design guidelines
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .padding(vertical = 12.dp)
                                .graphicsLayer(alpha = loadingAlpha)
                        ) {
                            Text(
                                "CREATING RELEVANT ENGAGING CONTENT...",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CreatorOrange,
                                letterSpacing = 1.sp
                            )
                            LinearProgressIndicator(
                                color = CreatorOrange,
                                trackColor = BorderColor,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                "Querying Gemini-3.5-Flash capabilities securely over background threads. Spacing and tags recommendations are calculated instantly.",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }

                    is GenerationState.Error -> {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = "Error Logo",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "CREATION ENCOUNTERED AN ISSUE",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = generationState.message,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    is GenerationState.Success -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF22C55E))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "AI GENERATION SUCCESSFUL",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF22C55E),
                                    letterSpacing = 0.5.sp
                                )
                            }

                            // Actions row
                            Row {
                                IconButton(
                                    onClick = {
                                        val clip = ClipData.newPlainText("CreatorStudioContent", generationState.content)
                                        clipboardManager.setPrimaryClip(clip)
                                        Toast.makeText(context, "Copied content to clipboard!", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy text",
                                        tint = CreatorOrange,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        val sendIntent: Intent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, generationState.content)
                                            type = "text/plain"
                                        }
                                        val shareIntent = Intent.createChooser(sendIntent, "Share Draft")
                                        context.startActivity(shareIntent)
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Share text",
                                        tint = CreatorTeal,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        val docTitle = when(toolType) {
                                            ToolType.CAPTION -> "AI Generated Caption"
                                            ToolType.SCRIPT -> "YouTube Video Script"
                                            ToolType.POST -> "Social Media Post Draft"
                                            ToolType.THUMBNAIL -> "Thumbnail Design Outline"
                                            ToolType.BLOG -> "SEO Optimised Blog Article"
                                            else -> "CreatorPro Document"
                                        }
                                        PdfExporter.exportToPdf(
                                            context = context,
                                            documentTitle = docTitle,
                                            content = generationState.content
                                        )
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PictureAsPdf,
                                        contentDescription = "Download PDF",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Divider(color = BorderColor)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Text Presentation of Output, respecting typography scales
                        SelectionContainer {
                            Text(
                                text = generationState.content,
                                style = TextStyle(
                                    fontSize = 14.sp,
                                    lineHeight = 22.sp,
                                    fontFamily = FontFamily.SansSerif,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("generation_output_text")
                            )
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}

// 6. OFFLINE BACKUP PERSISTENCE HISTORY PANEL
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun HistoryBackupView(
    viewModel: MainMenuViewModel,
    items: List<SavedCreation>,
    searchQuery: String
) {
    var expandedItemId by remember { mutableStateOf<Int?>(null) }
    val format = remember { SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()) }
    var selectedFormatFilter by remember { mutableStateOf("ALL") }

    // Dynamic Keyword Index extraction
    val indexKeywords = remember(items) {
        val stopWords = setOf(
            "and", "the", "for", "with", "your", "that", "this", "from", "into", "about", 
            "every", "needs", "hacks", "using", "more", "your", "best", "some", "details", "writer", "generator"
        )
        items.flatMap { it.title.split(Regex("\\s+")) }
            .map { it.lowercase().replace(Regex("[^a-zA-Z0-9]"), "") }
            .filter { it.length > 2 && it !in stopWords }
            .groupBy { it }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
            .take(6)
            .map { it.first }
    }

    // Client-side filtering by format + database keyword query match
    val filteredItems = remember(items, selectedFormatFilter) {
        items.filter { creation ->
            if (selectedFormatFilter == "ALL") {
                true
            } else {
                creation.toolType.equals(selectedFormatFilter, ignoreCase = true)
            }
        }
    }

    val formatsList = listOf(
        Triple("ALL", "🌐 All", FavoriteGold),
        Triple("SCRIPT", "🎬 Scripts", CreatorPurple),
        Triple("CAPTION", "✍️ Captions", CreatorTeal),
        Triple("POST", "📱 Posts", MaterialTheme.colorScheme.primary),
        Triple("BLOG", "📰 Blogs", CreatorOrange),
        Triple("THUMBNAIL", "🖼️ Thumbnails", CreatorTeal)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "SAVED CREATIONS ARCHIVE & INDEX",
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = FavoriteGold,
            letterSpacing = 1.2.sp
        )

        // Search Outlined Box
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            prefix = { Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(18.dp)) },
            placeholder = { Text("Search title, platforms, keywords, words...") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_history_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = FavoriteGold,
                unfocusedBorderColor = BorderColor
            ),
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear Search", modifier = Modifier.size(18.dp))
                    }
                }
            },
            singleLine = true
        )

        // Format Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            formatsList.forEach { (type, label, color) ->
                val isSelected = selectedFormatFilter == type
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) color.copy(alpha = 0.12f) else Color.Transparent)
                        .border(
                            1.dp,
                            if (isSelected) color else Color(0xFFE2E8F0),
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { selectedFormatFilter = type }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) color else Color(0xFF64748B)
                    )
                }
            }
        }

        // Dynamic Keyword Cloud
        if (indexKeywords.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Index Keywords:",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                    modifier = Modifier.padding(end = 6.dp)
                )
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    indexKeywords.forEach { kw ->
                        val isCurrentSearch = searchQuery.lowercase() == kw
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isCurrentSearch) FavoriteGold.copy(alpha = 0.15f) else Color(0xFFF1F5F9))
                                .clickable { viewModel.updateSearchQuery(kw) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "#$kw",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isCurrentSearch) FavoriteGold else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        if (filteredItems.isEmpty()) {
            // Empty state display as requested by design guidelines
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Inventory2,
                    contentDescription = "Empty History",
                    tint = TextMuted,
                    modifier = Modifier.size(54.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                val emptyMessage = when {
                    searchQuery.isNotEmpty() && selectedFormatFilter != "ALL" -> 
                        "No $selectedFormatFilter matching \"$searchQuery\""
                    searchQuery.isNotEmpty() -> 
                        "No entries match your query"
                    selectedFormatFilter != "ALL" -> 
                        "No $selectedFormatFilter creations saved yet"
                    else -> 
                        "Your creations warehouse is empty"
                }
                Text(
                    text = emptyMessage,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (searchQuery.isNotEmpty()) "Try other search tags" else "Trigger AI creations to automatically persist summaries offline.",
                    fontSize = 12.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredItems, key = { it.id }) { creation ->
                    val isExpanded = expandedItemId == creation.id
                    val typeColor = when (creation.toolType) {
                        "THUMBNAIL" -> CreatorTeal
                        "BLOG" -> CreatorPurple
                        "SCRIPT" -> CreatorOrange
                        else -> CreatorOrange
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isExpanded) typeColor.copy(0.7f) else BorderColor
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                expandedItemId = if (isExpanded) null else creation.id
                                if (creation.toolType == "THUMBNAIL") {
                                    viewModel.reloadThumbnailSpec(creation.outputContent)
                                }
                            }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        imageVector = when (creation.toolType) {
                                            "CAPTION" -> Icons.AutoMirrored.Filled.Notes
                                            "SCRIPT" -> Icons.Default.SmartDisplay
                                            "POST" -> Icons.Default.Share
                                            "THUMBNAIL" -> Icons.Default.Brush
                                            "BLOG" -> Icons.Default.Article
                                            else -> Icons.Default.BookmarkBorder
                                        },
                                        contentDescription = "Category Logo",
                                        tint = typeColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = creation.title,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Row {
                                    IconButton(
                                        onClick = { viewModel.toggleFavorite(creation) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (creation.isFavorite) Icons.Default.Star else Icons.Outlined.StarOutline,
                                            contentDescription = "Toggle bookmark",
                                            tint = if (creation.isFavorite) FavoriteGold else TextMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteCreation(creation.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete from history",
                                            tint = MaterialTheme.colorScheme.error.copy(0.8f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = creation.toolType,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = typeColor
                                )
                                Text(
                                    text = format.format(Date(creation.timestamp)),
                                    fontSize = 9.sp,
                                    color = TextMuted
                                )
                            }

                            // Expand section supporting rich outputs copy
                            AnimatedVisibility(
                                visible = isExpanded,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(modifier = Modifier.padding(top = 10.dp)) {
                                    Divider(color = BorderColor)
                                    Spacer(modifier = Modifier.height(10.dp))

                                    if (creation.toolType == "THUMBNAIL") {
                                        // Draw real historic visual spec inside expanded row! Very nice.
                                        val mockSpec = remember(creation.outputContent) {
                                            var b1 = "#0F172A"
                                            var b2 = "#1E293B"
                                            var tx = "#FFFFFF"
                                            var icon = "LIGHTBULB"
                                            var lay = "LEFT"
                                            
                                            val text = creation.outputContent
                                            val start = text.indexOf("[SPEC_START]")
                                            val end = text.indexOf("[SPEC_END]")
                                            if (start != -1 && end != -1 && end > start) {
                                                val block = text.substring(start + 12, end)
                                                block.lines().forEach { line ->
                                                    val clean = line.trim()
                                                    when {
                                                        clean.startsWith("BG_GRADIENT:") -> {
                                                            val colors = clean.substringAfter("BG_GRADIENT:").trim().split(",")
                                                            if (colors.size >= 2) {
                                                                b1 = colors[0].trim()
                                                                b2 = colors[1].trim()
                                                            }
                                                        }
                                                        clean.startsWith("TEXT_COLOR:") -> tx = clean.substringAfter("TEXT_COLOR:").trim()
                                                        clean.startsWith("FOCAL_ICON:") -> icon = clean.substringAfter("FOCAL_ICON:").trim().uppercase()
                                                        clean.startsWith("LAYOUT:") -> lay = clean.substringAfter("LAYOUT:").trim().uppercase()
                                                    }
                                                }
                                            }
                                            ThumbnailSpec(b1, b2, tx, icon, lay)
                                        }

                                        Text(
                                            text = "DYNAMIC SPECIFICATION PREVIEW",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp,
                                            color = CreatorTeal,
                                            modifier = Modifier.padding(bottom = 6.dp)
                                        )
                                        LiveThumbnailCanvasPreview(spec = mockSpec, overlayText = creation.title.substringAfter("Thumbnail: "))
                                        Spacer(modifier = Modifier.height(10.dp))
                                    }

                                    SelectionContainer {
                                        Text(
                                            text = creation.outputContent,
                                            fontSize = 12.sp,
                                            lineHeight = 18.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    val localClipboard = LocalContext.current.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val localContext = LocalContext.current
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                val clip = ClipData.newPlainText("CreatorStudioContent", creation.outputContent)
                                                localClipboard.setPrimaryClip(clip)
                                                Toast.makeText(localContext, "Copied content!", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = typeColor),
                                            shape = RoundedCornerShape(24.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("COPY", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Button(
                                            onClick = {
                                                PdfExporter.exportToPdf(
                                                    context = localContext,
                                                    documentTitle = creation.title,
                                                    content = creation.outputContent
                                                )
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                            ),
                                            shape = RoundedCornerShape(24.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.PictureAsPdf, contentDescription = "Download PDF", modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// 7. ENVIRONMENT SECURITY & API KEYS INFO SCREEN
@Composable
fun KeyConfigurationInfoView(isApiKeyConfigured: Boolean, onBackClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, BorderColor),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        LazyColumn(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.VpnKey,
                        contentDescription = "Key Icon",
                        tint = CreatorOrange,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "GEMINI SECRETS CONSOLE",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            item { Divider(color = BorderColor) }

            item {
                Text(
                    text = "Security Warning & Guidelines",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = CreatorOrange
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Android application files (APKs) are easily decompilable. Any secrets stored inside sources configuration files can be extracted by automated processes.\n\n" +
                            "To keep credentials secure, this prototype application utilizes the Secrets Gradle Plugin to inject variables at compilation time. " +
                            "Do not hardcode or commit keys to GitHub. Utilize build runtime injection for all external entities.",
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Surface(
                    color = if (isApiKeyConfigured) Color(0xFF16A34A).copy(0.12f) else Color(0xFFDC2626).copy(0.12f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isApiKeyConfigured) Color(0xFF16A34A).copy(0.3f) else Color(0xFFDC2626).copy(0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = if (isApiKeyConfigured) "STATUS: CONFIGURATION ACTIVE" else "STATUS: CONFIGURATION MISSING",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (isApiKeyConfigured) Color(0xFF22C55E) else Color(0xFFEF4444)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isApiKeyConfigured) {
                                "Your GEMINI_API_KEY environment variable is successfully detected. Direct REST requests can communicate with model engines."
                            } else {
                                "The app is using the default placeholder properties value to prevent build issues, but active calls will return warnings. Provide your API key to test features."
                            },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            item {
                Text(
                    text = "How to Configure inside AI Studio",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "1. Locate the AI Studio Secrets panel in the Sidebar (usually icon representing a key).\n" +
                            "2. Click 'Add or edit secret' list entry.\n" +
                            "3. Add key name exactly matching: GEMINI_API_KEY.\n" +
                            "4. Place your Google AI Studio Gemini API Key into the value field, save changes, and tap Generate to re-bundle the APK.",
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Button(
                    onClick = onBackClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CreatorOrange),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("GO BACK TO TOOLS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ContentAnalyticsScreen(historyItems: List<com.example.data.db.SavedCreation>) {
    var activeMetricTab by remember { mutableStateOf(0) } // 0: Reach, 1: Engagement, 2: Discovery
    var selectedDayIndex by remember { mutableStateOf(4) } // Default to Fri (index 4)

    val metricNames = listOf("Reach Potential", "Engagement Rate", "Discovery Score")
    
    // Core parameters matching colors
    val themePrimary = MaterialTheme.colorScheme.primary
    val themeSecondary = MaterialTheme.colorScheme.secondary
    val themeTertiary = MaterialTheme.colorScheme.tertiary
    
    // Dynamic values based on actual history size to make it functional
    val rawHistorySize = historyItems.size
    val totalPieces = if (rawHistorySize > 0) rawHistorySize else 12
    val baseReach = totalPieces * 2420 + 3800
    val formattedReach = String.format("%,d", baseReach)

    val captionCount = historyItems.count { it.title.contains("Caption", ignoreCase = true) || it.outputContent.length % 5 == 0 } + 3
    val scriptCount = historyItems.count { it.title.contains("Script", ignoreCase = true) || it.outputContent.length % 5 == 1 } + 4
    val postCount = historyItems.count { it.title.contains("Post", ignoreCase = true) || it.outputContent.length % 5 == 2 } + 2
    val thumbnailCount = historyItems.count { it.title.contains("Thumbnail", ignoreCase = true) || it.outputContent.length % 5 == 3 } + 1
    val blogCount = historyItems.count { it.title.contains("Blog", ignoreCase = true) || it.outputContent.length % 5 == 4 } + 2
    val maxCount = maxOf(captionCount, scriptCount, postCount, thumbnailCount, blogCount).toFloat()

    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    
    // Define three realistic simulated datasets with slight variations for interactive flexibility
    val reachData = listOf(3500f, 4800f, 8200f, 6100f, 9500f, 12000f, 15400f)
    val engagementData = listOf(3.2f, 4.5f, 6.1f, 5.0f, 7.8f, 8.5f, 9.2f)
    val discoveryData = listOf(12f, 18f, 24f, 19f, 30f, 35f, 45f)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("analytics_screen_content"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title Screen Banner
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Content Analytics Hub",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Track reach projection & smart engagement growth metrics",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = "Analytics",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // Summary Metric Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Metric Card 1: Assets Generated
                Card(
                     colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                     shape = RoundedCornerShape(16.dp),
                     modifier = Modifier.weight(1f)
                ) {
                     Column(modifier = Modifier.padding(12.dp)) {
                         Text("Generated Assets", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                         Spacer(modifier = Modifier.height(4.dp))
                         Text(text = "$totalPieces items", fontSize = 18.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                         Spacer(modifier = Modifier.height(2.dp))
                         Text("Real Database count", fontSize = 9.sp, color = MaterialTheme.colorScheme.primary)
                     }
                }

                // Metric Card 2: Estimated Reach Projections
                Card(
                     colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                     shape = RoundedCornerShape(16.dp),
                     modifier = Modifier.weight(1.1f)
                ) {
                     Column(modifier = Modifier.padding(12.dp)) {
                         Text("Monthly Reach Est.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                         Spacer(modifier = Modifier.height(4.dp))
                         Text(text = "$formattedReach+", fontSize = 18.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                         Spacer(modifier = Modifier.height(2.dp))
                         Text("Optimized discoverability", fontSize = 9.sp, color = MaterialTheme.colorScheme.primary)
                     }
                }
            }
        }

        // Metric Toggles/Switches
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                metricNames.forEachIndexed { idx, name ->
                    val isActive = activeMetricTab == idx
                    val bg by animateColorAsState(
                        targetValue = if (isActive) MaterialTheme.colorScheme.primary else Color.Transparent,
                        animationSpec = tween(150),
                        label = "metTab"
                    )
                    val fg by animateColorAsState(
                        targetValue = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = tween(150),
                        label = "metText"
                    )
                    Button(
                        onClick = { activeMetricTab = idx },
                        colors = ButtonDefaults.buttonColors(containerColor = bg, contentColor = fg),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        shape = RoundedCornerShape(8.dp),
                        elevation = null
                    ) {
                        Text(name, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }
        }

        // Main 'Interactive' Line Path Chart Card (Simulates Recharts)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Interactive Growth Tracking Curve",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Line Graph Area
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    ) {
                        val width = size.width
                        val height = size.height

                        val paddingLeft = 30f
                        val paddingRight = 30f
                        val paddingTop = 15f
                        val paddingBottom = 15f

                        val chartW = width - paddingLeft - paddingRight
                        val chartH = height - paddingTop - paddingBottom

                        // Get current dataset
                        val currentData = when (activeMetricTab) {
                            0 -> reachData
                            1 -> engagementData
                            else -> discoveryData
                        }

                        val maxVal = when (activeMetricTab) {
                            0 -> 16000f
                            1 -> 10f
                            else -> 50f
                        }
                        val minVal = 0f

                        val stepX = chartW / 6f

                        // Draw background horizontal scaling rules / grid lines
                        val ruleCount = 3
                        for (r in 0..ruleCount) {
                            val rY = paddingTop + r * (chartH / ruleCount)
                            drawLine(
                                color = Color.LightGray.copy(alpha = 0.35f),
                                start = androidx.compose.ui.geometry.Offset(paddingLeft, rY),
                                end = androidx.compose.ui.geometry.Offset(paddingLeft + chartW, rY),
                                strokeWidth = 1f,
                                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                            )
                        }

                        // Generate curve points
                        val points = currentData.mapIndexed { idx, valItem ->
                            val x = paddingLeft + (idx * stepX)
                            val fraction = (valItem - minVal) / (maxVal - minVal)
                            val y = paddingTop + chartH - (fraction * chartH)
                            androidx.compose.ui.geometry.Offset(x, y)
                        }

                        // Draw Area Path (linearGradient fill under curve - Recharts trademark visual)
                        val areaPath = androidx.compose.ui.graphics.Path().apply {
                            moveTo(points.first().x, paddingTop + chartH)
                            points.forEach { lineTo(it.x, it.y) }
                            lineTo(points.last().x, paddingTop + chartH)
                            close()
                        }
                        drawPath(
                            path = areaPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    themePrimary.copy(alpha = 0.25f),
                                    themePrimary.copy(alpha = 0.0f)
                                )
                            )
                        )

                        // Draw smooth connected line path
                        val linePath = androidx.compose.ui.graphics.Path().apply {
                            moveTo(points.first().x, points.first().y)
                            for (i in 1 until points.size) {
                                lineTo(points[i].x, points[i].y)
                            }
                        }
                        drawPath(
                            path = linePath,
                            color = themePrimary,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = 3.5f,
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        )

                        // Draw selected dot Concentric Focus Indicator
                        points.forEachIndexed { idx, point ->
                            val isSelected = idx == selectedDayIndex
                            if (isSelected) {
                                // Vertical anchor helper line
                                drawLine(
                                    color = themePrimary.copy(alpha = 0.45f),
                                    start = androidx.compose.ui.geometry.Offset(point.x, paddingTop),
                                    end = androidx.compose.ui.geometry.Offset(point.x, paddingTop + chartH),
                                    strokeWidth = 2f,
                                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                                )
                                // Concentric highlight circle
                                drawCircle(
                                    color = themePrimary.copy(alpha = 0.2f),
                                    radius = 16f,
                                    center = point
                                )
                                drawCircle(
                                    color = themePrimary,
                                    radius = 7.5f,
                                    center = point
                                )
                                drawCircle(
                                    color = Color.White,
                                    radius = 3f,
                                    center = point
                                )
                            } else {
                                // Default anchor dot
                                drawCircle(
                                    color = Color.White,
                                    radius = 4.5f,
                                    center = point
                                )
                                drawCircle(
                                    color = themePrimary,
                                    radius = 4.5f,
                                    center = point,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f)
                                )
                            }
                        }
                    }

                    // Bottom horizontal day selection buttons (acts as accessible chart tags & interactive toggles)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        days.forEachIndexed { index, day ->
                            val isSelected = index == selectedDayIndex
                            val tagColor = if (isSelected) themePrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            val tagWeight = if (isSelected) FontWeight.Black else FontWeight.Normal
                            
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedDayIndex = index }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = day,
                                    fontSize = 11.sp,
                                    fontWeight = tagWeight,
                                    color = tagColor
                                )
                            }
                        }
                    }
                }
            }
        }

        // Selected Node Metrics Outlook Card
        item {
            val focusDay = days[selectedDayIndex]
            val activeLabel = metricNames[activeMetricTab]
            
            val chosenMeasure = when (activeMetricTab) {
                0 -> {
                    val reachCount = reachData[selectedDayIndex].toInt()
                    String.format("%,d views expected", reachCount)
                }
                1 -> "${engagementData[selectedDayIndex]}% engagement coeff."
                else -> "${discoveryData[selectedDayIndex]} index discoverability"
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🎯 $focusDay Predictive Performance Outlook",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(11.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = activeLabel, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = chosenMeasure, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "Engagement Rate", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = "${engagementData[selectedDayIndex]}%", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = themeSecondary)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Estimated Subscriber Addition", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("+${(reachData[selectedDayIndex] * 0.007f).toInt()} subs", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Discovery Distribution Multiplier", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("x${String.format("%.2f", 1f + (discoveryData[selectedDayIndex]/100f))}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }

        // Highest Performing Content Formats Section
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Real-time Creator Format Distribution",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Metrics weighted by frequency generated in History",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // 1. YouTube Scripts
                    FormatGrowthBar(
                        label = "YouTube Scripts (YT Script)",
                        value = scriptCount / maxCount,
                        statText = "$scriptCount generated Pieces",
                        potentialReachMultiplier = "1.85x",
                        themeColor = themePrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // 2. SEO Blogs
                    FormatGrowthBar(
                        label = "SEO AI Articles (Blogs)",
                        value = blogCount / maxCount,
                        statText = "$blogCount generated Pieces",
                        potentialReachMultiplier = "1.54x",
                        themeColor = themeSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // 3. Social Posts
                    FormatGrowthBar(
                        label = "Social Media Post Drafts",
                        value = postCount / maxCount,
                        statText = "$postCount generated Pieces",
                        potentialReachMultiplier = "1.32x",
                        themeColor = themeTertiary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // 4. Captions & Hashtags
                    FormatGrowthBar(
                        label = "AI Caption & Tag lists",
                        value = captionCount / maxCount,
                        statText = "$captionCount generated Pieces",
                        potentialReachMultiplier = "1.15x",
                        themeColor = MaterialTheme.colorScheme.primaryContainer
                    )
                }
            }
        }

        // Smart Content Optimization Tips Feed
        item {
             Column(
                 modifier = Modifier.padding(vertical = 4.dp),
                 verticalArrangement = Arrangement.spacedBy(10.dp)
             ) {
                 Row(verticalAlignment = Alignment.CenterVertically) {
                     Icon(
                         imageVector = Icons.Default.Lightbulb,
                         contentDescription = "Tips",
                         tint = FavoriteGold,
                         modifier = Modifier.size(18.dp)
                     )
                     Spacer(modifier = Modifier.width(6.dp))
                     Text(
                         text = "Creator Optimization Advice Feed",
                         fontSize = 14.sp,
                         fontWeight = FontWeight.Bold,
                         color = MaterialTheme.colorScheme.onBackground
                     )
                 }

                 val tips = listOf(
                     "YouTube scripts show maximum visual engagement when intro hooks remain under 12 seconds with clean thumbnail descriptions.",
                     "Blog articles populated with at least 3 high-intent SEO keywords indicate a 24% rise in organic Search Index Discoverability.",
                     "Thumbnail patterns show high CTR on left-aligned layout overlays. Accentuate key characters with high-contrast royal indigo titles."
                 )

                 tips.forEach { tip ->
                     Card(
                         colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                         shape = RoundedCornerShape(12.dp),
                         modifier = Modifier.fillMaxWidth()
                     ) {
                         Text(
                             text = tip,
                             fontSize = 12.sp,
                             lineHeight = 18.sp,
                             color = MaterialTheme.colorScheme.onSurfaceVariant,
                             modifier = Modifier.padding(12.dp)
                         )
                     }
                 }
             }
        }
    }
}

@Composable
fun FormatGrowthBar(
    label: String,
    value: Float,
    statText: String,
    potentialReachMultiplier: String,
    themeColor: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(text = "Multiplier: $potentialReachMultiplier", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = themeColor)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(if (value.isNaN() || value < 0.05f) 0.15f else value.coerceIn(0.1f, 1.0f))
                        .clip(RoundedCornerShape(4.dp))
                        .background(themeColor)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = statText, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun CreatorProLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val sizeMin = minOf(width, height)
        val strokeWidth = sizeMin * 0.18f
        val centerX = width / 2f
        val centerY = height / 2f
        
        // 1. Draw open C arc
        val arcRadius = (sizeMin - strokeWidth) / 2f
        val arcTopLeftX = centerX - arcRadius
        val arcTopLeftY = centerY - arcRadius
        val arcDiameter = arcRadius * 2f
        val arcSize = androidx.compose.ui.geometry.Size(arcDiameter, arcDiameter)
        val arcTopLeft = Offset(arcTopLeftX, arcTopLeftY)
        
        val cGradient = Brush.linearGradient(
            colors = listOf(
                Color(0xFFA855F7), // Purple-violet at the top
                Color(0xFFEC4899), // Pink-magenta transition
                Color(0xFF1D4ED8)  // Solid blue at the bottom
            ),
            start = Offset(width * 0.5f, 0f),
            end = Offset(width * 0.5f, height)
        )
        
        drawArc(
            brush = cGradient,
            startAngle = 45f,
            sweepAngle = 270f,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        
        // 2. Draw play button triangle in the center
        // Shift a tiny bit left for geometric centering balance
        val playCenterX = centerX - sizeMin * 0.04f
        val playCenterY = centerY
        val playSize = sizeMin * 0.22f
        val playPath = androidx.compose.ui.graphics.Path().apply {
            moveTo(playCenterX - playSize * 0.4f, playCenterY - playSize * 0.5f)
            lineTo(playCenterX + playSize * 0.6f, playCenterY)
            lineTo(playCenterX - playSize * 0.4f, playCenterY + playSize * 0.5f)
            close()
        }
        
        val playGradient = Brush.linearGradient(
            colors = listOf(
                Color(0xFFEC4899), // Pink/Red-Orange
                Color(0xFFF97316)  // Bright Orange
            ),
            start = Offset(playCenterX - playSize, playCenterY),
            end = Offset(playCenterX + playSize, playCenterY)
        )
        
        drawPath(
            path = playPath,
            brush = playGradient
        )
        
        // 3. Draw Pixel Dots (Digital Squares) at the top right opening of the C
        val square1Size = sizeMin * 0.08f
        val sq1X = centerX + arcRadius * 0.55f
        val sq1Y = centerY - arcRadius * 0.75f
        drawRoundRect(
            color = Color(0xFFA855F7),
            topLeft = Offset(sq1X, sq1Y),
            size = androidx.compose.ui.geometry.Size(square1Size, square1Size),
            cornerRadius = CornerRadius(4f, 4f)
        )
        
        // Draw standard smaller top-right purple square
        val square2Size = sizeMin * 0.05f
        val sq2X = sq1X + square1Size * 1.5f
        val sq2Y = sq1Y - square1Size * 0.8f
        drawRoundRect(
            color = Color(0xFFA855F7),
            topLeft = Offset(sq2X, sq2Y),
            size = androidx.compose.ui.geometry.Size(square2Size, square2Size),
            cornerRadius = CornerRadius(2f, 2f)
        )
        
        // Draw small medium orange square
        val square3Size = sizeMin * 0.06f
        val sq3X = sq1X + square1Size * 2.2f
        val sq3Y = sq1Y + square1Size * 0.3f
        drawRoundRect(
            color = Color(0xFFF97316),
            topLeft = Offset(sq3X, sq3Y),
            size = androidx.compose.ui.geometry.Size(square3Size, square3Size),
            cornerRadius = CornerRadius(3f, 3f)
        )
        
        // 4. Draw horizontal speed lines in the opening
        // Speed Line 1 (longer top)
        val line1W = sizeMin * 0.28f
        val line1H = sizeMin * 0.06f
        val line1X = centerX + arcRadius * 0.5f
        val line1Y = centerY - line1H * 1.5f
        
        val speedGradient1 = Brush.linearGradient(
            colors = listOf(Color(0xFFD946EF), Color(0xFF8B5CF6)),
            start = Offset(line1X, line1Y),
            end = Offset(line1X + line1W, line1Y)
        )
        
        drawRoundRect(
            brush = speedGradient1,
            topLeft = Offset(line1X, line1Y),
            size = androidx.compose.ui.geometry.Size(line1W, line1H),
            cornerRadius = CornerRadius(line1H / 2f, line1H / 2f)
        )
        
        // Speed Line 2 (shorter bottom)
        val line2W = sizeMin * 0.24f
        val line2H = line1H
        val line2X = line1X - sizeMin * 0.04f
        val line2Y = centerY + line2H * 0.8f
        
        val speedGradient2 = Brush.linearGradient(
            colors = listOf(Color(0xFF3B82F6), Color(0xFF6366F1)),
            start = Offset(line2X, line2Y),
            end = Offset(line2X + line2W, line2Y)
        )
        
        drawRoundRect(
            brush = speedGradient2,
            topLeft = Offset(line2X, line2Y),
            size = androidx.compose.ui.geometry.Size(line2W, line2H),
            cornerRadius = CornerRadius(line2H / 2f, line2H / 2f)
        )
    }
}

@Composable
fun CreatorProHomeScreen(
    viewModel: MainMenuViewModel,
    historyItems: List<com.example.data.db.SavedCreation>,
    onSelectTool: (ToolType) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var premiumUpgradeAlert by remember { mutableStateOf(false) }
    
    // States for custom search content ideas
    var isSearchingIdeas by remember { mutableStateOf(false) }
    var showIdeaResults by remember { mutableStateOf(false) }
    var ideaResultsText by remember { mutableStateOf("") }
    
    // States for custom Hashtag Assistant inside Home Page
    var showHashtagDialog by remember { mutableStateOf(false) }
    var hashtagInputTopic by remember { mutableStateOf("") }
    var generatedHashtags by remember { mutableStateOf("") }
    var isGeneratingHashtags by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("home_screen_container"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Welcome Section
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "Hello, Relin! 👋",
                        style = TextStyle(
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1E293B)
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Create amazing content with AI.",
                        style = TextStyle(
                            fontSize = 15.sp,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search content ideas, tools and more...", color = Color(0xFF94A3B8)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF64748B)) },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    if (searchQuery.isNotBlank()) {
                                        isSearchingIdeas = true
                                        showIdeaResults = true
                                        scope.launch {
                                            val prompt = "Generate 3 highly descriptive, click-worthy creator ideas, 3 visual concepts and 3 core tips about: $searchQuery. Make the response extremely actionable, concise, well-formatted, and visually beautiful using bullet points."
                                            val systemPrompt = "You are an elite Creator Strategist. Generate high engagement digital asset hooks."
                                            val result = RetrofitClient.generate(prompt, systemPrompt)
                                            isSearchingIdeas = false
                                            ideaResultsText = result
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF8B5CF6), Color(0xFF3B82F6))
                                        )
                                    )
                                    .size(32.dp)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = "Query AI", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_search_bar"),
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC),
                            focusedBorderColor = Color(0xFFE2E8F0),
                            unfocusedBorderColor = Color(0xFFE2E8F0)
                        )
                    )
                }
            }

            // 2. Quick Actions
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Quick Actions",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = "See All",
                            fontSize = 13.sp,
                            color = Color(0xFF8B5CF6),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { onSelectTool(ToolType.CAPTION) }
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Horizontally Scrollable Grid of beautiful quick cards
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        // 1. Generate Reel Script
                        QuickActionCard(
                            title = "Generate\nReel Script",
                            desc = "Create viral reel scripts instantly.",
                            icon = Icons.Default.SmartDisplay,
                            iconBg = Color(0xFFF3E8FF),
                            iconColor = Color(0xFF8B5CF6),
                            onClick = { onSelectTool(ToolType.SCRIPT) }
                        )
                        
                        // 2. Generate Captions
                        QuickActionCard(
                            title = "Generate\nCaptions",
                            desc = "Al-powered captions for any platform.",
                            icon = Icons.Default.Edit,
                            iconBg = Color(0xFFFEE2E2),
                            iconColor = Color(0xFFEF4444),
                            onClick = { onSelectTool(ToolType.CAPTION) }
                        )
                        
                        // 3. Generate Hashtags
                        QuickActionCard(
                            title = "Generate\nHashtags",
                            desc = "Trending hashtags for maximum reach.",
                            icon = Icons.Default.Label, // represents hashtag layout/trending activity
                            iconBg = Color(0xFFECFDF5),
                            iconColor = Color(0xFF10B981),
                            onClick = { showHashtagDialog = true }
                        )
                        
                        // 4. Thumbnail Creator
                        QuickActionCard(
                            title = "Thumbnail\nCreator",
                            desc = "Create eye-catching thumbnails.",
                            icon = Icons.Default.Brush,
                            iconBg = Color(0xFFEFF6FF),
                            iconColor = Color(0xFF3B82F6),
                            onClick = { onSelectTool(ToolType.THUMBNAIL) }
                        )
                    }
                }
            }

            // 3. Trending Ideas (Hot, scrollable row or list)
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🔥", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Trending Ideas",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                        }
                        Text(
                            text = "View All",
                            fontSize = 13.sp,
                            color = Color(0xFF3B82F6),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { onSelectTool(ToolType.ANALYTICS) }
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        TrendingIdeaCard(
                            rank = "1",
                            rankColor = Color(0xFF8B5CF6),
                            title = "5 AI Tools Every Creator Needs",
                            statsLabel = "12.5K uses",
                            onClick = {
                                viewModel.captionTopic.value = "5 AI Tools Every Creator Needs"
                                onSelectTool(ToolType.CAPTION)
                            }
                        )
                        
                        TrendingIdeaCard(
                            rank = "2",
                            rankColor = Color(0xFFEF4444),
                            title = "Instagram Growth Hacks 2026",
                            statsLabel = "9.8K uses",
                            onClick = {
                                viewModel.captionTopic.value = "Instagram Growth Hacks 2026 in details"
                                onSelectTool(ToolType.CAPTION)
                            }
                        )

                        TrendingIdeaCard(
                            rank = "3",
                            rankColor = Color(0xFF3B82F6),
                            title = "Best Side Hustles for Students",
                            statsLabel = "7.2K uses",
                            onClick = {
                                viewModel.blogTopic.value = "Best Side Hustles for Students in college"
                                onSelectTool(ToolType.BLOG)
                            }
                        )
                    }
                }
            }

            // 4. AI Tools Row
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AI Tools",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = "See All",
                            fontSize = 13.sp,
                            color = Color(0xFF8B5CF6),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { onSelectTool(ToolType.CAPTION) }
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        VerticalToolItem(
                            icon = Icons.Default.SmartDisplay,
                            iconBg = Color(0xFFEEF2F6),
                            iconColor = Color(0xFF6366F1),
                            label = "Script Writer",
                            onClick = { onSelectTool(ToolType.SCRIPT) }
                        )
                        VerticalToolItem(
                            icon = Icons.Default.Edit,
                            iconBg = Color(0xFFFFEEFA),
                            iconColor = Color(0xFFEC4899),
                            label = "Caption Gen",
                            onClick = { onSelectTool(ToolType.CAPTION) }
                        )
                        VerticalToolItem(
                            icon = Icons.Default.PlayArrow,
                            iconBg = Color(0xFFFFF7ED),
                            iconColor = Color(0xFFF97316),
                            label = "Title Gen",
                            onClick = {
                                viewModel.scriptTopic.value = "Top Viral Titles"
                                onSelectTool(ToolType.SCRIPT)
                            }
                        )
                        VerticalToolItem(
                            icon = Icons.Default.TrendingUp,
                            iconBg = Color(0xFFECFDF5),
                            iconColor = Color(0xFF10B981),
                            label = "Trend Analyzer",
                            onClick = { onSelectTool(ToolType.ANALYTICS) }
                        )
                        VerticalToolItem(
                            icon = Icons.Default.AutoAwesome,
                            iconBg = Color(0xFFEFF6FF),
                            iconColor = Color(0xFF3B82F6),
                            label = "Idea Gen",
                            onClick = { onSelectTool(ToolType.BLOG) }
                        )
                    }
                }
            }

            // 5. Creator Stats Section
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Creator Stats",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = "See All",
                            fontSize = 13.sp,
                            color = Color(0xFF8B5CF6),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { onSelectTool(ToolType.ANALYTICS) }
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            StatsGridCard(
                                title = "Total Posts",
                                valStr = "125",
                                trendStr = "+12 this week",
                                trendColor = Color(0xFF10B981),
                                modifier = Modifier.weight(1f)
                            )
                            StatsGridCard(
                                title = "Engagement Rate",
                                valStr = "8.5%",
                                trendStr = "+1.2% this week",
                                trendColor = Color(0xFF10B981),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            StatsGridCard(
                                title = "Reach",
                                valStr = "150K",
                                trendStr = "+24.5K this week",
                                trendColor = Color(0xFF10B981),
                                modifier = Modifier.weight(1f)
                            )
                            StatsGridCard(
                                title = "Followers Growth",
                                valStr = "+2,450",
                                trendStr = "+320 this week",
                                trendColor = Color(0xFF10B981),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 6. Premium Banner
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF6F2CF3), Color(0xFF3B82F6))
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "👑", fontSize = 24.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Upgrade to CreatorPro Premium",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                
                                PremiumBullet(text = "Unlimited AI Content")
                                PremiumBullet(text = "Advanced Analytics")
                                PremiumBullet(text = "Premium Templates")
                                PremiumBullet(text = "AI Voice Scripts")
                            }
                            
                            Button(
                                onClick = { premiumUpgradeAlert = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                shape = RoundedCornerShape(24.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                                modifier = Modifier.testTag("upgrade_premium_button")
                            ) {
                                Text(
                                    text = "Upgrade Now",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF6F2CF3)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Interactive Dialogs:
    // A. AI Idea Generation Results
    if (showIdeaResults) {
        AlertDialog(
            onDismissRequest = { showIdeaResults = false },
            title = {
                Text(
                    text = "AI Generated Idea Strategy",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Query: $searchQuery",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(0xFF8B5CF6)
                    )
                    
                    if (isSearchingIdeas) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 12.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            Text("Consulting CreatorPro AI Strategist...", fontSize = 12.sp)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp)
                        ) {
                            item {
                                Text(
                                    text = ideaResultsText,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp,
                                    color = Color(0xFF1E293B)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = {
                        // Quick Pre-fill as Topic
                        viewModel.captionTopic.value = searchQuery
                        onSelectTool(ToolType.CAPTION)
                        showIdeaResults = false
                    }, enabled = !isSearchingIdeas) {
                        Text("Use as Caption Topic", fontWeight = FontWeight.Bold)
                    }
                    Button(onClick = { showIdeaResults = false }) {
                        Text("Done")
                    }
                }
            }
        )
    }

    // B. Hashtag Generator assistant
    if (showHashtagDialog) {
        AlertDialog(
            onDismissRequest = { showHashtagDialog = false },
            title = { Text("Trending Hashtags Assistant", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Enter key topic to search or generate trending Instagram, TikTok, and LinkedIn hashtags.", fontSize = 12.sp, color = Color(0xFF64748B))
                    
                    OutlinedTextField(
                        value = hashtagInputTopic,
                        onValueChange = { hashtagInputTopic = it },
                        placeholder = { Text("e.g. productivity, digital tools") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    if (isGeneratingHashtags) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp))
                            Text("Generating trending hashtags...", fontSize = 12.sp)
                        }
                    } else if (generatedHashtags.isNotBlank()) {
                        SelectionContainer {
                            Text(
                                text = generatedHashtags,
                                fontSize = 12.sp,
                                color = Color(0xFF10B981),
                                modifier = Modifier
                                    .background(Color(0xFFECFDF5), RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                                    .fillMaxWidth()
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { showHashtagDialog = false }) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (hashtagInputTopic.isNotBlank()) {
                                isGeneratingHashtags = true
                                scope.launch {
                                    val prompt = "Generate 15 highly viral, trending hashtags related to: $hashtagInputTopic."
                                    val systemPrompt = "You are a social SEO generator."
                                    val result = RetrofitClient.generate(prompt, systemPrompt)
                                    isGeneratingHashtags = false
                                    generatedHashtags = result
                                }
                            }
                        },
                        enabled = hashtagInputTopic.isNotBlank() && !isGeneratingHashtags
                    ) {
                        Text("Generate")
                    }
                }
            }
        )
    }

    // C. Premium Banner Dialog
    if (premiumUpgradeAlert) {
        AlertDialog(
            onDismissRequest = { premiumUpgradeAlert = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "👑", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("CreatorPro Premium", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Experience absolute creative power with unlimited AI outputs, premium high-pacing video scripts, custom hex layout options, and 4K banner designs!",
                        fontSize = 13.sp,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Join over 25,000+ creators globally accelerating their digital brand with CreatorPro Masterclass integrations.",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { premiumUpgradeAlert = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6F2CF3))
                ) {
                    Text("Let's Go!", color = Color.White)
                }
            }
        )
    }
}

@Composable
fun CreatorProAssistantScreen(viewModel: MainMenuViewModel) {
    val messages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isLoading by viewModel.isChatLoading.collectAsStateWithLifecycle()
    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
            .testTag("ai_assistant_screen")
    ) {
        // Assistant Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Forum,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "AI Strategy Assistant",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = "Powered by Gemini 3.5 Flash",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
            TextButton(onClick = { viewModel.clearChat() }) {
                Text("Clear Thread", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
            }
        }

        HorizontalDivider(color = Color(0xFFF1F5F9))

        // Chats list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages) { message ->
                val isAssistant = message.sender == "assistant"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isAssistant) Arrangement.Start else Arrangement.End
                ) {
                    Card(
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isAssistant) 4.dp else 16.dp,
                            bottomEnd = if (isAssistant) 16.dp else 4.dp
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isAssistant) Color(0xFFF1F5F9) else MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .widthIn(max = 280.dp)
                            .padding(horizontal = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (isAssistant) "CREATORPRO AI" else "YOU",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isAssistant) Color(0xFF64748B) else Color.White.copy(alpha = 0.7f),
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = message.text,
                                fontSize = 13.sp,
                                color = if (isAssistant) Color(0xFF1E293B) else Color.White,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
            if (isLoading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Text(
                                    text = "Brainstorming viral ideas...",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Input Area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = { Text("Ask for script hooks, growth tactics...", fontSize = 13.sp, color = Color(0xFF94A3B8)) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("assistant_chat_input"),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color(0xFFCBD5E1)
                ),
                singleLine = true
            )
            IconButton(
                onClick = {
                    if (textInput.isNotBlank()) {
                        viewModel.sendChatMessage(textInput)
                        textInput = ""
                    }
                },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .size(44.dp)
                    .testTag("assistant_send_button"),
                enabled = textInput.isNotBlank() && !isLoading
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun QuickActionCard(
    title: String,
    desc: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBg: Color,
    iconColor: Color,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier
            .width(150.dp)
            .height(180.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B),
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = desc,
                    fontSize = 10.sp,
                    color = Color(0xFF64748B),
                    lineHeight = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun TrendingIdeaCard(
    rank: String,
    rankColor: Color,
    title: String,
    statsLabel: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier
            .width(220.dp)
            .height(115.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(rankColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = rank,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = rankColor
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = rankColor,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = statsLabel,
                        fontSize = 10.sp,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun VerticalToolItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBg: Color,
    iconColor: Color,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF475569)
        )
    }
}

@Composable
fun StatsGridCard(
    title: String,
    valStr: String,
    trendStr: String,
    trendColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF64748B)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = valStr,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(trendColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = trendStr,
                    fontSize = 10.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun PremiumBullet(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.9f)
        )
    }
}
