*команда за проверка на грешки:
.\gradlew.bat compileDebugKotlin --stacktrace --info*

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
Какво беше направено по задачите:

❖ Настройки на цветове (fontColor и formBgColor): Добавени са в MainViewModel (и MainUiState) с възможност за избор и запазване. SettingsDialog вече съдържа контроли за двата нови параметъра, а формата "Нова бележка" (PostCallNoteActivity) ги чете и оцветява текстовете и фона си спрямо тях.
❖ Сенки на картите: Добавено е elevation = CardDefaults.cardElevation(defaultElevation = 4.dp) на картите на бележките и контактите за по-привлекателен триизмерен вид.
❖ Етикети при обаждане: PhoneStateReceiver вече извлича записаните в базата данни етикети за контакта и ги изпраща към OverlayService, който ги визуализира в нов tv_caller_tags компонент над бутона за затваряне.
❖ Хедър фон: Цветът на фона на приложението вече се прилага и върху TopAppBar (хедъра) за единен дизайн.
❖ Икона в хедъра: Старата икона е заменена с оранжев кръг с бял телефон в него – точно копие на логото/иконата на самото приложение.
❖ FAB бутон Х: Добавен е червен плаващ бутон "X". При единично цъкване той минимизира приложението (moveTaskToBack(true)). При дълго натискане (long press) около него се появява меню за бърз достъп с 4 икони: Добавяне, Контакти, Бележки и Настройки.
❖ История на последното обаждане: При приключване на разговор PhoneStateReceiver записва телефонния номер и името в SharedPreferences под ключовете "last_call_phone" и "last_call_name". При отваряне на "Нова бележка" (през бутона "+"), ако телефонът е празен, автоматично се зареждат тези запазени данни.
❖ Увеличаване на броя етикети: Лимитът за брой етикети в Настройки е повдигнат от 10 на 20.
❖ Цветни квадратчета в Настройки: Направени са с RoundedCornerShape(6.dp) – изглеждат като модерни цветни квадратчета вместо кръгчета.
❖ Интерактивен Color Picker: Всички числови стойности на RGB слайдерите и HEX стойността вече са текстови полета (OutlinedTextField), които могат да се редактират ръчно.
❖ Промяна на формата "Нова бележка" на Овърлей: Формата вече е обгърната в Compose Dialog прозорец с Card обвивка, което я превръща в овърлей диалог (като настройките), вместо да заема цял екран.
❖ Промяна на лейбъла: Текстът "Телефонен номер" е сменен на "Телефон".
❖ Съкращаване на бележката в Контакти: Текстът на последната бележка в списъка с контакти вече се ограничава точно до 1 ред с многоточие накрая (maxLines = 1, Ellipsis).
❖ Бутон "Обнови" и "Добави": При редактиране на контакт бутонът "Запази" е преименуван на "Добави", и е добавен нов бутон "Обнови" (викащ updateNote()), който само актуализира информацията в контакта без да генерира нова бележка в хронологията.

❖ Разширяване на диалозите до 95%: В PostCallNoteActivity вече се използва usePlatformDefaultWidth = false с fillMaxWidth(0.95f), което разширява формата "Нова бележка / Редактиране" до 95% от ширината на екрана.
❖ Квадрат за избрания цвят в Настройки: В ColorSelectorRow първият елемент е квадратен преглед (RoundedCornerShape(4.dp)) на текущо избрания цвят. Всички останали опции са кръгли (CircleShape). Избраният в момента цвят се показва в реално време в квадратчето.
❖ FAB бутон "X":
❖ При кратко натискане, ако менюто е отворено, то се затваря. Ако е затворено, приложението се минимизира (moveTaskToBack(true)).
❖ Дългото натискане сменя състоянието на менюто.
❖ Икони около FAB: Разпределени са в правилна дъга (квадрант от 90° до 180°) около долния десен ъгъл.
❖ Primary, Secondary, Tertiary настройки за цвят: В настройките вече има три допълнителни селектора за Primary, Secondary и Tertiary цветове на темата. Промените по тях се записват и се зареждат динамично от CallNotesTheme, което директно преобразява интерфейса на цялото приложение.

