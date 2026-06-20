*команда за проверка на грешки:
.\gradlew.bat compileDebugKotlin --stacktrace --info*

*Разширението xyz.local-history пази всички версии тук:
C:\Users\Cepreu\AppData\Local\xyz.local-history
%APPDATA%\Antigravity\User\History*

Резюме:
Цветове и тема:
- ColorConstants.kt — оптимизиран, премахнати ненужни стойности, добавен contrastOn() за динамични on-цветове
- Всички бутони унифицирани (darkGray фон, бял текст)
- Primary за елементи извън панелите, Secondary за Контакти, Tertiary за Бележки
- Настройки = фон/шрифт на формата
UI промени:
- Икона в хедъра = launcher icon
- Икона за Бележки = Note (листче) вместо Edit (молив)
- FAB: 45dp, Quick Ball стил, рамка вместо сянка
- FAB иконки 22dp
- TagChip цветове от ColorConstants
- Settings: квадратчета показват реални default цветове, color picker показва текущия цвят
Функционалност:
- Long press → потвърждение за обаждане (с име и номер)
- Call screening role request с try-catch защити
- fallbackToDestructiveMigration() върнат за DB
- Reset цветове → commit() вместо apply()
- Изчистване на theme цветове при fresh install

Задължителни настройки на Xiaomi:
1. Autostart
   - Settings → Apps → Manage apps → CX Call Notes → Autostart → Enable
2. Display pop-up window
   - Settings → Apps → CX Call Notes → Permissions → "Display pop-up window while running in background" → Allow
3. Battery optimization
   - Settings → Battery → CX Call Notes → No restrictions
4. Lock screen notifications
   - Settings → Notifications → Lock screen → Show notifications
5. Overlay permission (вече се иска при първо стартиране)
   - Settings → Apps → CX Call Notes → Display over other apps → Allow
Как да стигнеш до тези настройки:
- Settings → Apps → Manage apps → търси "CX Call Notes" → Permissions / Battery / Autostart

Да, може пак да се появи пренасочване на обажданията на TCL, когато потребителят приеме ролята ROLE_CALL_SCREENING. Но без нея IncomingCallScreeningService не се задейства за изходни обаждания и формата изобщо не се показва. Ако проблемът се появи отново, потребителят може да откаже ролята — приложението ще работи, просто без форма след изходни обаждания от тел.
ROLE_CALL_SCREENING е ролята за apps които скринират входни обаждания. Диалогът обикновено казва нещо от рода:
> "Да се разреши на CX Call Notes да скринира входни обаждания?"
> Това ще позволи на приложението да получава входни обаждания и да предприема действия като блокиране на спам.

Какво беше направено по задачите:

