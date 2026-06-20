# Session notes
_Free-form scratchpad for the main agent. Append entries as you go; the checkpoint writer reconciles them at checkpoint events. Format each entry as `## [turn N · YYYY-MM-DDTHH:MM:SSZ]` (minute precision UTC, seconds optional) followed by free-form body. Before appending: scan existing entries — if you've already noted substantially similar content, add a short `(see entry above)` reference instead of duplicating._

## [2026-06-20T16:30:00Z]
Backup/Restore feature (T3) — design decisions:

**Approach:** SAF (Storage Access Framework) for Google Drive export/import. No Google SDK dependencies.

**Settings UI:** New section in SettingsDialog with:
- "Backup" button → SAF file picker → exports JSON to chosen location, да уточним възможността за incremental backup и възможност за отделяне на настройките в отделен файл
- "Restore" button → SAF file picker → imports JSON from chosen file, да уточним дали ще презаписваме базата или ще правим "incremental" restore и възможност за restore само на настройките
- Backup frequency field (days, default 7)
- Last backup date saved in SharedPreferences

**JSON format:** Exports contacts, call notes, and settings (tags, colors, FAB position), евентуално settings в отделен файл на избраното място за основния

**Reminder mechanism (confirmed approach):**
1. PostCallNoteActivity.onCreate → async check if backup period expired → set flag if yes - ако това прекалено усложнява кода може да направим проверката при натискане на бутон, различен от Отказ, преди да затворим формата
2. PostCallNoteActivity closes → moveTaskToBack ако е натиснат Отказ, launch MainActivity with extra flag ако е натиснат бутон, различен от Отказ
3. MainActivity.onResume → if flag set → show reminder dialog with 3 options:
   - "Backup сега" → export + save new date
   - "Отложи" → save date = today + X days
   - "Отказ" → do nothing, next form close shows reminder again

**SharedPreferences keys needed:**
- backup_frequency_days (int, default 7)
- last_backup_date (long, timestamp)
- show_backup_reminder (boolean, flag between activities)