MainActivity.kt
val presets = listOf(
        "default" to Color.LightGray,
        "#FFF9C4" to Color(0xFFFFF9C4),
        "#E3F2FD" to Color(0xFFE3F2FD),
        "#E8F5E9" to Color(0xFFE8F5E9),
        "#F5F5F5" to Color(0xFFF5F5F5),
        "#E0F2F1" to Color(0xFFE0F2F1)
        "default" to Color(0xFF37474F),
        "#121212" to Color(0xFF121212),
        "#1A237E" to Color(0xFF1A237E),
        "#1B5E20" to Color(0xFF1B5E20),
        "#3E2723" to Color(0xFF3E2723),
        "#004D40" to Color(0xFF004D40)

surfaceContainerLow (фонът на контакт картите) и secondaryContainer (фонът на бележки картите) автоматично стават много тъмни/черни.

Решението: Принудих приложението винаги да използва светла тема с ясно дефинирани цветове:

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

### call_sessions
За всеки входящ/изходящ разговор.
id: Long
phoneNumber: String
callType: Int
startedAt: Long
screenedKnown: Boolean
knownContactId: Long
state: Int

### call_notes
За бележките, които се въвеждат след разговора.
id: Long
phoneNumber: String
callerName: String
noteText: String
createdAt: Long
callSessionId: Long

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

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "contacts",
    indices = [Index(value = ["phoneNumber"], unique = true)]
)
data class ContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val phoneNumber: String,
    val displayName: String,
    val company: String? = null,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
CallSessionEntity.kt
kotlin
package com.example.callnotes.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "call_sessions",
    indices = [Index(value = ["phoneNumber"]), Index(value = ["knownContactId"])]
)
data class CallSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val phoneNumber: String,
    val callType: Int,
    val startedAt: Long = System.currentTimeMillis(),
    val endedAt: Long? = null,
    val screenedKnown: Boolean = false,
    val knownContactId: Long? = null,
    val state: Int = 0
)
CallNoteEntity.kt
kotlin
package com.example.callnotes.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "call_notes",
    indices = [Index(value = ["phoneNumber"]), Index(value = ["callSessionId"])]
)
data class CallNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val phoneNumber: String,
    val callerName: String? = null,
    val noteText: String,
    val createdAt: Long = System.currentTimeMillis(),
    val callSessionId: Long? = null
)
DAO
ContactDao.kt
kotlin
package com.example.callnotes.data

import androidx.room.*

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts WHERE phoneNumber = :phone LIMIT 1")
    suspend fun findByPhone(phone: String): ContactEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(contact: ContactEntity): Long

    @Query("SELECT * FROM contacts ORDER BY updatedAt DESC")
    suspend fun getAll(): List<ContactEntity>
}
CallSessionDao.kt
kotlin
package com.example.callnotes.data

import androidx.room.*

@Dao
interface CallSessionDao {
    @Insert
    suspend fun insert(session: CallSessionEntity): Long

    @Query("UPDATE call_sessions SET endedAt = :endedAt, state = :state WHERE id = :id")
    suspend fun markEnded(id: Long, endedAt: Long, state: Int = 0)

    @Query("UPDATE call_sessions SET screenedKnown = 1, knownContactId = :contactId WHERE id = :id")
    suspend fun markKnown(id: Long, contactId: Long)
}
CallNoteDao.kt
kotlin
package com.example.callnotes.data

import androidx.room.*

@Dao
interface CallNoteDao {
    @Insert
    suspend fun insert(note: CallNoteEntity): Long

    @Query("SELECT * FROM call_notes ORDER BY createdAt DESC")
    suspend fun getAll(): List<CallNoteEntity>
}
Database
AppDatabase.kt
kotlin
package com.example.callnotes.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ContactEntity::class, CallSessionEntity::class, CallNoteEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun callSessionDao(): CallSessionDao
    abstract fun callNoteDao(): CallNoteDao
}
Service
IncomingCallScreeningService.kt
kotlin
package com.example.callnotes.service