Goal
Building and iterating on an Android app called CX Call Notes (cx-calls) — a local call screening app that:
- Checks incoming calls against a local Room database
- Shows caller info overlay for known contacts during ringing
- Shows a post-call note form for unknown contacts
- Fully offline, no external services
Instructions
- User communicates in Bulgarian, prefers Bulgarian responses
- Compile check after each change: .\gradlew.bat compileDebugKotlin
- App is tested on Xiaomi (MIUI) device
- User wants clean, minimal UI with customization options
- # prefix on contact name = exclude from call processing (no overlay, no post-call form)
- Contacts with # still show in Contacts list but not in Notes list
- call_sessions table has been removed — schema simplified to just contacts and call_notes
- fallbackToDestructiveMigration() removed — starting fresh
- FAB menu icons: Контакти, Бележки, Добави бележка, Настройки (order changed)
- Post-call note form is now a Compose Dialog (not separate Activity), shows with dimmed background like Settings
Discoveries
- Xiaomi/MIUI: Standard overlay approach works but requires enabling: Autostart, "Display pop-up window while running in background", Battery → No restrictions
- Notification/Foreground service approach failed on Xiaomi — full-screen intent and foreground service couldn't show over lock screen. Reverted to SYSTEM_ALERT_WINDOW overlay approach
- PostCallNoteActivity had android.telecom.action.POST_CALL intent-filter in manifest — system was launching it automatically after every call, overriding app logic. Removed it.
- IncomingCallScreeningService doesn't fire on user's Xiaomi device — only PhoneStateReceiver handles calls
- FAB positioning required moving from Scaffold's floatingActionButton slot to a separate BoxWithConstraints overlay in content area, because Scaffold constrains the coordinate space
- remember keys on fabXState/fabYState caused position reset on recomposition — removed keys
- Dialog's onDismiss with moveTaskToBack(true) requires LaunchedEffect pattern since it can't be called directly from composable lambda context
Accomplished
Completed:
- Overlay on lock screen: SYSTEM_ALERT_WINDOW with FLAG_SHOW_WHEN_LOCKED + FLAG_TURN_SCREEN_ON — works on Xiaomi after enabling permissions
- Draggable overlay: Saves/restores position via SharedPreferences, always uses Gravity.TOP or Gravity.START
- Close button overlay: Circular red X button (ImageButton) with ripple background
- Call sessions removed: call_sessions table, callSessionId, all session-related code deleted. DB version 3.
- # prefix exclusion: Contacts starting with # skip overlay, skip post-call form, hidden from Notes list, visible in Contacts
- FAB redesign: Default position = middle of right edge. Semicircle menu (6 positions, 4 icons, 45° spacing) on edges. 1/4 arc in corners and center. LocalView for full screen dimensions.
- FAB transparency: Slider 0-100% in Settings, affects FAB background + X icon, NOT menu icons
- FAB hide checkbox: In Settings, completely hides FAB + menu
- Name wrapping: splitNameForFirstLine() — first line constrained for phone number, remaining lines full width. Applied to both Contacts and Notes cards.
- Post-call form as Dialog: PostCallNoteScreen shown as Compose Dialog in MainActivity, not separate Activity. PostCallNoteViewModel instantiated in MainActivity.
- "Нов contato" title: Shows "📝 Нов контакт" when caller is unknown
- Cancel minimizes app: Uses shouldMinimize state + LaunchedEffect + moveTaskToBack(true)
- Clear button on form: X button next to title clears all fields
- Phone editable in edit mode: Removed Card/Text, always OutlinedTextField
- DB permissions removed: POST_NOTIFICATIONS, USE_FULL_SCREEN_INTENT, FOREGROUND_SERVICE, FOREGROUND_SERVICE_PHONE_CALL
Still needs work:
- White flash before dialog appears (standard Android Dialog animation)
- Could be eliminated by switching from Dialog to custom overlay (more complex)
Relevant files / directories
Core app files:
- app/src/main/java/com/example/callnotes/MainActivity.kt — Main UI, FAB, SettingsDialog, PostCallNoteScreen, ContactsList, NotesList
- app/src/main/java/com/example/callnotes/ui/PostCallNoteActivity.kt — PostCallNoteScreen composable (still used as UI component)
- app/src/main/java/com/example/callnotes/ui/PostCallNoteViewModel.kt — Note/contact editing logic, isNewContact flag
- app/src/main/java/com/example/callnotes/ui/MainViewModel.kt — Main screen state, settings load/save, fab position/transparency/hidden
- app/src/main/java/com/example/callnotes/theme/Theme.kt
Data layer:
- app/src/main/java/com/example/callnotes/data/ContactEntity.kt
- app/src/main/java/com/example/callnotes/data/CallNoteEntity.kt (no more callSessionId)
- app/src/main/java/com/example/callnotes/data/ContactDao.kt
- app/src/main/java/com/example/callnotes/data/CallNoteDao.kt
- app/src/main/java/com/example/callnotes/data/AppDatabase.kt (version 3, 2 entities only)
- app/src/main/java/com/example/callnotes/data/CallNotesRepository.kt
- app/src/main/java/com/example/callnotes/data/DatabaseProvider.kt (no fallbackToDestructiveMigration)
- app/src/main/java/com/example/callnotes/data/PhoneNumberNormalizer.kt
Services:
- app/src/main/java/com/example/callnotes/service/PhoneStateReceiver.kt — Main call handler on Xiaomi, # exclusion logic
- app/src/main/java/com/example/callnotes/service/IncomingCallScreeningService.kt — Not active on user's device
- app/src/main/java/com/example/callnotes/service/OverlayService.kt — Draggable overlay with saved position
- app/src/main/java/com/example/callnotes/service/CallStateWatcher.kt
- app/src/main/java/com/example/callnotes/service/CallUiEvents.kt (no more sessionId)
Resources:
- app/src/main/res/layout/overlay_note.xml — Overlay layout with circular close button
- app/src/main/res/drawable/close_button_bg.xml, overlay_background.xml, ic_close.xml
- app/src/main/AndroidManifest.xml — PostCallNoteActivity no longer has POST_CALL intent-filter

