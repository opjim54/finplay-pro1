package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.CategoryBreakdown
import com.example.domain.CurrencyUtil
import com.example.domain.PeriodComparisonPoint
import com.example.i18n.LocalStrings
import com.example.ui.theme.LocalFinanceColors
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InteractivePieDonutChart(
    items: List<CategoryBreakdown>,
    currencyCode: String,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    var selectedIdx by remember { mutableIntStateOf(-1) }
    var animStarted by remember { mutableStateOf(false) }
    LaunchedEffect(items) { animStarted = true }
    val sweepProgress by animateFloatAsState(
        targetValue = if (animStarted) 1f else 0f,
        animationSpec = tween(650),
        label = "pieSweep"
    )

    if (items.isEmpty()) {
        Text(
            text = strings.noDataYet,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.padding(vertical = 16.dp)
        )
        return
    }

    val total = items.sumOf { it.amount }.coerceAtLeast(1.0)
    val selected = items.getOrNull(selectedIdx) ?: items.first()

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier.size(164.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .size(160.dp)
                        .pointerInput(items) {
                            detectTapGestures { offset ->
                                val cx = size.width / 2f
                                val cy = size.height / 2f
                                val angle = ((Math.toDegrees(
                                    atan2((offset.y - cy).toDouble(), (offset.x - cx).toDouble())
                                ) + 360.0 + 90.0) % 360.0).toFloat()
                                var acc = 0f
                                items.forEachIndexed { idx, item ->
                                    val sweep = ((item.amount / total) * 360.0).toFloat()
                                    if (angle in acc..(acc + sweep)) {
                                        selectedIdx = idx
                                        return@detectTapGestures
                                    }
                                    acc += sweep
                                }
                            }
                        }
                ) {
                    val strokeWidth = 28.dp.toPx()
                    val diameter = min(size.width, size.height) - strokeWidth
                    val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                    val arcSize = Size(diameter, diameter)
                    var startAngle = -90f
                    items.forEachIndexed { idx, item ->
                        val sweep = (((item.amount / total) * 360.0).toFloat()) * sweepProgress
                        val isSel = idx == selectedIdx
                        drawArc(
                            color = Color(item.colorHex),
                            startAngle = startAngle,
                            sweepAngle = max(1f, sweep - 2f),
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(
                                width = if (isSel) strokeWidth + 6.dp.toPx() else strokeWidth,
                                cap = StrokeCap.Butt
                            )
                        )
                        startAngle += sweep
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = selected.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyUtil.formatCompact(selected.amount, currencyCode),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${selected.percentage.roundToInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(selected.colorHex)
                    )
                }
            }

            FlowRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items.take(6).forEachIndexed { idx, item ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (idx == selectedIdx) Color(item.colorHex).copy(alpha = 0.16f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.clickable { selectedIdx = idx }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(item.colorHex))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${item.name} ${item.percentage.roundToInt()}%",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InteractiveBarComparisonChart(
    points: List<PeriodComparisonPoint>,
    currencyCode: String,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val financeColors = LocalFinanceColors.current
    var selectedIdx by remember { mutableIntStateOf(points.lastIndex.coerceAtLeast(0)) }
    var barAnimStarted by remember(points) { mutableStateOf(false) }
    LaunchedEffect(points) { barAnimStarted = true }
    val barProgress by animateFloatAsState(
        targetValue = if (barAnimStarted) 1f else 0f,
        animationSpec = tween(560),
        label = "barProgress"
    )

    if (points.isEmpty()) {
        Text(
            text = strings.noDataYet,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    val maxVal = points.maxOf { max(it.income, it.expense) }.coerceAtLeast(1.0)
    val selected = points.getOrNull(selectedIdx)

    Column(modifier = modifier.fillMaxWidth()) {
        if (selected != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = selected.label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${strings.income}: ${CurrencyUtil.formatCompact(selected.income, currencyCode)}  |  ${strings.expense}: ${
                        CurrencyUtil.formatCompact(selected.expense, currencyCode)
                    }",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .pointerInput(points) {
                    detectTapGestures { offset ->
                        if (points.isNotEmpty()) {
                            val groupWidth = size.width / points.size.toFloat()
                            val idx = (offset.x / groupWidth).toInt().coerceIn(0, points.lastIndex)
                            selectedIdx = idx
                        }
                    }
                }
        ) {
            val count = points.size
            val groupW = size.width / count.toFloat()
            val barW = (groupW * 0.28f).coerceAtMost(24.dp.toPx())
            val chartH = size.height - 8.dp.toPx()

            // Subtle baseline
            drawLine(
                color = financeColors.cardBorder,
                start = Offset(0f, chartH),
                end = Offset(size.width, chartH),
                strokeWidth = 1.dp.toPx()
            )

            points.forEachIndexed { idx, p ->
                val centerX = idx * groupW + groupW / 2f
                val incH = (((p.income / maxVal) * (chartH - 10.dp.toPx())).toFloat() * barProgress).coerceAtLeast(3f)
                val expH = (((p.expense / maxVal) * (chartH - 10.dp.toPx())).toFloat() * barProgress).coerceAtLeast(3f)

                if (idx == selectedIdx) {
                    drawRoundRect(
                        color = financeColors.income.copy(alpha = 0.08f),
                        topLeft = Offset(idx * groupW + 4f, 0f),
                        size = Size(groupW - 8f, chartH),
                        cornerRadius = CornerRadius(10.dp.toPx())
                    )
                }

                drawRoundRect(
                    color = financeColors.income,
                    topLeft = Offset(centerX - barW - 2.dp.toPx(), chartH - incH),
                    size = Size(barW, incH),
                    cornerRadius = CornerRadius(5.dp.toPx())
                )
                drawRoundRect(
                    color = financeColors.expense,
                    topLeft = Offset(centerX + 2.dp.toPx(), chartH - expH),
                    size = Size(barW, expH),
                    cornerRadius = CornerRadius(5.dp.toPx())
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            points.forEachIndexed { idx, p ->
                Text(
                    text = p.label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (idx == selectedIdx) FontWeight.Bold else FontWeight.Normal,
                    color = if (idx == selectedIdx) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { selectedIdx = idx }
                )
            }
        }
    }
}

@Composable
fun InteractiveLineAreaChart(
    points: List<PeriodComparisonPoint>,
    currencyCode: String,
    showArea: Boolean = true,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val financeColors = LocalFinanceColors.current
    var selectedIdx by remember { mutableIntStateOf(points.lastIndex.coerceAtLeast(0)) }

    if (points.isEmpty()) {
        Text(
            text = strings.noDataYet,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    val maxVal = points.maxOf { max(it.income, max(it.expense, it.savings)) }.coerceAtLeast(1.0)
    val selected = points.getOrNull(selectedIdx)

    Column(modifier = modifier.fillMaxWidth()) {
        if (selected != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = selected.label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${strings.income}: ${CurrencyUtil.formatCompact(selected.income, currencyCode)} | ${strings.expense}: ${
                        CurrencyUtil.formatCompact(selected.expense, currencyCode)
                    }",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .pointerInput(points) {
                    detectTapGestures { offset ->
                        if (points.size > 1) {
                            val step = size.width / (points.size - 1).toFloat()
                            selectedIdx = (offset.x / step).roundToInt().coerceIn(0, points.lastIndex)
                        }
                    }
                }
        ) {
            val chartH = size.height - 16.dp.toPx()
            val padTop = 10.dp.toPx()
            val stepX = if (points.size > 1) size.width / (points.size - 1).toFloat() else size.width

            val incomePts = points.mapIndexed { idx, p ->
                val x = if (points.size == 1) size.width / 2f else idx * stepX
                val y = padTop + (chartH - padTop) * (1f - (p.income / maxVal).toFloat())
                Offset(x, y)
            }
            val expensePts = points.mapIndexed { idx, p ->
                val x = if (points.size == 1) size.width / 2f else idx * stepX
                val y = padTop + (chartH - padTop) * (1f - (p.expense / maxVal).toFloat())
                Offset(x, y)
            }

            if (showArea && incomePts.size >= 2) {
                val incArea = Path().apply {
                    moveTo(incomePts.first().x, chartH)
                    incomePts.forEach { lineTo(it.x, it.y) }
                    lineTo(incomePts.last().x, chartH)
                    close()
                }
                drawPath(
                    path = incArea,
                    brush = Brush.verticalGradient(
                        listOf(financeColors.income.copy(alpha = 0.28f), Color.Transparent)
                    )
                )

                val expArea = Path().apply {
                    moveTo(expensePts.first().x, chartH)
                    expensePts.forEach { lineTo(it.x, it.y) }
                    lineTo(expensePts.last().x, chartH)
                    close()
                }
                drawPath(
                    path = expArea,
                    brush = Brush.verticalGradient(
                        listOf(financeColors.expense.copy(alpha = 0.20f), Color.Transparent)
                    )
                )
            }

            if (incomePts.size >= 2) {
                val incPath = Path().apply {
                    moveTo(incomePts.first().x, incomePts.first().y)
                    incomePts.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(incPath, color = financeColors.income, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))

                val expPath = Path().apply {
                    moveTo(expensePts.first().x, expensePts.first().y)
                    expensePts.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(expPath, color = financeColors.expense, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
            }

            incomePts.forEachIndexed { idx, pt ->
                drawCircle(
                    color = financeColors.income,
                    radius = if (idx == selectedIdx) 6.dp.toPx() else 3.5.dp.toPx(),
                    center = pt
                )
            }
            expensePts.forEachIndexed { idx, pt ->
                drawCircle(
                    color = financeColors.expense,
                    radius = if (idx == selectedIdx) 6.dp.toPx() else 3.5.dp.toPx(),
                    center = pt
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            points.forEachIndexed { idx, p ->
                Text(
                    text = p.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (idx == selectedIdx) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { selectedIdx = idx }
                )
            }
        }
    }
}

@Composable
fun CategoryComparisonBars(
    items: List<CategoryBreakdown>,
    currencyCode: String,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    if (items.isEmpty()) {
        Text(
            text = strings.noDataYet,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    val maxVal = items.maxOf { it.amount }.coerceAtLeast(1.0)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items.take(8).forEach { item ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${CurrencyUtil.format(item.amount, currencyCode)} (${item.percentage.roundToInt()}%)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                LinearProgressIndicator(
                    progress = { (item.amount / maxVal).toFloat().coerceIn(0.02f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Color(item.colorHex),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

@Composable
fun Interactive30DayCashflowChart(
    points: List<PeriodComparisonPoint>,
    currencyCode: String,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val financeColors = LocalFinanceColors.current
    var selectedIdx by remember(points) { mutableIntStateOf(points.lastIndex.coerceAtLeast(0)) }
    var cashAnimStarted by remember(points) { mutableStateOf(false) }
    LaunchedEffect(points) { cashAnimStarted = true }
    val cashProgress by animateFloatAsState(
        targetValue = if (cashAnimStarted) 1f else 0f,
        animationSpec = tween(580),
        label = "cashflowProgress"
    )

    if (points.isEmpty()) {
        Text(
            text = strings.noDataYet,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    val maxVal = points.maxOf { max(it.income, it.expense) }.coerceAtLeast(1.0)
    val selected = points.getOrNull(selectedIdx)

    Column(modifier = modifier.fillMaxWidth()) {
        if (selected != null) {
            val net = selected.income - selected.expense
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${strings.date}: ${selected.label}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "+${CurrencyUtil.formatCompact(selected.income, currencyCode)}  •  -${CurrencyUtil.formatCompact(selected.expense, currencyCode)}  (${if (net >= 0) "+" else ""}${CurrencyUtil.formatCompact(net, currencyCode)})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (net >= 0) financeColors.income else financeColors.expense
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(175.dp)
                .pointerInput(points) {
                    detectTapGestures { offset ->
                        if (points.isNotEmpty()) {
                            val groupW = size.width / points.size.toFloat()
                            selectedIdx = (offset.x / groupW).toInt().coerceIn(0, points.lastIndex)
                        }
                    }
                }
        ) {
            val count = points.size
            val groupW = size.width / count.toFloat()
            val barW = (groupW * 0.45f).coerceAtLeast(3.dp.toPx()).coerceAtMost(12.dp.toPx())
            val chartH = size.height - 12.dp.toPx()
            val padTop = 8.dp.toPx()

            drawLine(
                color = financeColors.cardBorder,
                start = Offset(0f, chartH),
                end = Offset(size.width, chartH),
                strokeWidth = 1.dp.toPx()
            )

            val netPts = mutableListOf<Offset>()
            points.forEachIndexed { idx, p ->
                val cx = idx * groupW + groupW / 2f
                val incH = (((p.income / maxVal) * (chartH - padTop)).toFloat() * cashProgress).coerceAtLeast(2f)
                val expH = (((p.expense / maxVal) * (chartH - padTop)).toFloat() * cashProgress).coerceAtLeast(2f)

                if (idx == selectedIdx) {
                    drawRoundRect(
                        color = financeColors.income.copy(alpha = 0.12f),
                        topLeft = Offset(idx * groupW, 0f),
                        size = Size(groupW, chartH),
                        cornerRadius = CornerRadius(6.dp.toPx())
                    )
                }

                if (p.income > 0.0) {
                    drawRoundRect(
                        color = financeColors.income.copy(alpha = 0.85f),
                        topLeft = Offset(cx - barW / 2f, chartH - incH),
                        size = Size(barW, incH),
                        cornerRadius = CornerRadius(3.dp.toPx())
                    )
                }
                if (p.expense > 0.0) {
                    drawRoundRect(
                        color = financeColors.expense.copy(alpha = 0.78f),
                        topLeft = Offset(cx - barW / 2f + 1.5.dp.toPx(), chartH - expH),
                        size = Size(barW, expH),
                        cornerRadius = CornerRadius(3.dp.toPx())
                    )
                }

                val netY = padTop + (chartH - padTop) * (1f - (max(p.income, p.expense) / maxVal).toFloat())
                netPts.add(Offset(cx, netY))
            }

            if (netPts.size >= 2) {
                val path = Path().apply {
                    moveTo(netPts.first().x, netPts.first().y)
                    netPts.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(
                    path = path,
                    color = financeColors.savings.copy(alpha = 0.75f),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        val labelIndices = listOf(0, points.size / 4, points.size / 2, (points.size * 3) / 4, points.lastIndex)
            .distinct()
            .filter { it in points.indices }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            labelIndices.forEach { idx ->
                Text(
                    text = points[idx].label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (idx == selectedIdx) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { selectedIdx = idx }
                )
            }
        }
    }
}