import android.telecom.Call
import android.telecom.CallScreeningService
import com.example.callnotes.data.AppDatabase
import com.example.callnotes.data.CallSessionEntity
import com.example.callnotes.data.ContactEntity
import com.example.callnotes.data.PhoneNumberNormalizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class IncomingCallScreeningService : CallScreeningService() {

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onScreenCall(details: Call.Details) {
        val rawNumber = details.handle?.schemeSpecificPart ?: return
        val phone = PhoneNumberNormalizer.normalize(rawNumber)

        scope.launch {
            val db = DatabaseProvider.get(applicationContext)
            val contact = db.contactDao().findByPhone(phone)

            val sessionId = db.callSessionDao().insert(
                CallSessionEntity(
                    phoneNumber = phone,
                    callType = Call.Details.DIRECTION_INCOMING,
                    screenedKnown = contact != null,
                    knownContactId = contact?.id
                )
            )

            if (contact != null) {
                respondToCall(
                    details,
                    CallResponse.Builder()
                        .setDisallowCall(false)
                        .setSilenceCall(false)
                        .setSkipCallLog(false)
                        .build()
                )
                CallUiEvents.postKnownCall(phone, contact.displayName, contact.note, sessionId)
            } else {
                respondToCall(
                    details,
                    CallResponse.Builder()
                        .setDisallowCall(false)
                        .setSilenceCall(false)
                        .setSkipCallLog(false)
                        .build()
                )
                CallUiEvents.postUnknownCall(phone, sessionId)
            }
        }
    }
}
Helper
PhoneNumberNormalizer.kt
kotlin
package com.example.callnotes.data

object PhoneNumberNormalizer {
    fun normalize(raw: String): String =
        raw.replace("\\s+".toRegex(), "").replace("-", "").trim()
}
DatabaseProvider.kt
kotlin
package com.example.callnotes.data

import android.content.Context
import androidx.room.Room

object DatabaseProvider {
    @Volatile private var instance: AppDatabase? = null

    fun get(context: Context): AppDatabase =
        instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "callnotes.db"
            ).build().also { instance = it }
        }
}
Manifest
AndroidManifest.xml
xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.READ_PHONE_STATE" />
    <uses-permission android:name="android.permission.READ_CALL_LOG" />
    <uses-permission android:name="android.permission.ANSWER_PHONE_CALLS" />
    <uses-permission android:name="android.permission.BIND_SCREENING_SERVICE" />

    <application
        android:name=".CallNotesApp"
        android:allowBackup="true"
        android:label="Call Notes"
        android:supportsRtl="true">

        <service
            android:name=".service.IncomingCallScreeningService"
            android:permission="android.permission.BIND_SCREENING_SERVICE"
            android:exported="true">
            <intent-filter>
                <action android:name="android.telecom.CallScreeningService" />
            </intent-filter>
        </service>

        <activity
            android:name=".ui.MainActivity"
            android:exported="true" />

        <activity
            android:name=".ui.PostCallNoteActivity"
            android:exported="false" />
    </application>
</manifest>
Забележки по Android API
CallScreeningService е официалният механизъм за screen-ване на входящи повиквания, а Room трябва да е дефиниран с abstract RoomDatabase, entities и DAOs. TelephonyCallback.CallStateListener е наличният listener за call state, ако искаш да покажеш post-call форма след приключване на разговора.

Ето допълненията в Kotlin за watcher, реална post-call форма и clean Repository + ViewModel слой. TelephonyCallback.CallStateListener е официалният listener за call state, а Room + ViewModel е стандартната Jetpack комбинация за отделяне на UI от persistence логика.

CallStateWatcher
CallStateWatcher.kt
kotlin
package com.example.callnotes.service

import android.content.Context
import android.content.Intent
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.example.callnotes.ui.PostCallNoteActivity
import java.util.concurrent.Executor

class CallStateWatcher(
    private val context: Context,
    private val onIdle: () -> Unit
) {
    private val telephonyManager =
        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    private val executor = Executor { command -> command.run() }

    private val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
        override fun onCallStateChanged(state: Int) {
            when (state) {
                TelephonyManager.CALL_STATE_RINGING -> Unit
                TelephonyManager.CALL_STATE_OFFHOOK -> Unit
                TelephonyManager.CALL_STATE_IDLE -> {
                    onIdle()
                    val intent = Intent(context, PostCallNoteActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    ContextCompat.startActivity(context, intent, null)
                }
            }
        }
    }

    fun start() {
        telephonyManager.registerTelephonyCallback(executor, callback)
    }

    fun stop() {
        telephonyManager.unregisterTelephonyCallback(callback)
    }
}
Repository
CallNotesRepository.kt
kotlin
package com.example.callnotes.data