---

Следващите описания отразяват ранни и по-късни етапи от разработката, но за текущото състояние на приложението следват да се вземат предвид САМО последната версия на кода!!! 

❖ Настройки на цветове (fontColor и formBgColor): Добавени са в MainViewModel (и MainUiState) с възможност за избор и запазване. SettingsDialog вече съдържа контроли за двата нови параметъра, а формата "Нова бележка" (PostCallNoteActivity) ги чете и оцветява текстовете и фона си спрямо тях.
❖ Сенки на картите: Добавено е elevation = CardDefaults.cardElevation(defaultElevation = 4.dp) на картите на бележките и контактите за по-привлекателен триизмерен вид.
❖ Етикети при обаждане: PhoneStateReceiver вече извлича записаните в базата данни етикети за контакта и ги изпраща към OverlayService, който ги визуализира в нов tv_caller_tags компонент над бутона за затваряне.
❖ Хедър фон: Цветът на фона на приложението вече се прилага и върху TopAppBar (хедъра) за единен дизайн.
❖ Икона в хедъра: Старата икона е заменена с оранжев кръг с бял телефон в него – точно копие на логото/иконата на самото приложение.
❖ FAB бутон Х: Добавен е червен плаващ бутон "X". При единично цъкване той минимизира приложението (moveTaskToBack(true)). При дълго натискане (long press) около него се появява меню за бърз достъп с 4 икони: Добавяне, Контакти, Бележки и Настройки.
❖ История на последното обаждане: При приключване на разговор PhoneStateReceiver записва телефонния номер и името в SharedPreferences под ключовете "last_call_phone" и "last_call_name". При отваряне на "Нова бележка" (през бутона "+"), ако телефонът е празен, автоматично се зареждат тези запазени данни.
❖ Увеличаване на броя етикети: Лимитът за брой етикети в Настройки е повдигнат от 10 на 20.
❖ Интерактивен Color Picker: Всички числови стойности на RGB слайдерите и HEX стойността вече са текстови полета (OutlinedTextField), които могат да се редактират ръчно.
❖ Съкращаване на бележката в Контакти: Текстът на последната бележка в списъка с контакти вече се ограничава точно до 1 ред с многоточие накрая (maxLines = 1, Ellipsis).
❖ Бутон "Обнови" и "Добави": При редактиране на контакт бутонът "Запази" е преименуван на "Добави", и е добавен нов бутон "Обнови" (викащ updateNote()), който само актуализира информацията в контакта без да генерира нова бележка в хронологията.

❖ Разширяване на диалозите до 95%: В PostCallNoteActivity вече се използва usePlatformDefaultWidth = false с fillMaxWidth(0.95f), което разширява формата "Нова бележка / Редактиране" до 95% от ширината на екрана.
❖ Квадрат за избрания цвят в Настройки: В ColorSelectorRow първият елемент е квадратен преглед (RoundedCornerShape(4.dp)) на текущо избрания цвят. Всички останали опции са кръгли (CircleShape). Избраният в момента цвят се показва в реално време в квадратчето.
❖ FAB бутон "X":
❖ Primary, Secondary, Tertiary настройки за цвят: В настройките вече има три допълнителни селектора за Primary, Secondary и Tertiary цветове на темата. Промените по тях се записват и се зареждат динамично от CallNotesTheme, което директно преобразява интерфейса на цялото приложение.

MainActivity.kt
val presets = listOf(
        "default" to Color.LightGray,
        "#FFF9C4" to Color(0xFFFFF9C4),
        "#E3F2FD" to Color(0xFFE3F2FD),
        "#E8F5E9" to Color(0xFFE8F5E9),
        "#F5F5F5" to Color(0xFFF5F5F5),
        "#E0F2F1" to Color(0xFFE0F2F1)
)

surfaceContainerLow = Color(0xFFF3EDF7) — светло лилав фон за контакт панели
secondaryContainer = Color(0xFFE8DEF8) — леко по-наситен лилав за бележки панели
background = Color(0xFFFFFBFE) — почти бял общ фон        

543 - color = Color(0xFF333333),                                                                                                      │
543 + color = MaterialTheme.colorScheme.secondary

