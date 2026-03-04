package com.jewish.calendar.ui.screens.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jewish.calendar.data.SpecialPrayerContent
import com.jewish.calendar.viewmodel.SpecialPrayersViewModel

private val SpBg    = Color(0xFFF0F4FF)
private val SpBlue  = Color(0xFF1A237E)
private val SpGold  = Color(0xFFD4AF37)
private val SpInk   = Color(0xFF0D1B4E)
private val SpMuted = Color(0xFF5C6BC0)

private typealias SpecialPrayer = SpecialPrayerContent

@Suppress("unused")
private val prayers = listOf(
    SpecialPrayer(
        "💊",
        "תפילה לרפואה",
        "לחולה ולרפואתו",
        "יְהִי רָצוֹן מִלְּפָנֶיךָ ה׳ אֱלֹהֵינוּ וֵאלֹהֵי אֲבוֹתֵינוּ שֶׁתִּשְׁלַח מְהֵרָה רְפוּאָה שְׁלֵמָה מִן הַשָּׁמַיִם, רְפוּאַת הַנֶּפֶשׁ וּרְפוּאַת הַגּוּף לְכָל הַחוֹלִים וּלְכָל הַחוֹלוֹת בְּתוֹךְ שְׁאָר חוֹלֵי יִשְׂרָאֵל וְרַחֵם עֲלֵיהֶם. וְיָשׁוּבוּ לְחַיִּים טוֹבִים, לְחַיִּים אֲרֻכִּים, לְחַיִּים שֶׁיֵּשׁ בָּהֶם שָׁלוֹם וְשַׁלְוָה.\n\nרְפָאֵנוּ ה׳ וְנֵרָפֵא, הוֹשִׁיעֵנוּ וְנִוָּשֵׁעָה כִּי תְהִלָּתֵנוּ אָתָּה.\nבָּרוּךְ אַתָּה ה׳ רוֹפֵא הַחוֹלִים."
    ),
    SpecialPrayer(
        "🙏",
        "תפילה בעת צרה",
        "בעת קושי ומצוקה",
        "אָנָּא ה׳, קַשֶּׁב לְקוֹל תַּחֲנוּנַי וּשְׁמַע צַעֲקָתִי.\nאֲנִי פּוֹנֶה אֵלֶיךָ בְּלֵב שָׁבוּר וּבְנֶפֶשׁ עֲיֵפָה.\nהַרְאֵנִי אֶת הַדֶּרֶךְ, חַזֵּק אֶת לִבִּי, הָאִר עֵינַי.\n\nמִן הַמֵּצַר קָרָאתִי יָהּ, עָנָנִי בַמֶּרְחַב יָהּ.\nה׳ לִי לֹא אִירָא, מַה יַּעֲשֶׂה לִי אָדָם.\n\nתֵּן בְּלִבִּי בִּטָּחוֹן וּשְׁלָוָה, דַּע שֶׁאַתָּה עִמִּי בַּמַּסָּע הַזֶּה.\nאָמֵן."
    ),
    SpecialPrayer(
        "💑",
        "תפילה לשידוך",
        "למציאת הזיווג הנכון",
        "רִבּוֹנוֹ שֶׁל עוֹלָם, אַתָּה הוּא הַמְּזַוֵּג זִוּוּגִים.\nאֲנִי עוֹמֵד/ת לְפָנֶיךָ וּמְבַקֵּשׁ/ת שֶׁתִּשְׁלַח לִי אֶת זִיוּגִי הַנָּכוֹן, בַּעֲלוֹת מִדּוֹת, בִּשְׁמִירַת מִצְווֹת, בְּאַהֲבָה אֲמִתִּית.\n\nזַכֵּנִי לִבְנוֹת בַּיִת נֶאֱמָן בְּיִשְׂרָאֵל,\nבַּיִת שֶׁל שָׁלוֹם, שֶׁל אַהֲבָה, שֶׁל קְדֻשָּׁה.\nוּכְשֵׁם שֶׁחִבַּרְתָּ אָדָם וְחַוָּה, כֵּן תְּחַבֵּר אוֹתָנוּ בְּאַהֲבָה עוֹלָמִית.\nאָמֵן."
    ),
    SpecialPrayer(
        "💰",
        "תפילה לפרנסה",
        "לברכה בפרנסה ובעסקים",
        "יְהִי רָצוֹן מִלְּפָנֶיךָ ה׳ אֱלֹהֵינוּ וֵאלֹהֵי אֲבוֹתֵינוּ,\nשֶׁתִּשְׁלַח בְּרָכָה בְּמַעֲשֵׂי יָדֵינוּ וּבְכָל אֲשֶׁר נִפְנֶה.\nפְּתַח לָנוּ אֶת יָדְךָ הָרְחָבָה וְהַשְׂבִּיעֵנוּ מִטּוּבְךָ.\n\nאַל תַּשְׁפִּיל אוֹתָנוּ לִידֵי מַתְּנַת בָּשָׂר וָדָם,\nכִּי אִם לְיָדְךָ הַמְּלֵאָה, הָרְחָבָה וְהַגְּדוֹלָה.\nוְנֹאמַר: בָּרוּךְ אַתָּה ה׳ הַזָּן אֶת הַכֹּל.\nאָמֵן."
    ),
    SpecialPrayer(
        "👶",
        "תפילה לזרע של קיימא",
        "לברכת ילדים",
        "אָנָּא אֵל נָא, כַּאֲשֶׁר פָּקַדְתָּ אֶת שָׂרָה, רָחֵל, חַנָּה וּלְאָה,\nכֵּן פְּקָד אוֹתָנוּ בְּזֶרַע שֶׁל קַיָּמָא, בָּנִים וּבָנוֹת הַמְגַדְּלִים לְתוֹרָה וּלְחֻפָּה.\n\nזַכֵּנוּ לְגַדֵּל בָּנִים וּבְנֵי בָנִים לַתּוֹרָה, לַחֻפָּה וּלְמַעֲשִׂים טוֹבִים.\nוִיהִי בֵיתֵנוּ מָלֵא בְּשִׂמְחַת צֶאֱצָאֵינוּ.\nאָמֵן."
    ),
    SpecialPrayer(
        "🌍",
        "תפילה לשלום ישראל",
        "לשלום עמנו וארצנו",
        "אָחֵינוּ כָּל בֵּית יִשְׂרָאֵל הַנְּתוּנִים בְּצָרָה וּבַשִּׁבְיָה, הָעוֹמְדִים בֵּין בַּיָּם וּבֵין בַּיַּבָּשָׁה — הַמָּקוֹם יְרַחֵם עֲלֵיהֶם וְיוֹצִיאֵם מִצָּרָה לִרְוָחָה, וּמֵאֲפֵלָה לְאוֹרָה, וּמִשִּׁעְבּוּד לִגְאֻלָּה, הַשְׁתָּא בַּעֲגָלָא וּבִזְמַן קָרִיב.\n\nוְנֹאמַר אָמֵן."
    ),
    SpecialPrayer(
        "😔",
        "תפילה בעת עצב",
        "לנחמה ולשמחה",
        "ה׳ אֱלֹהַי, אַתָּה יוֹדֵעַ אֶת כָּל מַה שֶּׁבְּלִבִּי.\nהָעֶצֶב שֶׁמְּכַבֵּד עָלַי — קַח אוֹתוֹ מִמֶּנִּי.\nזַכֵּנִי לִשְׂמֹחַ בִּישׁוּעָתְךָ, לִרְאוֹת טוֹב בְּחַיַּי.\n\nאַל תַּשְׁלֵךְ אוֹתִי מִלְּפָנֶיךָ,\nוְרוּחַ קָדְשְׁךָ אַל תִּקַּח מִמֶּנִּי.\nמַלֵּא לִבִּי בְּשִׂמְחַת חַיִּים, בְּאַהֲבָה וּבְתִקְוָה.\nאָמֵן."
    ),
    SpecialPrayer(
        "🎓",
        "תפילה לפני לימוד",
        "לסיוע בלימוד ובהצלחה",
        "יְהִי רָצוֹן מִלְּפָנֶיךָ ה׳ אֱלֹהֵינוּ וֵאלֹהֵי אֲבוֹתֵינוּ\nשֶׁתְּהֵא תוֹרָתְךָ בְּפִינוּ וּבְפִי עַמְּךָ בֵּית יִשְׂרָאֵל\nוְנִהְיֶה אֲנַחְנוּ וְצֶאֱצָאֵינוּ וְצֶאֱצָאֵי עַמְּךָ יוֹדְעֵי שְׁמֶךָ וְלוֹמְדֵי תוֹרָתְךָ לִשְׁמָהּ.\n\nבָּרוּךְ אַתָּה ה׳ הַמְּלַמֵּד תּוֹרָה לְעַמּוֹ יִשְׂרָאֵל.\nאָמֵן."
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpecialPrayersScreen(
    onBack: () -> Unit,
    viewModel: SpecialPrayersViewModel = hiltViewModel()
) {
    val prayers by viewModel.prayers.collectAsState()
    var selected by remember { mutableStateOf<SpecialPrayerContent?>(null) }
    var fontSize by remember { mutableIntStateOf(20) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("תפילות מיוחדות", fontWeight = FontWeight.Bold, fontSize = 19.sp, color = SpBlue)
                        if (selected != null) Text(selected!!.subtitle, fontSize = 12.sp, color = SpMuted)
                        else Text("לכל עת ושעה", fontSize = 12.sp, color = SpMuted)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = if (selected != null) ({ selected = null }) else onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "חזור", tint = SpBlue)
                    }
                },
                actions = {
                    if (selected != null) {
                        TextButton(onClick = { if (fontSize > 14) fontSize -= 2 }) {
                            Text("A-", color = SpBlue, fontWeight = FontWeight.Bold)
                        }
                        TextButton(onClick = { if (fontSize < 38) fontSize += 2 }) {
                            Text("A+", color = SpBlue, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SpBg)
            )
        },
        containerColor = SpBg
    ) { padding ->
        if (selected == null) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Brush.horizontalGradient(listOf(SpBlue.copy(0.12f), SpGold.copy(0.1f))))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("\"שְׁפֹךְ לִבְּךָ כַמַּיִם נֹכַח פְּנֵי ה׳\"", fontSize = 15.sp, color = SpGold, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(4.dp))
                            Text("איכה ב, יט", fontSize = 12.sp, color = SpMuted)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                items(prayers) { prayer ->
                    ElevatedCard(
                        onClick = { selected = prayer },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        elevation = CardDefaults.elevatedCardElevation(2.dp),
                        colors = CardDefaults.elevatedCardColors(Color.White)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(prayer.emoji, fontSize = 30.sp)
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(prayer.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SpBlue)
                                Text(prayer.subtitle, fontSize = 13.sp, color = SpMuted)
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                state = rememberLazyListState(),
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.verticalGradient(listOf(SpBlue.copy(0.1f), SpGold.copy(0.06f))))
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(selected!!.emoji, fontSize = 40.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(selected!!.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = SpBlue, textAlign = TextAlign.Center)
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(
                        selected!!.text,
                        fontSize = fontSize.sp,
                        color = SpInk,
                        lineHeight = (fontSize * 1.9f).sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(24.dp))
                    Text(
                        "⸻ אָמֵן ⸻",
                        fontSize = 16.sp,
                        color = SpGold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
