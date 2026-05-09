package com.example.myapplication // Keep your package name!

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler // NEW: Added for back navigation
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions

// --- CONTEMPORARY INDIGO COLOR PALETTE ---
val MatchaGreen = Color(0xFFCDDDB4)
val DeepIndigo = Color(0xFF1E293B)
val RoseCoral = Color(0xFFFB7185)
val VibrantIndigo = Color(0xFF6366F1)
val CustomWhite = Color(0xFFFFFFFF)

// --- LANGUAGE & DICTIONARY SYSTEM ---
enum class Lang(val code: String, val mlKitCode: String, val display: String, val isRtl: Boolean) {
    EN("en", TranslateLanguage.ENGLISH, "English", false),
    HE("he", TranslateLanguage.HEBREW, "עברית", true),
    AR("ar", TranslateLanguage.ARABIC, "العربية", true),
    RU("ru", TranslateLanguage.RUSSIAN, "Русский", false)
}

val dict = mapOf(
    "menu_title" to mapOf(Lang.EN to "Main Menu", Lang.HE to "תפריט ראשי", Lang.AR to "القائمة الرئيسية", Lang.RU to "Главное меню"),
    "play_btn" to mapOf(Lang.EN to "PLAY", Lang.HE to "שחק", Lang.AR to "يلعب", Lang.RU to "ИГРАТЬ"),
    "add_btn" to mapOf(Lang.EN to "ADD QUESTION", Lang.HE to "הוסף שאלה", Lang.AR to "أضف سؤالا", Lang.RU to "ДОБАВИТЬ ВОПРОС"),
    "loading" to mapOf(Lang.EN to "Loading...", Lang.HE to "טוען...", Lang.AR to "تحميل...", Lang.RU to "Загрузка..."),
    "cancel" to mapOf(Lang.EN to "Cancel", Lang.HE to "ביטול", Lang.AR to "إلغاء", Lang.RU to "Отмена"),
    "save" to mapOf(Lang.EN to "SAVE TO CLOUD", Lang.HE to "שמור לענן", Lang.AR to "حفظ في السحابة", Lang.RU to "СОХРАНИТЬ В ОБЛАКО"),
    "translating" to mapOf(Lang.EN to "Translating & Saving...", Lang.HE to "מתרגם ושומר...", Lang.AR to "جاري الترجمة والحفظ...", Lang.RU to "Перевод и сохранение..."),
    "game_over" to mapOf(Lang.EN to "Game Over!", Lang.HE to "המשחק נגמר!", Lang.AR to "انتهت اللعبة!", Lang.RU to "Игра окончена!"),
    "continue_btn" to mapOf(Lang.EN to "CONTINUE", Lang.HE to "המשך", Lang.AR to "متابعة", Lang.RU to "ПРОДОЛЖИТЬ"),
    "q_label" to mapOf(Lang.EN to "Question", Lang.HE to "שאלה", Lang.AR to "سؤال", Lang.RU to "Вопрос"),
    "opt_label" to mapOf(Lang.EN to "Option", Lang.HE to "אפשרות", Lang.AR to "خيار", Lang.RU to "Вариант"),
    "correct_ans" to mapOf(Lang.EN to "Correct Answer", Lang.HE to "תשובה נכונה", Lang.AR to "الإجابة الصحيحة", Lang.RU to "Правильный ответ"),
    "cat_anime" to mapOf(Lang.EN to "Anime", Lang.HE to "אנימה", Lang.AR to "أنيمي", Lang.RU to "Аниме"),
    "cat_pop" to mapOf(Lang.EN to "Pop Culture", Lang.HE to "תרבות פופ", Lang.AR to "ثقافة البوب", Lang.RU to "Поп-культура"),
    "cat_terms" to mapOf(Lang.EN to "Terms", Lang.HE to "מונחים", Lang.AR to "مصطلحات", Lang.RU to "Термины"),
    "empty_cat" to mapOf(Lang.EN to "Empty category", Lang.HE to "קטגוריה ריקה", Lang.AR to "فئة فارغة", Lang.RU to "Пустая категория"),
    "fill_fields" to mapOf(Lang.EN to "Fill all fields", Lang.HE to "מלא את כל השדות", Lang.AR to "املأ جميع الحقول", Lang.RU to "Заполните все поля"),
    "check_btn" to mapOf(Lang.EN to "CHECK", Lang.HE to "בדיקה", Lang.AR to "يتحقق", Lang.RU to "ПРОВЕРИТЬ"),
    // NEW: Back Translation
    "back" to mapOf(Lang.EN to "Back", Lang.HE to "חזור", Lang.AR to "رجوع", Lang.RU to "Назад")
)

