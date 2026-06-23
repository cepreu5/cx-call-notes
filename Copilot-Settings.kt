1. Къде да сложиш SettingsDialogScreen?
Има два варианта:

A) В същия файл като MainScreen
(ако проектът е малък)

B) В отделен файл
ui/settings/SettingsDialogScreen.kt  
(по‑добрата практика)

Пример:
app/
 └─ ui/
     ├─ main/MainScreen.kt
     └─ settings/SettingsDialogScreen.kt

2. Как да го извикаш от основното приложение
В твоя MainScreen() (или HomeScreen, или AppScreen) добавяш:
var showSettings by remember { mutableStateOf(false) }

И бутонът, който отваря модала:
Button(
    onClick = { showSettings = true }
) {
    Text("Отвори настройки")
}

След това — под бутона, но вътре в същия Composable:
if (showSettings) {
    SettingsDialogScreen(
        onClose = { showSettings = false },

        onSave = { appBg, contactsBg, notesBg, fontColor, formBgColor, themePrimary, themeSecondary, themeTertiary, tags ->
            // Тук си прилагаш настройките към приложението
            showSettings = false
        },

        onBackupClick = {
            // Архивиране (от секция Архивиране)
        },

        onRestoreClick = {
            // Възстановяване (от секция Архивиране)
        },

        onSettingsBackupClick = {
            // Архивиране (от секция Настройки)
        },

        onSettingsRestoreClick = {
            // Възстановяване (от секция Настройки)
        },

        onFabTransparencyChange = { newValue ->
            // Променяш прозрачността на FAB
        },

        onFabHiddenChange = { hidden ->
            // Скриваш/показваш FAB
        },

        onBackupFrequencyChange = { days ->
            // Записваш периодичността
        }
    )
}

🎯 3. Какво става при натискане на Затвори
Вече е свързано:
onClose = { showSettings = false }
Модалът се затваря.

🎯 4. Какво става при натискане на Запази
Ти вече имаш логиката вътре в SettingsDialogScreen:
onSave(
    appBg = colorsState["Основен екран"]!!.background,
    contactsBg = colorsState["Контакти"]!!.background,
    notesBg = colorsState["Бележки"]!!.background,
    fontColor = colorsState[selectedElement]!!.text,
    formBgColor = colorsState["Форма"]!!.background,
    themePrimary = colorsState["Основен екран"]!!.background,
    themeSecondary = colorsState["Контакти"]!!.background,
    themeTertiary = colorsState["Бележки"]!!.background,
    tags = tagsList
)
В твоя MainScreen:
onSave = { appBg, contactsBg, notesBg, fontColor, formBgColor, themePrimary, themeSecondary, themeTertiary, tags ->
    // Тук прилагаш настройките към приложението
    showSettings = false
}


Пълен минимален пример за MainScreen
@Composable
fun MainScreen() {

    var showSettings by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Button(onClick = { showSettings = true }) {
            Text("Отвори настройки")
        }

        if (showSettings) {
            SettingsDialogScreen(
                onClose = { showSettings = false },

                onSave = { appBg, contactsBg, notesBg, fontColor, formBgColor, themePrimary, themeSecondary, themeTertiary, tags ->
                    // прилагаш настройките
                    showSettings = false
                },

                onBackupClick = { /* Архивиране */ },
                onRestoreClick = { /* Възстановяване */ },

                onSettingsBackupClick = { /* Архивиране от Настройки */ },
                onSettingsRestoreClick = { /* Възстановяване от Настройки */ },

                onFabTransparencyChange = { /* FAB transparency */ },
                onFabHiddenChange = { /* FAB hidden */ },
                onBackupFrequencyChange = { /* Backup frequency */ }
            )
        }
    }
}