### Задание
искам да създадем Андроид приложение със следните възможности:
- при получаване на позвъняване проверява номера в локална база данни и ако намери запис го показва на екрана за информация, без да ангажира самото обаждане.
- ако не съществува запис, след приключване на разговора извежда малка форма за бележка, която се записва в локалната база данни, включваща телефонния номер, името на повикващия и кратък текст
### Допълнителни изисквания
- приложението трябва да работи на заден план и да показва информация при входящо обаждане
- приложението трябва да има лесен за използване минималистичен интерфейс

проектирай като чисто offline Android app, където входящото обаждане се проверява локално, а след края на разговора се показва кратка форма само ако номерът не е познат. CallScreeningService е API за screen-ване на входящи повиквания, TelecomManager дава достъп до call state, а Room е за локалната база.

### Архитектура
Call screening layer
CallScreeningService прихваща входящото повикване и проверява номера в локалната база. Ако номерът съществува, приложението може да покаже собствен overlay/notification с данните, без да блокира обаждането; ако не съществува, обаждането се допуска нормално. CallScreeningService може да allow/silence/disallow повикването според логиката ти.

Call state layer
TelecomManager.isInCall() е удобен за агрегирана проверка дали телефонът е в разговор, а за по-фина реакция можеш да ползваш TelephonyCallback.CallStateListener. Това е полезно за показване на post-call форма веднага след приключване на разговора.

Persistence layer
Room съхранява контакти, call log и бележки напълно локално. Room entities директно описват таблиците и могат да имат индекси за бърз lookup по телефонен номер.

### Room схема
три основни таблици.

### contacts
За познатите номера и показваните данни.
id: Long
phoneNumber: String уникален индекс
displayName: String
note: String
createdAt: Long
updatedAt: Long

### call_notes
За бележките, които се въвеждат след разговора.
id: Long
phoneNumber: String
callerName: String
noteText: String
createdAt: Long

За бързо търсене направи индекс върху phoneNumber във всички таблици, а върху contacts.phoneNumber сложи unique index.

### Kotlin implementation
Поток на събитията
Входящо повикване идва в CallScreeningService.
Нормализираш номера до E.164 или поне до единен вътрешен формат.
Правиш lookup в ContactDao.

Ако има запис, записваш call_sessions и пускаш UI overlay с името и бележката.

Ако няма запис, оставяш разговора да мине и слушаш TelephonyCallback/isInCall().

При transition към idle показваш малка форма за бележка.

При save записваш нов call_notes и по желание създаваш нов contacts запис.

### Room DAO
ContactDao.findByPhoneNumber(phone: String)
CallSessionDao.insert(session)
CallSessionDao.markEnded(id, endedAt)
CallNoteDao.insert(note)

### UI
Main screen: списък с контакти и бележки.
Incoming overlay: компактна карта с име/компания/note.
Post-call bottom sheet: phone number + caller name + note text + Save.

### Android API-та
За Android частта бих използвал следните API-та:
CallScreeningService за входящото обаждане и решение allow/silence/disallow.
TelecomManager за общия call state.
TelephonyCallback.CallStateListener за real-time state updates на разговора.
Room за локална persistence layer.
NotificationManager за да можеш да показваш неинвазивна notification вместо full overlay.
SYSTEM_ALERT_WINDOW защото трябва floating overlay.

По-долу е работещ skeleton за Kotlin-only Android app с Room, CallScreeningService, и базова manifest конфигурация. CallScreeningService трябва да е деклариран с android.permission.BIND_SCREENING_SERVICE и intent action android.telecom.CallScreeningService, а Room се състои от entities, DAO и abstract RoomDatabase.

Структура на проекта
text
app/
  src/main/java/com/example/callnotes/
    data/
      ContactEntity.kt
      CallSessionEntity.kt
      CallNoteEntity.kt
      ContactDao.kt
      CallSessionDao.kt
      CallNoteDao.kt
      AppDatabase.kt
      PhoneNumberNormalizer.kt
    service/
      IncomingCallScreeningService.kt
      CallStateWatcher.kt
    ui/
      MainActivity.kt
      PostCallNoteActivity.kt
Entities
ContactEntity.kt
kotlin
package com.example.callnotes.data