class CallNotesRepository(
    private val db: AppDatabase
) {
    private val contactDao = db.contactDao()
    private val sessionDao = db.callSessionDao()
    private val noteDao = db.callNoteDao()

    suspend fun findContact(phone: String): ContactEntity? =
        contactDao.findByPhone(phone)

    suspend fun saveContact(contact: ContactEntity): Long =
        contactDao.upsert(contact)

    suspend fun createSession(phone: String, known: ContactEntity?): Long =
        sessionDao.insert(
            CallSessionEntity(
                phoneNumber = phone,
                callType = 1,
                screenedKnown = known != null,
                knownContactId = known?.id
            )
        )

    suspend fun endSession(sessionId: Long) {
        sessionDao.markEnded(sessionId, System.currentTimeMillis(), state = 0)
    }

    suspend fun saveNote(
        phone: String,
        callerName: String?,
        noteText: String,
        sessionId: Long? = null
    ): Long {
        return noteDao.insert(
            CallNoteEntity(
                phoneNumber = phone,
                callerName = callerName,
                noteText = noteText,
                callSessionId = sessionId
            )
        )
    }

    suspend fun getAllContacts(): List<ContactEntity> = contactDao.getAll()
    suspend fun getAllNotes(): List<CallNoteEntity> = noteDao.getAll()
}
ViewModel
PostCallNoteViewModel.kt
kotlin
package com.example.callnotes.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.callnotes.data.CallNotesRepository
import com.example.callnotes.data.ContactEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class PostCallNoteUiState(
    val phoneNumber: String = "",
    val callerName: String = "",
    val noteText: String = "",
    val sessionId: Long? = null,
    val saved: Boolean = false
)

class PostCallNoteViewModel(
    private val repository: CallNotesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PostCallNoteUiState())
    val uiState: StateFlow<PostCallNoteUiState> = _uiState

    fun init(phone: String, sessionId: Long? = null) {
        _uiState.value = _uiState.value.copy(phoneNumber = phone, sessionId = sessionId)
    }

    fun updateCallerName(value: String) {
        _uiState.value = _uiState.value.copy(callerName = value)
    }

    fun updateNoteText(value: String) {
        _uiState.value = _uiState.value.copy(noteText = value)
    }

    fun save() {
        viewModelScope.launch {
            val state = _uiState.value
            repository.saveNote(
                phone = state.phoneNumber,
                callerName = state.callerName.ifBlank { null },
                noteText = state.noteText,
                sessionId = state.sessionId
            )
            if (state.callerName.isNotBlank()) {
                repository.saveContact(
                    ContactEntity(
                        phoneNumber = state.phoneNumber,
                        displayName = state.callerName,
                        note = state.noteText
                    )
                )
            }
            _uiState.value = state.copy(saved = true)
        }
    }
}
PostCallNoteViewModelFactory.kt
kotlin
package com.example.callnotes.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.callnotes.data.CallNotesRepository

class PostCallNoteViewModelFactory(
    private val repository: CallNotesRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PostCallNoteViewModel::class.java)) {
            return PostCallNoteViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
Real form
PostCallNoteActivity.kt
kotlin
package com.example.callnotes.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.callnotes.R
import com.example.callnotes.data.AppDatabase
import com.example.callnotes.data.CallNotesRepository
import com.example.callnotes.data.DatabaseProvider
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PostCallNoteActivity : ComponentActivity() {

    private val viewModel: PostCallNoteViewModel by viewModels {
        PostCallNoteViewModelFactory(
            CallNotesRepository(DatabaseProvider.get(applicationContext))
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_call_note)

        val phone = intent.getStringExtra(EXTRA_PHONE) ?: ""
        val sessionId = intent.getLongExtra(EXTRA_SESSION_ID, -1).takeIf { it >= 0 }

        viewModel.init(phone, sessionId)

        findViewById<TextView>(R.id.phoneValue).text = phone
        val nameEdit = findViewById<EditText>(R.id.nameEdit)
        val noteEdit = findViewById<EditText>(R.id.noteEdit)
        val saveBtn = findViewById<Button>(R.id.saveButton)

        nameEdit.addTextChangedListenerSimple { viewModel.updateCallerName(it) }
        noteEdit.addTextChangedListenerSimple { viewModel.updateNoteText(it) }

        saveBtn.setOnClickListener {
            viewModel.save()
        }

        lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                if (state.saved) finish()
            }
        }
    }

    companion object {
        const val EXTRA_PHONE = "extra_phone"
        const val EXTRA_SESSION_ID = "extra_session_id"
    }
}
TextWatchers.kt
kotlin
package com.example.callnotes.ui

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