@Composable
fun SettingsDialogScreen(
    onClose: () -> Unit,
    onSave: (
        appBg: Color,
        contactsBg: Color,
        notesBg: Color,
        fontColor: Color,
        formBgColor: Color,
        themePrimary: Color,
        themeSecondary: Color,
        themeTertiary: Color,
        tags: List<String>
    ) -> Unit,
    onBackupClick: () -> Unit,
    onRestoreClick: () -> Unit,
    onSettingsBackupClick: () -> Unit,
    onSettingsRestoreClick: () -> Unit,
    onFabTransparencyChange: (Int) -> Unit,
    onFabHiddenChange: (Boolean) -> Unit,
    onBackupFrequencyChange: (Int) -> Unit
) {
    // ------------------------------------------------------------
    // 1) ЕЛЕМЕНТИ (Основен екран, Контакти, Бележки, Форма)
    // ------------------------------------------------------------

    val elements = listOf("Основен екран", "Контакти", "Бележки", "Форма")

    data class ElementColors(
        val background: Color,
        val text: Color
    )

    val colorsState = remember {
        mutableStateMapOf(
            "Основен екран" to ElementColors(Color(0xFFF97316), Color.White),
            "Контакти" to ElementColors(Color(0xFFF97316), Color.White),
            "Бележки" to ElementColors(Color(0xFFF97316), Color.White),
            "Форма" to ElementColors(Color(0xFFF97316), Color.White)
        )
    }

    var selectedElement by remember { mutableStateOf(elements[0]) }
    val currentColors = colorsState[selectedElement]!!

    // ------------------------------------------------------------
    // 2) COLOR PICKER (HEX ↔ RGB)
    // ------------------------------------------------------------

    data class ColorPickerState(
        val r: Float = 255f,
        val g: Float = 120f,
        val b: Float = 0f
    ) {
        val color: Color get() = Color(r.toInt(), g.toInt(), b.toInt())
        val hex: String get() = "#%02X%02X%02X".format(r.toInt(), g.toInt(), b.toInt())
    }

    fun hexToRgb(hex: String): Triple<Int, Int, Int>? {
        val clean = hex.removePrefix("#")
        if (clean.length != 6) return null
        return try {
            Triple(
                clean.substring(0, 2).toInt(16),
                clean.substring(2, 4).toInt(16),
                clean.substring(4, 6).toInt(16)
            )
        } catch (_: Exception) {
            null
        }
    }

    var picker by remember { mutableStateOf(ColorPickerState()) }
    var hexInput by remember { mutableStateOf(picker.hex) }

    fun onHexChanged(newHex: String) {
        hexInput = newHex.uppercase()
        val rgb = hexToRgb(hexInput)
        if (rgb != null) {
            picker = picker.copy(
                r = rgb.first.toFloat(),
                g = rgb.second.toFloat(),
                b = rgb.third.toFloat()
            )
        }
    }

    fun applyHexToBackground() {
        colorsState[selectedElement] = currentColors.copy(background = picker.color)
    }

    fun applyHexToText() {
        colorsState[selectedElement] = currentColors.copy(text = picker.color)
    }

    // ------------------------------------------------------------
    // 3) TAGS, FAB, BACKUP FREQUENCY
    // ------------------------------------------------------------

    var tagsInput by remember { mutableStateOf("") }
    var fabTransparency by remember { mutableStateOf(51f) }
    var fabHidden by remember { mutableStateOf(false) }
    var backupFrequency by remember { mutableStateOf(7) }

    // ------------------------------------------------------------
    // 4) UI
    // ------------------------------------------------------------

    Dialog(onDismissRequest = onClose) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
            ) {

                // HEADER
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFFF97316), Color(0xFFFB923C))
                            )
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Настройки",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .background(Color(0xFFFAFAFA))
                        .padding(16.dp)
                ) {

                    // ------------------------------------------------------------
                    // ПРЕГЛЕД
                    // ------------------------------------------------------------
                    PreviewCard(
                        backgroundColor = currentColors.background,
                        textColor = currentColors.text
                    )

                    Spacer(Modifier.height(16.dp))

                    // ------------------------------------------------------------
                    // SELECT: ЦВЕТОВЕ
                    // ------------------------------------------------------------
                    ColorsSelect(
                        selected = selectedElement,
                        options = elements,
                        onSelected = { selectedElement = it }
                    )

                    Spacer(Modifier.height(16.dp))

                    // ------------------------------------------------------------
                    // ПАЛИТРА (Фон + Шрифт + квадратче "+")
                    // ------------------------------------------------------------
                    PaletteCard(
                        selectedBackground = currentColors.background,
                        selectedText = currentColors.text,
                        pickerColor = picker.color,
                        onBackgroundSelected = {
                            colorsState[selectedElement] = currentColors.copy(background = it)
                        },
                        onTextSelected = {
                            colorsState[selectedElement] = currentColors.copy(text = it)
                        },
                        onHexApplyBackground = { applyHexToBackground() },
                        onHexApplyText = { applyHexToText() }
                    )

                    Spacer(Modifier.height(16.dp))

                    // ------------------------------------------------------------
                    // HEX INPUT
                    // ------------------------------------------------------------
                    HexInput(
                        hex = hexInput,
                        onHexChange = { onHexChanged(it) },
                        previewColor = picker.color
                    )

                    Spacer(Modifier.height(16.dp))

                    // ------------------------------------------------------------
                    // RGB SLIDERS
                    // ------------------------------------------------------------
                    RgbSliders(
                        picker = picker,
                        onChange = { newPicker ->
                            picker = newPicker
                            hexInput = newPicker.hex
                        }
                    )

                    Spacer(Modifier.height(16.dp))

                    // ------------------------------------------------------------
                    // TAGS
                    // ------------------------------------------------------------
                    TagsCard(
                        tagsInput = tagsInput,
                        onChange = { tagsInput = it }
                    )

                    Spacer(Modifier.height(16.dp))

                    // ------------------------------------------------------------
                    // FAB (Меню бутон)
                    // ------------------------------------------------------------
                    FabCard(
                        fabHidden = fabHidden,
                        onFabHiddenChange = { fabHidden = it }
                    )

                    Spacer(Modifier.height(16.dp))

                    // ------------------------------------------------------------
                    // FAB Transparency
                    // ------------------------------------------------------------
                    TransparencyCard(
                        value = fabTransparency,
                        onChange = { fabTransparency = it }
                    )

                    Spacer(Modifier.height(16.dp))

                    // ------------------------------------------------------------
                    // АРХИВИРАНЕ
                    // ------------------------------------------------------------
                    ArchiveCard(
                        onBackupClick = onBackupClick,
                        onRestoreClick = onRestoreClick
                    )

                    Spacer(Modifier.height(16.dp))

                    // ------------------------------------------------------------
                    // НАСТРОЙКИ (Архив + Възст. + периодичност)
                    // ------------------------------------------------------------
                    SettingsBackupCard(
                        backupFrequency = backupFrequency,
                        onBackupFrequencyChange = { backupFrequency = it },
                        onBackupClick = onSettingsBackupClick,
                        onRestoreClick = onSettingsRestoreClick
                    )
                }

                // ------------------------------------------------------------
                // FOOTER BUTTONS
                // ------------------------------------------------------------
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFFF97316), Color(0xFFFB923C))
                            )
                        )
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onClose) {
                        Text("Затвори", color = Color.White)
                    }

                    Spacer(Modifier.width(12.dp))

                    Button(
                        onClick = {
                            val tagsList = tagsInput.split(",")
                                .map { it.trim() }
                                .filter { it.isNotEmpty() }
                                .take(20)

                            onFabTransparencyChange(fabTransparency.toInt())
                            onFabHiddenChange(fabHidden)
                            onBackupFrequencyChange(backupFrequency)

                            onSave(
                                appBg = colorsState["Основен екран"]!!.background,
                                contactsBg = colorsState["Контакти"]!!.background,
                                notesBg = colorsState["Бележки"]!!.background,
                                fontColor = colorsState[selectedElement]!!.text,
                                formBgColor = colorsState["Форма"]!!.background,
                                themePrimary = colorsState["Основен екран"]!!.background,
                                themeSecondary = colorsState["Контакти"]!!.background,
                                themeTertiary = colorsState["Бележки"]!!.background,
                                tags = tagsList
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                    ) {
                        Text("Запази", color = Color(0xFFF97316), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