Забележки по Android API
CallScreeningService е официалният механизъм за screen-ване на входящи повиквания, а Room трябва да е дефиниран с abstract RoomDatabase, entities и DAOs. TelephonyCallback.CallStateListener е наличният listener за call state, ако искаш да покажеш post-call форма след приключване на разговора.

Ето допълненията в Kotlin за watcher, реална post-call форма и clean Repository + ViewModel слой. TelephonyCallback.CallStateListener е официалният listener за call state, а Room + ViewModel е стандартната Jetpack комбинация за отделяне на UI от persistence логика.

Свързване на watcher-а
В MainActivity или в собствен service initializer можеш да стартираш CallStateWatcher и при IDLE да отвориш формата. Ако искаш по-чисто поведение, пусни watcher-а от foreground component или service, а не от Activity, за да не зависи от UI lifecycle. TelephonyCallback се регистрира през TelephonyManager.registerTelephonyCallback(Executor, TelephonyCallback).

Пълен Hilt-less skeleton за проекта, организиран като класически Android app с Kotlin, Room, CallScreeningService, TelephonyCallback, ViewModel, repository и Application-level singleton wiring. Application е правилното място за глобална инициализация, а Room database instance трябва да е singleton в един process.

Проектна структура
text
com.example.callnotes/
  CallNotesApp.kt
  AppContainer.kt

data/
  ContactEntity.kt
  CallSessionEntity.kt
  CallNoteEntity.kt
  ContactDao.kt
  CallSessionDao.kt
  CallNoteDao.kt
  AppDatabase.kt
  DatabaseProvider.kt
  PhoneNumberNormalizer.kt
  CallNotesRepository.kt

service/
  IncomingCallScreeningService.kt
  CallStateWatcher.kt
  CallUiEvents.kt

ui/
  MainActivity.kt
  MainViewModel.kt
  MainViewModelFactory.kt
  PostCallNoteActivity.kt
  PostCallNoteViewModel.kt
  PostCallNoteViewModelFactory.kt
  TextWatchers.kt

util/
  IntentExt.kt

### Gradle dependencies
kotlin
dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")

    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")

    implementation("androidx.room:room-runtime:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}

Бързи бележки
CallScreeningService служи за call screening/identification на входящи разговори, а TelephonyCallback.CallStateListener е подходящият callback за state changes; Room с repository + ViewModel е стандартният Jetpack подход. Application-класът е базовият контейнер за глобална инициализация и тук е използван за wiring без Hilt

логиката за извикване на диалог върху екрана веднага след разговора в двата подхода:

Flutter side: showModalBottomSheet за post-call форма, която се отваря при unknownCallPending stream.

Android (Kotlin) side: DialogFragment за пост-разговор форма в host app, ако искаш чисто Android UI.

showModalBottomSheet в Flutter използва Navigator за push на route и е идеален за post-call модална форма. DialogFragment е специален Fragment subclass за dialogs, който FragmentManager управлява и автоматично възстановява state.

Flutter side
1. Слушай stream от native
В main.dart (или home.dart):

dart
import 'package:flutter/material.dart';
import 'native_bridge.dart';

class HomeScreen extends StatefulWidget {
  @override
  _HomeScreenState createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  String? _pendingPhone;
  int? _pendingSessionId;

  @override
  void initState() {
    super.initState();
    _listenUnknownCall();
  }

  void _listenUnknownCall() {
    NativeBridge.unknownCallPending.listen((event) {
      _pendingPhone = event['phone'] as String?;
      _pendingSessionId = event['sessionId'] as int?;
      _showPostCallDialog();
    });
  }

  void _showPostCallDialog() {
    if (_pendingPhone == null) return;

    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      builder: (context) => _PostCallNoteSheet(
        phone: _pendingPhone!,
        sessionId: _pendingSessionId,
        onSave: (name, text) async {
          await NativeBridge.saveNote(phone: _pendingPhone!, name: name, text: text);
          if (name?.isNotEmpty == true) {
            await NativeBridge.saveContact(phone: _pendingPhone!, name: name!, note: text);
          }
          _pendingPhone = null;
          _pendingSessionId = null;
        },
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Call Notes')),
      body: const Center(child: Text('Очаквай обаждане...')),
    );
  }
}
2. Post-call BottomSheet
dart
class _PostCallNoteSheet extends StatefulWidget {
  final String phone;
  final int? sessionId;
  final Future<void> Function(String? name, String text) onSave;

  _PostCallNoteSheet({
    required this.phone,
    this.sessionId,
    required this.onSave,
  });

  @override
  _PostCallNoteSheetState createState() => _PostCallNoteSheetState();
}

class _PostCallNoteSheetState extends State<_PostCallNoteSheet> {
  final _name = TextEditingController();
  final _note = TextEditingController();

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.only(
        bottom: MediaQuery.of(context).padding.bottom,
        left: 16,
        right: 16,
        top: 16,
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Text('Нова бележка', style: Theme.of(context).textTheme.titleLarge),
          Text(widget.phone, style: Theme.of(context).textTheme.bodyMedium),
          SizedBox(height: 12),
          TextField(
            controller: _name,
            decoration: const InputDecoration(labelText: 'Име на повикващия'),
          ),
          TextField(
            controller: _note,
            decoration: const InputDecoration(labelText: 'Кратък текст'),
            maxLines: 3,
          ),
          SizedBox(height: 12),
          ElevatedButton(
            onPressed: () {
              widget.onSave(_name.text, _note.text);
              Navigator.of(context).pop();
            },
            child: const Text('Запази'),
          ),
        ],
      ),
    );
  }
}
Това диалогът се отваря автоматично при входящ unknown call чрез unknownCallPending stream от native.