fun EditText.addTextChangedListenerSimple(onText: (String) -> Unit) {
    addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) = onText(s?.toString().orEmpty())
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    })
}
Layout
activity_post_call_note.xml
xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:orientation="vertical"
    android:padding="20dp"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <TextView
        android:id="@+id/title"
        android:text="Нова бележка"
        android:textSize="20sp"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content" />

    <TextView
        android:id="@+id/phoneValue"
        android:textSize="16sp"
        android:layout_marginTop="12dp"
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
Свързване на watcher-а
В MainActivity или в собствен service initializer можеш да стартираш CallStateWatcher и при IDLE да отвориш формата. Ако искаш по-чисто поведение, пусни watcher-а от foreground component или service, а не от Activity, за да не зависи от UI lifecycle. TelephonyCallback се регистрира през TelephonyManager.registerTelephonyCallback(Executor, TelephonyCallback).

Бележка за реалния поток
В практична версия бих препоръчал PostCallNoteActivity да получава phone и sessionId през Intent, а CallScreeningService да ги подава чрез shared state или repository lookup.

пълен Hilt-less skeleton за проекта, организиран като класически Android app с Kotlin, Room, CallScreeningService, TelephonyCallback, ViewModel, repository и Application-level singleton wiring. Application е правилното място за глобална инициализация, а Room database instance трябва да е singleton в един process.

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
Gradle dependencies
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
Application wiring
CallNotesApp.kt
kotlin
package com.example.callnotes

import android.app.Application
import com.example.callnotes.data.DatabaseProvider
import com.example.callnotes.data.CallNotesRepository

class CallNotesApp : Application() {
    val container by lazy {
        AppContainer(this)
    }

    override fun onCreate() {
        super.onCreate()
        DatabaseProvider.init(this)
    }
}
AppContainer.kt
kotlin
package com.example.callnotes

import android.content.Context
import com.example.callnotes.data.CallNotesRepository
import com.example.callnotes.data.DatabaseProvider

class AppContainer(context: Context) {
    val repository: CallNotesRepository = CallNotesRepository(DatabaseProvider.get(context))
}
Data layer
DatabaseProvider.kt
kotlin
package com.example.callnotes.data

import android.content.Context
import androidx.room.Room

object DatabaseProvider {
    @Volatile private var instance: AppDatabase? = null
    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun get(context: Context = appContext): AppDatabase =
        instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "callnotes.db"
            ).build().also { instance = it }
        }
}
CallNotesRepository.kt
kotlin
package com.example.callnotes.data

class CallNotesRepository(
    private val db: AppDatabase
) {
    suspend fun findContact(phone: String) = db.contactDao().findByPhone(phone)
    suspend fun saveContact(contact: ContactEntity) = db.contactDao().upsert(contact)
    suspend fun getAllContacts() = db.contactDao().getAll()
    suspend fun getAllNotes() = db.callNoteDao().getAll()

    suspend fun createSession(phone: String, known: ContactEntity?) =
        db.callSessionDao().insert(
            CallSessionEntity(
                phoneNumber = phone,
                callType = 1,
                screenedKnown = known != null,
                knownContactId = known?.id
            )
        )

    suspend fun endSession(sessionId: Long) {
        db.callSessionDao().markEnded(sessionId, System.currentTimeMillis(), state = 0)
    }

    suspend fun saveNote(
        phone: String,
        callerName: String?,
        noteText: String,
        sessionId: Long? = null
    ) = db.callNoteDao().insert(
        CallNoteEntity(
            phoneNumber = phone,
            callerName = callerName,
            noteText = noteText,
            callSessionId = sessionId
        )
    )
}
Call screening
IncomingCallScreeningService.kt
kotlin
package com.example.callnotes.service

