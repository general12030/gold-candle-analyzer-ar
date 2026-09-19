package com.goldcandle.analyzer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import com.goldcandle.analyzer.data.AppDatabase
import com.goldcandle.analyzer.data.Candle
import com.goldcandle.analyzer.data.MarketRepository
import com.goldcandle.analyzer.data.SignalEntity
import com.goldcandle.analyzer.data.SignalDetector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs

private val priceFormat = DecimalFormat("###,###.##")
private val dateFormatter = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())

data class MarketUiState(
    val symbol: String = "XAUUSDc",
    val timeframe: String = "M5",
    val currentPrice: Double = 4378.44,
    val analysisSummary: String = "التحليل مبدئي فقط ومصادره بيانات تجريبية.",
    val candles: List<Candle> = emptyList(),
    val signals: List<SignalEntity> = emptyList(),
    val brokerUrl: String = "",
    val status: String = "جاهز لربط خادم الوسيط"
)

class GoldViewModel(application: android.app.Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val repository = MarketRepository(db)
    private val _uiState = MutableStateFlow(MarketUiState())
    val uiState: StateFlow<MarketUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeSignals().collectLatest { list ->
                _uiState.value = _uiState.value.copy(signals = list)
            }
        }
        refreshMarket()
    }

    fun updateBrokerUrl(value: String) {
        _uiState.value = _uiState.value.copy(brokerUrl = value)
    }

    fun refreshMarket() {
        viewModelScope.launch {
            val candles = repository.generateDemoCandles()
            val latest = candles.last()
            val detector = SignalDetector()
            val newSignals = detector.detect(candles)
            val finalSignals = newSignals.ifEmpty { listOf(SignalEntity(0, "محايد", latest.close, latest.close - 1.2, latest.close + 1.2, "تجميع", 50, "لا توجد إشارة قوية في اللحظة الحالية", System.currentTimeMillis())) }

            repository.storeSignals(finalSignals)

            val analysis = buildAnalysis(candles, latest.close)
            val status = when {
                latest.close > candles[candles.size - 2].close -> "اتجاه صاعد في الـ M5"
                latest.close < candles[candles.size - 2].close -> "اتجاه هابط في الـ M5"
                else -> "تذبذب محايد"
            }

            _uiState.value = _uiState.value.copy(
                currentPrice = latest.close,
                candles = candles,
                analysisSummary = analysis,
                status = status
            )
        }
    }

    private fun buildAnalysis(candles: List<Candle>, current: Double): String {
        val previous = candles.takeLast(2)
        val prevClose = previous.firstOrNull()?.close ?: current
        val emaFast = simpleEma(candles.map { it.close }, 5)
        val emaSlow = simpleEma(candles.map { it.close }, 20)
        val direction = when {
            current > emaFast && emaFast > emaSlow -> "الاتجاه صاعد، مع دعم EMA القصير فوق الطويل."
            current < emaFast && emaFast < emaSlow -> "الاتجاه هابط، مع مقاومة EMA القصير تحت الطويل."
            else -> "السوق متذبذب، والاتجاه غير واضح في هذه اللحظة."
        }
        val momentum = if (current >= prevClose) "التقارب صاعد" else "التقارب هابط"
        val diff = abs(current - prevClose)
        return "$direction السعر الحالي ${priceFormat.format(current)}، و$differenceText(diff)، $momentum. يمكن أن تكون الإشارة قوية عند وجود دعم/مقاومة أو نموذج شمعة واضح."
    }

    private fun differenceText(delta: Double): String {
        return if (delta < 0.6) "التغير صغير" else if (delta < 2.0) "التغير معتدل" else "التغير كبير"
    }

    private fun simpleEma(values: List<Double>, period: Int): Double {
        if (values.isEmpty()) return 0.0
        val k = 2.0 / (period + 1.0)
        var ema = values.first()
        for (value in values.drop(1)) {
            ema = (value - ema) * k + ema
        }
        return ema
    }
}

@Composable
fun GoldCandleApp(viewModel: GoldViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF4F4F4)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "محلل الذهب",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${state.symbol} • ${state.timeframe}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("السعر الحالي", style = MaterialTheme.typography.labelMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = priceFormat.format(state.currentPrice),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(12.dp))
                    AssistChip(onClick = { viewModel.refreshMarket() }, label = { Text("تحديث") })
                }
                Text("الحالة: ${state.status}")
                Text("${state.analysisSummary}")
            }
        }

        OutlinedTextField(
            value = state.brokerUrl,
            onValueChange = { viewModel.updateBrokerUrl(it) },
            label = { Text("رابط خادم الوسيط") },
            placeholder = { Text("https://example.com/api") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = { viewModel.refreshMarket() },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("تحليل جديد")
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("سجل الإشارات", style = MaterialTheme.typography.titleLarge)
                if (state.signals.isEmpty()) {
                    Text("لا توجد إشارات حتى الآن", modifier = Modifier.padding(top = 12.dp))
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 260.dp)) {
                        items(state.signals.take(10)) { signal ->
                            Surface(
                                tonalElevation = 1.dp,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = if (signal.side == "BUY") "شراء" else if (signal.side == "SELL") "بيع" else "محايد")
                                        Text(text = signal.model)
                                    }
                                    Text("الدخول: ${priceFormat.format(signal.entry)}")
                                    Text("الوقف: ${priceFormat.format(signal.stop)} | الهدف: ${priceFormat.format(signal.target)}")
                                    Text("الثقة: ${signal.confidence}/100")
                                    Text(signal.reason)
                                    Text(
                                        text = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault())
                                            .format(Instant.ofEpochMilli(signal.createdAt))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
