package com.bazaarhelper.app.presentation.screens.home

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.bazaarhelper.app.R
import com.bazaarhelper.app.presentation.components.*

@Composable
fun HomeScreen(
    onAddClick: () -> Unit,
    onReportsClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val colors = MaterialTheme.colorScheme
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            BazaarTopBar(
                title = "Bazaar Helper",
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.Language, contentDescription = stringResource(R.string.language))
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("English") },
                                onClick = {
                                    showMenu = false
                                    val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags("en")
                                    AppCompatDelegate.setApplicationLocales(appLocale)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("العربية") },
                                onClick = {
                                    showMenu = false
                                    val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags("ar")
                                    AppCompatDelegate.setApplicationLocales(appLocale)
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        AnimatedContent(
            targetState = state.isLoading,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            label = "home_content"
        ) { isLoading ->
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val today = state.todayRecord
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    colors.primaryContainer.copy(alpha = 0.2f),
                                    colors.background
                                )
                            )
                        )
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Centered Date Section
                    Surface(
                        color = colors.secondaryContainer.copy(alpha = 0.5f),
                        shape = CircleShape
                    ) {
                        Text(
                            text = java.time.LocalDate.now().formatArabic(),
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.onSecondaryContainer
                        )
                    }

                    // Main Summary Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        HomeStatCard(
                            title = stringResource(R.string.sales),
                            value = (today?.sales ?: 0.0).formatCurrency(),
                            icon = Icons.AutoMirrored.Filled.TrendingUp,
                            color = Color(0xFF4CAF50),
                            modifier = Modifier.weight(1f)
                        )
                        HomeStatCard(
                            title = stringResource(R.string.purchases),
                            value = (today?.purchases ?: 0.0).formatCurrency(),
                            icon = Icons.AutoMirrored.Filled.TrendingDown,
                            color = Color(0xFFF44336),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Profit Card (Premium look)
                    val profit = today?.profit ?: 0.0
                    val isProfit = profit >= 0
                    val gradient = if (isProfit) {
                        Brush.linearGradient(listOf(Color(0xFF6200EE), Color(0xFF7C4DFF)))
                    } else {
                        Brush.linearGradient(listOf(Color(0xFFD32F2F), Color(0xFFEF5350)))
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraLarge,
                        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(gradient)
                                .fillMaxWidth()
                                .padding(vertical = 40.dp, horizontal = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (isProfit) stringResource(R.string.total_profit_today) else stringResource(R.string.total_loss_today),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = profit.formatCurrency(),
                                    fontSize = 42.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    if (today == null) {
                        Card(
                            onClick = onAddClick,
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = colors.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, colors.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = colors.primary.copy(alpha = 0.1f),
                                    shape = CircleShape,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = colors.primary)
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = stringResource(R.string.no_data_yet),
                                    fontSize = 14.sp,
                                    color = colors.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Quick Actions
                    Text(
                        text = stringResource(R.string.quick_actions),
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ActionCard(
                            text = if (today == null) stringResource(R.string.add_daily) else stringResource(R.string.edit_today),
                            icon = Icons.Default.Add,
                            color = colors.primary,
                            onClick = onAddClick,
                            modifier = Modifier.weight(1f)
                        )
                        ActionCard(
                            text = stringResource(R.string.records),
                            icon = Icons.Default.Assessment,
                            color = colors.secondary,
                            onClick = onReportsClick,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ActionCard(
    text: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(100.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                color = color.copy(alpha = 0.1f),
                shape = CircleShape,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
fun HomeStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