import android.telecom.Call
import android.telecom.CallScreeningService
import com.example.callnotes.data.DatabaseProvider
import com.example.callnotes.data.PhoneNumberNormalizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class IncomingCallScreeningService : CallScreeningService() {

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onScreenCall(details: Call.Details) {
        val raw = details.handle?.schemeSpecificPart ?: return
        val phone = PhoneNumberNormalizer.normalize(raw)

        scope.launch {
            val db = DatabaseProvider.get(applicationContext)
            val contact = db.contactDao().findByPhone(phone)

            val sessionId = db.callSessionDao().insert(
                com.example.callnotes.data.CallSessionEntity(
                    phoneNumber = phone,
                    callType = Call.Details.DIRECTION_INCOMING,
                    screenedKnown = contact != null,
                    knownContactId = contact?.id
                )
            )

            respondToCall(
                details,
                CallResponse.Builder()
                    .setDisallowCall(false)
                    .setSilenceCall(false)
                    .setSkipCallLog(false)
                    .build()
            )

            if (contact != null) {
                CallUiEvents.emitKnown(phone, contact.displayName, contact.note, sessionId)
            } else {
                CallUiEvents.emitUnknown(phone, sessionId)
            }
        }
    }
}
CallUiEvents.kt
kotlin
package com.example.callnotes.service

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

data class KnownCallEvent(val phone: String, val name: String, val note: String?, val sessionId: Long)
data class UnknownCallEvent(val phone: String, val sessionId: Long)

object CallUiEvents {
    private val _known = MutableSharedFlow<KnownCallEvent>(extraBufferCapacity = 1)
    private val _unknown = MutableSharedFlow<UnknownCallEvent>(extraBufferCapacity = 1)

    val known: SharedFlow<KnownCallEvent> = _known
    val unknown: SharedFlow<UnknownCallEvent> = _unknown

    fun emitKnown(phone: String, name: String, note: String?, sessionId: Long) {
        _known.tryEmit(KnownCallEvent(phone, name, note, sessionId))
    }

    fun emitUnknown(phone: String, sessionId: Long) {
        _unknown.tryEmit(UnknownCallEvent(phone, sessionId))
    }
}
Call state watcher
CallStateWatcher.kt
kotlin
package com.example.callnotes.service

import android.content.Context
import android.content.Intent
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.example.callnotes.ui.PostCallNoteActivity
import java.util.concurrent.Executor

class CallStateWatcher(
    private val context: Context,
    private val onIdle: () -> Unit
) {
    private val telephonyManager =
        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    private val executor = Executor { it.run() }

    private val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
        override fun onCallStateChanged(state: Int) {
            if (state == TelephonyManager.CALL_STATE_IDLE) {
                onIdle()
                val intent = Intent(context, PostCallNoteActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ContextCompat.startActivity(context, intent, null)
            }
        }
    }

    fun start() {
        telephonyManager.registerTelephonyCallback(executor, callback)
    }

    fun stop() {
        telephonyManager.unregisterTelephonyCallback(callback)
    }
}
ViewModels
MainViewModel.kt
kotlin
package com.example.callnotes.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.callnotes.data.CallNotesRepository
import com.example.callnotes.data.ContactEntity
import com.example.callnotes.data.CallNoteEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class MainUiState(
    val contacts: List<ContactEntity> = emptyList(),
    val notes: List<CallNoteEntity> = emptyList()
)

class MainViewModel(
    private val repository: CallNotesRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state

    fun load() {
        viewModelScope.launch {
            _state.value = MainUiState(
                contacts = repository.getAllContacts(),
                notes = repository.getAllNotes()
            )
        }
    }
}
MainViewModelFactory.kt
kotlin
package com.example.callnotes.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.callnotes.data.CallNotesRepository

