package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.GestureCategory
import com.example.data.GestureEntity
import com.example.ui.GestureUiState
import com.example.ui.components.GestureCard
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldActive

@Composable
fun GesturesHubScreen(
    state: GestureUiState,
    onSelectCategory: (GestureCategory) -> Unit,
    onSearchChange: (String) -> Unit,
    onToggleGesture: (String, Boolean) -> Unit,
    onEditGesture: (GestureEntity) -> Unit,
    onTestGesture: (GestureEntity) -> Unit,
    onEnableAll: () -> Unit,
    onOpenOverlayPad: () -> Unit,
    onOpenLauncherSetup: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWide = maxWidth >= 700.dp

        if (isWide) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item(span = { GridItemSpan(2) }) {
                    HeroAndFiltersSection(
                        state = state,
                        onSelectCategory = onSelectCategory,
                        onSearchChange = onSearchChange,
                        onEnableAll = onEnableAll,
                        onOpenOverlayPad = onOpenOverlayPad,
                        onOpenLauncherSetup = onOpenLauncherSetup
                    )
                }

                items(state.filteredGestures, key = { it.id }) { gesture ->
                    GestureCard(
                        gesture = gesture,
                        onToggle = { onToggleGesture(gesture.id, it) },
                        onEditAction = { onEditGesture(gesture) },
                        onTestExecute = { onTestGesture(gesture) }
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 600.dp)
            ) {
                item {
                    HeroAndFiltersSection(
                        state = state,
                        onSelectCategory = onSelectCategory,
                        onSearchChange = onSearchChange,
                        onEnableAll = onEnableAll,
                        onOpenOverlayPad = onOpenOverlayPad,
                        onOpenLauncherSetup = onOpenLauncherSetup
                    )
                }

                if (state.filteredGestures.isEmpty()) {
                    item {
                        EmptySearchPlaceholder(onReset = {
                            onSearchChange("")
                            onSelectCategory(GestureCategory.ALL)
                        })
                    }
                } else {
                    items(state.filteredGestures, key = { it.id }) { gesture ->
                        GestureCard(
                            gesture = gesture,
                            onToggle = { onToggleGesture(gesture.id, it) },
                            onEditAction = { onEditGesture(gesture) },
                            onTestExecute = { onTestGesture(gesture) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroAndFiltersSection(
    state: GestureUiState,
    onSelectCategory: (GestureCategory) -> Unit,
    onSearchChange: (String) -> Unit,
    onEnableAll: () -> Unit,
    onOpenOverlayPad: () -> Unit,
    onOpenLauncherSetup: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Hero Banner Card with generated kinetic artwork
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(216.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_banner),
                    contentDescription = "خلفية إيماءات اللمس متعددة الأصابع",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xAA070B14),
                                    Color(0x88070B14),
                                    Color(0xEB070B14)
                                )
                            )
                        )
                )

                Column(
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Service live status pill
                        val active = state.isAccessibilityEnabled
                        Surface(
                            shape = CircleShape,
                            color = if (active) EmeraldActive.copy(alpha = 0.22f)
                            else AmberWarning.copy(alpha = 0.22f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (active) EmeraldActive else AmberWarning)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (active) "يعمل فوق اللانشر المخصص" else "الخدمة بانتظار التفعيل",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = "${state.enabledCount} / ${state.totalCount} مفعّلة",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "محرك الإيماءات الفوري",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White
                        )
                        Text(
                            text = "اضغط 3 أصابع مرتين ليوتيوب، 3 أصابع مرة لكلاود، أو خصص أكثر من 24 إيماءة واختصار",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFD6E4FF),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (state.isAccessibilityEnabled) {
                                Button(
                                    onClick = onOpenOverlayPad,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                    modifier = Modifier.testTag("hero_launch_pad_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Layers,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("إظهار لوحة الإيماءات")
                                }
                            } else {
                                Button(
                                    onClick = onOpenLauncherSetup,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                    modifier = Modifier.testTag("hero_activate_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Settings,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("تفعيل فوق اللانشر")
                                }
                            }

                            OutlinedButton(
                                onClick = onEnableAll,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                modifier = Modifier.testTag("enable_all_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DoneAll,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تفعيل الكل")
                            }
                        }
                    }
                }
            }
        }

        // Search Bar
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("ابحث عن إيماءة أو تطبيق (مثل: يوتيوب، كلاود، 3 أصابع...)") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "بحث") },
            trailingIcon = {
                if (state.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Rounded.Close, contentDescription = "مسح البحث")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("gestures_search_field")
        )

        // Category Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(GestureCategory.entries) { category ->
                val count = if (category == GestureCategory.ALL) {
                    state.allGestures.size
                } else {
                    state.allGestures.count { it.category == category.name }
                }
                val selected = state.selectedCategory == category
                FilterChip(
                    selected = selected,
                    onClick = { onSelectCategory(category) },
                    label = { Text("${category.titleAr} ($count)") },
                    leadingIcon = if (selected) {
                        {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.testTag("category_chip_${category.name}")
                )
            }
        }
    }
}

@Composable
private fun EmptySearchPlaceholder(onReset: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(28.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.TouchApp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "لا توجد إيماءة مطابقة لبحثك",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = onReset) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("عرض جميع الـ 24 إيماءة")
            }
        }
    }
}