fun getString(key: String, lang: Lang): String {
    return dict[key]?.get(lang) ?: dict[key]?.get(Lang.EN) ?: key
}

val categoryMap = listOf(
    "Anime" to "cat_anime",
    "Pop Culture" to "cat_pop",
    "Terms" to "cat_terms"
)

// --- CUSTOM 3D BUTTON (NEO-BRUTALISM STYLE) ---
@Composable
fun NeoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = CustomWhite,
    textColor: Color = DeepIndigo,
    enabled: Boolean = true,
    fontSize: androidx.compose.ui.unit.TextUnit = 16.sp,
    isToggled: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val offset by animateDpAsState(targetValue = if (isPressed || isToggled || !enabled) 5.dp else 0.dp, label = "pressAnim")

    Box(
        modifier = modifier.then(
            if (enabled) Modifier.clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            else Modifier
        )
    ) {
        Box(modifier = Modifier.matchParentSize().offset(x = 5.dp, y = 5.dp).background(if (enabled) DeepIndigo else DeepIndigo.copy(alpha = 0.3f), RoundedCornerShape(16.dp)))
        Box(
            modifier = Modifier.fillMaxWidth().offset(x = offset, y = offset).background(if (enabled) backgroundColor else backgroundColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp)).border(3.dp, if (enabled) DeepIndigo else DeepIndigo.copy(alpha = 0.3f), RoundedCornerShape(16.dp)).padding(horizontal = 16.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = text, color = if (enabled) textColor else textColor.copy(alpha = 0.6f), fontSize = fontSize, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}

// --- FIREBASE DATA MODEL ---
data class Question(
    val category: String = "",
    val textMap: Map<String, String> = emptyMap(),
    val optionsMap: Map<String, List<String>> = emptyMap(),
    val correctMap: Map<String, String> = emptyMap()
)

enum class ScreenState { SPLASH, MENU, SUBJECTS, PLAYING, GAME_OVER, ADD_QUESTION }

suspend fun translateTextToAllLanguages(originalText: String, sourceLang: Lang): Map<String, String> {
    val translatedMap = mutableMapOf<String, String>()
    translatedMap[sourceLang.code] = originalText

    for (targetLang in Lang.values()) {
        if (targetLang == sourceLang) continue
        val options = TranslatorOptions.Builder().setSourceLanguage(sourceLang.mlKitCode).setTargetLanguage(targetLang.mlKitCode).build()
        val translator = Translation.getClient(options)
        val conditions = DownloadConditions.Builder().build()
        try {
            translator.downloadModelIfNeeded(conditions).await()
            val translatedText = translator.translate(originalText).await()
            translatedMap[targetLang.code] = translatedText
        } catch (e: Exception) {
            translatedMap[targetLang.code] = originalText
        } finally {
            translator.close()
        }
    }
    return translatedMap
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var currentLang by remember { mutableStateOf(Lang.EN) }
            val direction = if (currentLang.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

            MaterialTheme(colorScheme = lightColorScheme(background = MatchaGreen, primary = DeepIndigo)) {
                CompositionLocalProvider(LocalLayoutDirection provides direction) {
                    Surface(modifier = Modifier.fillMaxSize(), color = MatchaGreen) {
                        TriviaApp(currentLang) { newLang -> currentLang = newLang }
                    }
                }
            }
        }
    }
}