Android (Kotlin) side
1. DialogFragment post-call
kotlin
package com.example.callnotes.ui

import android.app.Dialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.example.callnotes.CallNotesApp
import com.example.callnotes.data.CallNotesRepository
import com.example.callnotes.data.ContactEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PostCallDialogFragment : DialogFragment() {

    private val repository: CallNotesRepository by lazy {
        CallNotesRepository((requireActivity().application as CallNotesApp).container.database)
    }

    private val scope = CoroutineScope(Dispatchers.IO)

    var phone: String = ""
    var sessionId: Long? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = requireActivity().layoutInflater.inflate(R.layout.dialog_post_call_note, null)
        val dialog = Dialog(requireContext())
        dialog.setContentView(view)

        val nameEdit = view.findViewById<EditText>(R.id.nameEdit)
        val noteEdit = view.findViewById<EditText>(R.id.noteEdit)
        val saveBtn = view.findViewById<Button>(R.id.saveButton)

        saveBtn.setOnClickListener {
            scope.launch {
                val name = nameEdit.text.toString()
                val text = noteEdit.text.toString()
                repository.saveNote(phone, name.ifBlank { null }, text, sessionId)
                if (name.isNotBlank()) {
                    repository.saveContact(ContactEntity(0, phone, name, note = text))
                }
                dialog.dismiss()
            }
        }

        dialog.setTitle("Нова бележка")
        return dialog
    }
}
2. Layout dialog_post_call_note.xml
xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:orientation="vertical"
    android:padding="20dp"
    android:layout_width="match_parent"
    android:layout_height="wrap_content">

    <TextView
        android:id="@+id/phoneValue"
        android:textSize="16sp"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content" />

    <EditText
        android:id="@+id/nameEdit"
        android:hint="Име на повикващия"
        android:layout_width="match_parent"
        android:layout_height="wrap_content" />

    <EditText
        android:id="@+id/noteEdit"
        android:hint="Кратък текст"
        android:minLines="3"
        android:gravity="top"
        android:layout_width="match_parent"
        android:layout_height="wrap_content" />

    <Button
        android:id="@+id/saveButton"
        android:text="Запази"
        android:layout_width="match_parent"
        android:layout_height="wrap_content" />
</LinearLayout>
3. Извикване от MainActivity
kotlin
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // В CallStateWatcher.onCallStateChanged(CALL_STATE_IDLE) вече се инициализира Intent към PostCallNoteActivity.
        // Ако искаш DialogFragment вместо Activity:
        // В service или в слушач на IDLE:
        PostCallDialogFragment().apply {
            phone = incomingPhone
            sessionId = incomingSessionId
        }.show(supportFragmentManager, "post_call")
    }
}

Ключови точки
Flutter: showModalBottomSheet + EventChannel stream unknownCallPending отваря диалог автоматично.
Kotlin: DialogFragment.show(fragmentManager, tag) управлява dialog state и автоматично възстановява при config changes.
При запазване се извиква saveNote/saveContact чрез MethodChannel в/flutter bridge или директно в Kotlin.
