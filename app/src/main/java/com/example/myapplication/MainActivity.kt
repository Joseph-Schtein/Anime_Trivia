package com.example.myapplication // Keep your package name!

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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

    // NEW: Check Button Translation
    "check_btn" to mapOf(Lang.EN to "CHECK", Lang.HE to "בדיקה", Lang.AR to "يتحقق", Lang.RU to "ПРОВЕРИТЬ")
)

fun getString(key: String, lang: Lang): String {
    return dict[key]?.get(lang) ?: dict[key]?.get(Lang.EN) ?: key
}

val categoryMap = listOf(
    "Anime" to "cat_anime",
    "Pop Culture" to "cat_pop",
    "Terms" to "cat_terms"
)

// --- FIREBASE DATA MODEL ---
data class Question(
    val category: String = "",
    val textMap: Map<String, String> = emptyMap(),
    val optionsMap: Map<String, List<String>> = emptyMap(),
    val correctMap: Map<String, String> = emptyMap()
)

enum class ScreenState { SPLASH, MENU, SUBJECTS, PLAYING, GAME_OVER, ADD_QUESTION }

// --- TRANSLATION HELPER FUNCTION ---
suspend fun translateTextToAllLanguages(originalText: String, sourceLang: Lang): Map<String, String> {
    val translatedMap = mutableMapOf<String, String>()
    translatedMap[sourceLang.code] = originalText

    for (targetLang in Lang.values()) {
        if (targetLang == sourceLang) continue

        val options = TranslatorOptions.Builder()
            .setSourceLanguage(sourceLang.mlKitCode)
            .setTargetLanguage(targetLang.mlKitCode)
            .build()

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

    LaunchedEffect(Unit) {
        db.collection("questions").addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                questionBank = snapshot.documents.mapNotNull { it.toObject(Question::class.java) }
                isLoading = false
            }
        }
    }

    val currentPlayList = questionBank.filter { it.category == activeCategory }

    when (currentScreen) {
        ScreenState.SPLASH -> SplashScreen { currentScreen = ScreenState.MENU }
        ScreenState.MENU -> MenuScreen(currentLang, isLoading, questionBank.size, onLangChange,
            onPlayClick = { currentScreen = ScreenState.SUBJECTS },
            onAddClick = { currentScreen = ScreenState.ADD_QUESTION })
        ScreenState.SUBJECTS -> SubjectsScreen(currentLang,
            onCategorySelected = { categoryInternal ->
                activeCategory = categoryInternal
                if (questionBank.any { it.category == categoryInternal }) currentScreen = ScreenState.PLAYING
                else Toast.makeText(context, getString("empty_cat", currentLang), Toast.LENGTH_SHORT).show()
            },
            onBack = { currentScreen = ScreenState.MENU })
        ScreenState.PLAYING -> GameScreen(currentLang, currentPlayList) { score, total ->
            currentScore = score
            totalQuestionsPlayed = total
            currentScreen = ScreenState.GAME_OVER
        }
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
        Button(
            onClick = onPlayClick, colors = ButtonDefaults.buttonColors(containerColor = RoseCoral),
            shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth(0.7f).height(60.dp), border = BorderStroke(3.dp, DeepIndigo)
        ) { Text(getString("play_btn", lang), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = CustomWhite) }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(
            onClick = onAddClick, shape = RoundedCornerShape(20.dp), border = BorderStroke(3.dp, DeepIndigo),
            modifier = Modifier.fillMaxWidth(0.7f).height(60.dp)
        ) { Text(getString("add_btn", lang), color = DeepIndigo, fontWeight = FontWeight.Bold) }
    }
}

@Composable
fun SubjectsScreen(lang: Lang, onCategorySelected: (String) -> Unit, onBack: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        categoryMap.forEach { (internalName, dictKey) ->
            Button(
                onClick = { onCategorySelected(internalName) }, modifier = Modifier.fillMaxWidth(0.7f).padding(8.dp).height(50.dp),
                shape = RoundedCornerShape(20.dp), border = BorderStroke(3.dp, DeepIndigo), colors = ButtonDefaults.buttonColors(containerColor = CustomWhite)
            ) { Text(getString(dictKey, lang), color = DeepIndigo) }
        }
        Spacer(modifier = Modifier.height(24.dp))
        TextButton(onClick = onBack) { Text(getString("cancel", lang), color = DeepIndigo) }
    }
}