@Composable
fun TriviaApp(currentLang: Lang, onLangChange: (Lang) -> Unit) {
    val context = LocalContext.current
    val db = Firebase.firestore
    var currentScreen by remember { mutableStateOf(ScreenState.SPLASH) }
    var activeCategory by remember { mutableStateOf("") }
    var currentScore by remember { mutableStateOf(0) }
    var totalQuestionsPlayed by remember { mutableStateOf(0) }
    var questionBank by remember { mutableStateOf(listOf<Question>()) }
    var isLoading by remember { mutableStateOf(true) }
    var currentPlayList by remember { mutableStateOf(listOf<Question>()) }

    LaunchedEffect(Unit) {
        db.collection("questions").addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                questionBank = snapshot.documents.mapNotNull { it.toObject(Question::class.java) }
                isLoading = false
            }
        }
    }

    when (currentScreen) {
        ScreenState.SPLASH -> SplashScreen { currentScreen = ScreenState.MENU }
        ScreenState.MENU -> MenuScreen(currentLang, isLoading, questionBank.size, onLangChange,
            onPlayClick = { currentScreen = ScreenState.SUBJECTS },
            onAddClick = { currentScreen = ScreenState.ADD_QUESTION })
        ScreenState.SUBJECTS -> SubjectsScreen(currentLang,
            onCategorySelected = { categoryInternal ->
                val availableQuestions = questionBank.filter { it.category == categoryInternal }
                if (availableQuestions.isNotEmpty()) {
                    activeCategory = categoryInternal
                    currentPlayList = availableQuestions.shuffled().take(10)
                    currentScreen = ScreenState.PLAYING
                } else {
                    Toast.makeText(context, getString("empty_cat", currentLang), Toast.LENGTH_SHORT).show()
                }
            },
            onBack = { currentScreen = ScreenState.MENU })
        // NEW: Passed onExit action to the GameScreen
        ScreenState.PLAYING -> GameScreen(currentLang, currentPlayList,
            onGameOver = { score, total ->
                currentScore = score
                totalQuestionsPlayed = total
                currentScreen = ScreenState.GAME_OVER
            },
            onExit = { currentScreen = ScreenState.MENU }
        )
        ScreenState.GAME_OVER -> GameOverScreen(currentLang, currentScore, totalQuestionsPlayed) { currentScreen = ScreenState.MENU }
        ScreenState.ADD_QUESTION -> AddQuestionScreen(currentLang,
            onQuestionAdded = { newQ ->
                db.collection("questions").add(newQ).addOnSuccessListener { currentScreen = ScreenState.MENU }
            },
            onCancel = { currentScreen = ScreenState.MENU })
    }
}

// --- UI SCREENS ---

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    LaunchedEffect(Unit) { delay(2000); onTimeout() }
    Box(contentAlignment = Alignment.Center) {
        Text("Otaku Trivia", fontSize = 42.sp, fontWeight = FontWeight.Bold, color = DeepIndigo)
    }
}