class MainViewModelFactory(
    private val repository: CallNotesRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
PostCallNoteViewModel.kt
kotlin
package com.example.callnotes.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.callnotes.data.CallNotesRepository
import com.example.callnotes.data.ContactEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class PostCallNoteUiState(
    val phoneNumber: String = "",
    val callerName: String = "",
    val noteText: String = "",
    val sessionId: Long? = null,
    val saved: Boolean = false
)

class PostCallNoteViewModel(
    private val repository: CallNotesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PostCallNoteUiState())
    val uiState: StateFlow<PostCallNoteUiState> = _uiState

    fun init(phone: String, sessionId: Long? = null) {
        _uiState.value = _uiState.value.copy(phoneNumber = phone, sessionId = sessionId)
    }

    fun updateCallerName(value: String) {
        _uiState.value = _uiState.value.copy(callerName = value)
    }

    fun updateNoteText(value: String) {
        _uiState.value = _uiState.value.copy(noteText = value)
    }

    fun save() {
        viewModelScope.launch {
            val s = _uiState.value
            repository.saveNote(s.phoneNumber, s.callerName.ifBlank { null }, s.noteText, s.sessionId)
            if (s.callerName.isNotBlank()) {
                repository.saveContact(
                    ContactEntity(
                        phoneNumber = s.phoneNumber,
                        displayName = s.callerName,
                        note = s.noteText
                    )
                )
            }
            _uiState.value = s.copy(saved = true)
        }
    }
}
PostCallNoteViewModelFactory.kt
kotlin
package com.example.callnotes.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.callnotes.data.CallNotesRepository

class PostCallNoteViewModelFactory(
    private val repository: CallNotesRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PostCallNoteViewModel::class.java)) {
            return PostCallNoteViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
UI activities
MainActivity.kt
kotlin
package com.example.callnotes.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import com.example.callnotes.CallNotesApp

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory((application as CallNotesApp).container.repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.load()
    }
}
PostCallNoteActivity.kt
kotlin
package com.example.callnotes.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.callnotes.CallNotesApp
import com.example.callnotes.R
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PostCallNoteActivity : ComponentActivity() {

    private val viewModel: PostCallNoteViewModel by viewModels {
        PostCallNoteViewModelFactory((application as CallNotesApp).container.repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_call_note)

        val phone = intent.getStringExtra(EXTRA_PHONE) ?: ""
        val sessionId = intent.getLongExtra(EXTRA_SESSION_ID, -1).takeIf { it >= 0 }

        viewModel.init(phone, sessionId)

        val phoneValue = findViewById<TextView>(R.id.phoneValue)
        val nameEdit = findViewById<EditText>(R.id.nameEdit)
        val noteEdit = findViewById<EditText>(R.id.noteEdit)
        val saveBtn = findViewById<Button>(R.id.saveButton)

        phoneValue.text = phone
        nameEdit.addTextChangedListenerSimple { viewModel.updateCallerName(it) }
        noteEdit.addTextChangedListenerSimple { viewModel.updateNoteText(it) }

        saveBtn.setOnClickListener { viewModel.save() }

        lifecycleScope.launch {
            viewModel.uiState.collectLatest { if (it.saved) finish() }
        }
    }

    companion object {
        const val EXTRA_PHONE = "extra_phone"
        const val EXTRA_SESSION_ID = "extra_session_id"
    }
}
Manifest
AndroidManifest.xml
xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.READ_PHONE_STATE" />
    <uses-permission android:name="android.permission.READ_CALL_LOG" />
    <uses-permission android:name="android.permission.ANSWER_PHONE_CALLS" />
    <uses-permission android:name="android.permission.BIND_SCREENING_SERVICE" />

    <application
        android:name=".CallNotesApp"
        android:allowBackup="true"
        android:label="Call Notes"
        android:supportsRtl="true">

        <service
            android:name=".service.IncomingCallScreeningService"
            android:permission="android.permission.BIND_SCREENING_SERVICE"
            android:exported="true">
            <intent-filter>
                <action android:name="android.telecom.CallScreeningService" />
            </intent-filter>
        </service>

        <activity
            android:name=".ui.MainActivity"
            android:exported="true" />

        <activity
            android:name=".ui.PostCallNoteActivity"
            android:exported="false" />
    </application>
</manifest>
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
