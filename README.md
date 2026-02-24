# יהודה - לוח שנה יהודי דתי 📅

אפליקציית אנדרואיד מודרנית לציבור הדתי יהודי, בנויה עם Jetpack Compose.

## תכונות עיקריות

### 📅 לוח שנה עברי
- תצוגה כפולה תאריך עברי + לועזי
- מספרי גמטריה לימים עבריים
- הדגשת שבת, חגים, ראש חודש
- פרשת השבוע, ספירת העומר
- ניווט בין חודשים

### ⏰ זמני היום (זמנים הלכתיים)
- חישוב לפי מיקום GPS
- עלות השחר, הנץ, סוף זמן קריאת שמע (גר"א + מג"א)
- חצות, מנחה גדולה/קטנה, פלג המנחה
- שקיעה, צאת הכוכבים, רבנו תם
- הדלקת נרות לשבת/יום טוב

### 💧 ניהול טהרה (לנשים)
- מעקב מחזור - תחילה וסיום
- ספירת שבעה נקיים עם צ'קליסט יומי
- חישוב יום הטבילה
- הערות יומן פרטיות
- מפת מקוואות בסביבה עם פרטים

### 📿 בוט הלכתי (AI)
- שאלות הלכתיות מופעל ע"י Claude AI
- 6 נושאים: שבת, כשרות, תפילה, חגים, טהרה, ברכות
- ציון מקורות (שו"ע, משנה ברורה)
- היסטוריית שיחה
- disclaimer ברור - לא מחליף רב

## טכנולוגיות

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **Calendar**: KosherJava (Hebrew Calendar & Zmanim)
- **AI**: Claude API (Anthropic)
- **Database**: Room
- **DI**: Hilt
- **Navigation**: Navigation Compose

## הגדרה

1. שכפל את הפרויקט
2. צור קובץ `local.properties` עם:
```
CLAUDE_API_KEY=your_anthropic_api_key
GOOGLE_MAPS_KEY=your_google_maps_key
```
3. הרץ עם Android Studio

## מבנה הפרויקט

```
app/src/main/java/com/jewish/calendar/
├── MainActivity.kt
├── YehudaApp.kt
├── di/
│   └── AppModule.kt          # Hilt dependency injection
├── model/
│   ├── HebrewDateModel.kt    # Hebrew date data classes
│   ├── ZmanimModel.kt        # Zmanim data classes
│   ├── MikvehModel.kt        # Mikveh / cycle tracking
│   └── ChatModel.kt          # AI bot models
├── data/
│   ├── HebrewCalendarRepository.kt
│   ├── ZmanimRepository.kt
│   ├── MikvehRepository.kt
│   ├── ClaudeApiService.kt
│   └── AppDatabase.kt        # Room database
├── viewmodel/
│   ├── CalendarViewModel.kt
│   ├── ZmanimViewModel.kt
│   ├── MikvehViewModel.kt
│   └── HalachicBotViewModel.kt
└── ui/
    ├── theme/                 # Colors, Typography, Theme
    ├── navigation/            # Bottom nav + NavHost
    └── screens/
        ├── calendar/          # Hebrew calendar screen
        ├── zmanim/            # Daily times screen
        ├── mikveh/            # Mikveh tracker screen
        └── bot/               # AI halachic bot screen
```

## פרטיות

נתוני טהרה המשפחה נשמרים **מקומית בלבד** על המכשיר ולא מועלים לשרת.
