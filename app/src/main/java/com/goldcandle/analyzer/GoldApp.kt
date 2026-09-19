package com.goldcandle.analyzer

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.DecimalFormat
import java.time.LocalDateTime

private val fmt = DecimalFormat("0.00")
data class Candle(val time: String, val open: Double, val high: Double, val low: Double, val close: Double)
data class Signal(val time: String, val side: String, val entry: Double, val stop: Double, val target: Double, val reason: String)

class GoldViewModel : ViewModel() {
    private val _candles = MutableStateFlow(demoCandles()); val candles = _candles.asStateFlow()
    private val _signals = MutableStateFlow<List<Signal>>(emptyList()); val signals = _signals.asStateFlow()
    fun refresh() { val c = demoCandles(); _candles.value = c; _signals.value = detect(c) + _signals.value }
    private fun detect(c: List<Candle>): List<Signal> { if (c.size < 2) return emptyList(); val a=c[c.size-2]; val b=c.last(); val bullish=b.close>b.open && b.close>a.high; val bearish=b.close<b.open && b.close<a.low; val atr=(b.high-b.low).coerceAtLeast(0.5); return when { bullish -> listOf(Signal(LocalDateTime.now().toString(),"شراء",b.close,b.close-atr*1.3,b.close+atr*2,"اختراق قمة الشمعة السابقة مع إغلاق صاعد")); bearish -> listOf(Signal(LocalDateTime.now().toString(),"بيع",b.close,b.close+atr*1.3,b.close-atr*2,"كسر قاع الشمعة السابقة مع إغلاق هابط")); else -> emptyList() } }
}

@Composable fun GoldApp(vm: GoldViewModel = viewModel()) { val candles by vm.candles.collectAsState(); val signals by vm.signals.collectAsState(); val last=candles.last(); Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment=Alignment.End) { Text("محلل الذهب", style=MaterialTheme.typography.headlineMedium); Text("XAUUSDc • M5", style=MaterialTheme.typography.titleMedium); Spacer(Modifier.height(12.dp)); Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text("آخر سعر: ${fmt.format(last.close)}", style=MaterialTheme.typography.headlineSmall); Text("تحليل أولي: ${if(last.close>=last.open) "ميل صاعد" else "ميل هابط"}"); Text("تنبيه: البيانات الحالية تجريبية حتى ربط خادم الوسيط.") } }; Spacer(Modifier.height(12.dp)); Button(onClick={vm.refresh()}, modifier=Modifier.fillMaxWidth()) { Text("تحديث وتحليل") }; Spacer(Modifier.height(12.dp)); Text("سجل الإشارات", style=MaterialTheme.typography.titleLarge); LazyColumn { items(signals) { s -> ListItem(headlineContent={Text("${s.side} • دخول ${fmt.format(s.entry)}")}, supportingContent={Text("وقف ${fmt.format(s.stop)} | هدف ${fmt.format(s.target)}\n${s.reason}")}) } } } }

private fun demoCandles(): List<Candle> { val now=LocalDateTime.now(); val base=4378.44; return (0 until 30).map { i -> val o=base+i*.08; val c=o+(if(i==29) 1.2 else if(i%3==0) -.2 else .15); Candle(now.minusMinutes((29-i)*5).toString(),o,maxOf(o,c)+.4,minOf(o,c)-.3,c) } }