// --- UPDATED GAME SCREEN ---
@Composable
fun GameScreen(lang: Lang, questions: List<Question>, onGameOver: (Int, Int) -> Unit) {
    var currentIndex by remember { mutableStateOf(0) }
    var score by remember { mutableStateOf(0) }

    // NEW: Tracks which option the user has currently tapped
    var selectedOption by remember { mutableStateOf<String?>(null) }

    val q = questions[currentIndex]
    val qText = q.textMap[lang.code] ?: q.textMap["en"] ?: "Translation missing"
    val qOpts = q.optionsMap[lang.code] ?: q.optionsMap["en"] ?: emptyList()
    val qCorrect = q.correctMap[lang.code] ?: q.correctMap["en"] ?: ""

    Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {

        // --- FUTURE IMAGE PLACEHOLDER ---
        // When you have a real picture, replace this Box with:
        // Image(painter = painterResource(id = R.drawable.your_image_name), contentDescription = null)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("Future Character Image Space", color = DeepIndigo.copy(alpha = 0.6f))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Progress Bar
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            LinearProgressIndicator(
                progress = { (currentIndex + 1).toFloat() / questions.size },
                modifier = Modifier.weight(1f).height(12.dp),
                color = VibrantIndigo, trackColor = CustomWhite, strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text("${currentIndex + 1}/${questions.size}", color = DeepIndigo, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text(qText, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = DeepIndigo)
        Spacer(modifier = Modifier.height(32.dp))

        // Options List
        qOpts.forEach { option ->
            val isSelected = (option == selectedOption)

            Button(
                onClick = { selectedOption = option },
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).height(56.dp),
                // Change color if selected!
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) VibrantIndigo else CustomWhite
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(3.dp, DeepIndigo)
            ) {
                Text(
                    text = option,
                    // Make text white if button is Indigo, otherwise keep it Indigo
                    color = if (isSelected) CustomWhite else DeepIndigo,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // NEW: Check Answer Button
        Button(
            onClick = {
                // Only do something if they picked an answer
                if (selectedOption != null) {
                    if (selectedOption == qCorrect) {
                        score++
                    }

                    // Move to next question or end game
                    if (currentIndex < questions.size - 1) {
                        currentIndex++
                        selectedOption = null // Reset selection for the next screen
                    } else {
                        onGameOver(score, questions.size)
                    }
                }
            },
            // Disable the button if nothing is selected yet
            enabled = selectedOption != null,
            modifier = Modifier.fillMaxWidth(0.8f).height(60.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = RoseCoral,
                disabledContainerColor = RoseCoral.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(3.dp, if (selectedOption != null) DeepIndigo else DeepIndigo.copy(alpha = 0.5f))
        ) {
            Text(getString("check_btn", lang), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = CustomWhite)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun GameOverScreen(lang: Lang, score: Int, total: Int, onBackToMenu: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(getString("game_over", lang), fontSize = 36.sp, fontWeight = FontWeight.Bold, color = DeepIndigo)
        Spacer(modifier = Modifier.height(16.dp))
        Text("$score / $total", fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, color = VibrantIndigo)
        Spacer(modifier = Modifier.height(40.dp))
        Button(
            onClick = onBackToMenu, colors = ButtonDefaults.buttonColors(containerColor = RoseCoral),
            shape = RoundedCornerShape(20.dp), border = BorderStroke(3.dp, DeepIndigo), modifier = Modifier.fillMaxWidth(0.6f).height(56.dp)
        ) { Text(getString("continue_btn", lang), color = CustomWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
fun AddQuestionScreen(lang: Lang, onQuestionAdded: (Question) -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var qText by remember { mutableStateOf("") }
    var ans by remember { mutableStateOf("") }
    var opts = remember { mutableStateListOf("", "", "", "") }
    var selectedInternalCat by remember { mutableStateOf("Anime") }
    var isTranslating by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        OutlinedTextField(value = qText, onValueChange = { qText = it }, label = { Text("${getString("q_label", lang)} (${lang.display})") }, modifier = Modifier.fillMaxWidth())
        opts.forEachIndexed { i, opt ->
            OutlinedTextField(value = opt, onValueChange = { opts[i] = it }, label = { Text("${getString("opt_label", lang)} ${i+1}") }, modifier = Modifier.fillMaxWidth())
        }
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
            Button(
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
                                Lang.values().forEach { l ->
                                    translatedOptsMap[l.code]?.add(tOptMap[l.code] ?: opt)
                                }
                            }

                            val newQ = Question(
                                category = selectedInternalCat,
                                textMap = translatedQMap,
                                optionsMap = translatedOptsMap,
                                correctMap = translatedAnsMap
                            )
                            onQuestionAdded(newQ)
                            isTranslating = false
                        }
                    } else {
                        Toast.makeText(context, getString("fill_fields", lang), Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = RoseCoral), shape = RoundedCornerShape(20.dp), border = BorderStroke(3.dp, DeepIndigo), modifier = Modifier.fillMaxWidth().height(56.dp)
            ) { Text(getString("save", lang), color = CustomWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
        }

        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onCancel, enabled = !isTranslating) { Text(getString("cancel", lang), color = DeepIndigo) }
        Spacer(modifier = Modifier.height(48.dp))
    }
}