@Composable
fun MenuScreen(lang: Lang, isLoading: Boolean, qCount: Int, onLangChange: (Lang) -> Unit, onPlayClick: () -> Unit, onAddClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Row(modifier = Modifier.padding(bottom = 32.dp)) {
            Lang.values().forEach { l ->
                TextButton(onClick = { onLangChange(l) }) {
                    Text(l.display, color = if (lang == l) VibrantIndigo else DeepIndigo, fontWeight = if (lang == l) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
        Text(getString("menu_title", lang), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = DeepIndigo)
        if (isLoading) Text(getString("loading", lang), color = DeepIndigo)
        Spacer(modifier = Modifier.height(40.dp))

        NeoButton(text = getString("play_btn", lang), onClick = onPlayClick, modifier = Modifier.fillMaxWidth(0.7f), backgroundColor = RoseCoral, textColor = CustomWhite, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(24.dp))
        NeoButton(text = getString("add_btn", lang), onClick = onAddClick, modifier = Modifier.fillMaxWidth(0.7f), backgroundColor = CustomWhite, textColor = DeepIndigo, fontSize = 16.sp)
    }
}

@Composable
fun SubjectsScreen(lang: Lang, onCategorySelected: (String) -> Unit, onBack: () -> Unit) {
    // NEW: Intercepts the Android System Back Swipe/Button
    BackHandler { onBack() }

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        categoryMap.forEach { (internalName, dictKey) ->
            NeoButton(text = getString(dictKey, lang), onClick = { onCategorySelected(internalName) }, modifier = Modifier.fillMaxWidth(0.7f).padding(8.dp), fontSize = 18.sp)
        }
        Spacer(modifier = Modifier.height(24.dp))
        TextButton(onClick = onBack) { Text(getString("cancel", lang), color = DeepIndigo) }
    }
}

@Composable
fun GameScreen(lang: Lang, questions: List<Question>, onGameOver: (Int, Int) -> Unit, onExit: () -> Unit) {
    // NEW: Intercepts the Android System Back Swipe/Button
    BackHandler { onExit() }

    var currentIndex by remember { mutableStateOf(0) }
    var score by remember { mutableStateOf(0) }
    var selectedOption by remember { mutableStateOf<String?>(null) }

    val q = questions[currentIndex]
    val qText = q.textMap[lang.code] ?: q.textMap["en"] ?: "Translation missing"
    val qCorrect = q.correctMap[lang.code] ?: q.correctMap["en"] ?: ""
    val catDictKey = categoryMap.find { it.first == q.category }?.second ?: "cat_anime"
    val catDisplay = getString(catDictKey, lang)

    val qOpts = remember(currentIndex, lang) {
        val baseOpts = q.optionsMap[lang.code] ?: q.optionsMap["en"] ?: emptyList()
        baseOpts.shuffled()
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        // --- 1. THE SCROLLABLE ZONE ---
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {

            // NEW: Visible Back Button at the top of the screen
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                Text(
                    text = getString("back", lang),
                    color = DeepIndigo.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.clickable { onExit() }.padding(bottom = 16.dp)
                )
            }

            Box(modifier = Modifier.fillMaxWidth().height(180.dp).background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                Text("Future Character Image Space", color = DeepIndigo.copy(alpha = 0.6f))
            }

            Spacer(modifier = Modifier.height(24.dp))

            val progressFraction = currentIndex.toFloat() / questions.size
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f).height(14.dp).background(CustomWhite, RoundedCornerShape(50)).border(2.dp, DeepIndigo, RoundedCornerShape(50))) {
                    if (progressFraction > 0f) {
                        Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(progressFraction).background(VibrantIndigo, RoundedCornerShape(50)).border(2.dp, DeepIndigo, RoundedCornerShape(50)))
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text("${currentIndex + 1}/${questions.size}", color = DeepIndigo, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text(catDisplay, color = RoseCoral, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = qText, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = DeepIndigo, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(32.dp))

            qOpts.forEach { option ->
                val isSelected = (option == selectedOption)
                NeoButton(
                    text = option, onClick = { selectedOption = option }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    backgroundColor = if (isSelected) VibrantIndigo else CustomWhite, textColor = if (isSelected) CustomWhite else DeepIndigo,
                    fontSize = 15.sp, isToggled = isSelected
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // --- 2. THE ANCHORED ZONE ---
        Box(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), contentAlignment = Alignment.Center) {
            NeoButton(
                text = getString("check_btn", lang),
                onClick = {
                    if (selectedOption != null) {
                        if (selectedOption == qCorrect) score++
                        if (currentIndex < questions.size - 1) {
                            currentIndex++
                            selectedOption = null
                        } else {
                            onGameOver(score, questions.size)
                        }
                    }
                },
                enabled = selectedOption != null,
                modifier = Modifier.fillMaxWidth(0.9f), backgroundColor = RoseCoral, textColor = CustomWhite, fontSize = 18.sp
            )
        }
    }
}

@Composable
fun GameOverScreen(lang: Lang, score: Int, total: Int, onBackToMenu: () -> Unit) {
    // NEW: Intercepts the Android System Back Swipe/Button
    BackHandler { onBackToMenu() }

    // NEW: RTL specific formatting so it correctly shows Total / Score for Hebrew & Arabic
    val scoreText = if (lang.isRtl) "$total / $score" else "$score / $total"

    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(getString("game_over", lang), fontSize = 36.sp, fontWeight = FontWeight.Bold, color = DeepIndigo)
        Spacer(modifier = Modifier.height(16.dp))

        Text(scoreText, fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, color = VibrantIndigo)

        Spacer(modifier = Modifier.height(40.dp))
        NeoButton(text = getString("continue_btn", lang), onClick = onBackToMenu, modifier = Modifier.fillMaxWidth(0.6f), backgroundColor = RoseCoral, textColor = CustomWhite, fontSize = 18.sp)
    }
}

@Composable
fun AddQuestionScreen(lang: Lang, onQuestionAdded: (Question) -> Unit, onCancel: () -> Unit) {
    // NEW: Intercepts the Android System Back Swipe/Button
    BackHandler { onCancel() }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var qText by remember { mutableStateOf("") }
    var ans by remember { mutableStateOf("") }
    var opts = remember { mutableStateListOf("", "", "", "") }
    var selectedInternalCat by remember { mutableStateOf("Anime") }
    var isTranslating by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(48.dp))
        OutlinedTextField(value = qText, onValueChange = { qText = it }, label = { Text("${getString("q_label", lang)} (${lang.display})") }, modifier = Modifier.fillMaxWidth())
        opts.forEachIndexed { i, opt -> OutlinedTextField(value = opt, onValueChange = { opts[i] = it }, label = { Text("${getString("opt_label", lang)} ${i+1}") }, modifier = Modifier.fillMaxWidth()) }
        OutlinedTextField(value = ans, onValueChange = { ans = it }, label = { Text(getString("correct_ans", lang)) }, modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            categoryMap.forEach { (internalName, dictKey) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = selectedInternalCat == internalName, onClick = { selectedInternalCat = internalName })
                    Text(getString(dictKey, lang), modifier = Modifier.padding(end = 12.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))

        if (isTranslating) {
            CircularProgressIndicator(color = VibrantIndigo)
            Spacer(modifier = Modifier.height(8.dp))
            Text(getString("translating", lang), color = DeepIndigo, fontWeight = FontWeight.Bold)
        } else {
            NeoButton(
                text = getString("save", lang),
                onClick = {
                    if (qText.isNotBlank() && ans.isNotBlank() && opts.all { it.isNotBlank() }) {
                        isTranslating = true
                        coroutineScope.launch {
                            val translatedQMap = translateTextToAllLanguages(qText, lang)
                            val translatedAnsMap = translateTextToAllLanguages(ans, lang)
                            val translatedOptsMap = mutableMapOf<String, MutableList<String>>()
                            Lang.values().forEach { translatedOptsMap[it.code] = mutableListOf() }
                            for (opt in opts) {
                                val tOptMap = translateTextToAllLanguages(opt, lang)
                                Lang.values().forEach { l -> translatedOptsMap[l.code]?.add(tOptMap[l.code] ?: opt) }
                            }
                            onQuestionAdded(Question(selectedInternalCat, translatedQMap, translatedOptsMap, translatedAnsMap))
                            isTranslating = false
                        }
                    } else Toast.makeText(context, getString("fill_fields", lang), Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(), backgroundColor = RoseCoral, textColor = CustomWhite, fontSize = 18.sp
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onCancel, enabled = !isTranslating) { Text(getString("cancel", lang), color = DeepIndigo) }
        Spacer(modifier = Modifier.height(48.dp))
    }
}