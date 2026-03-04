package com.jewish.calendar.ui.screens.tools

import android.Manifest
import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.jewish.calendar.data.OverpassElement
import com.jewish.calendar.data.SynagogueRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@SuppressLint("MissingPermission")
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SynagogueMapScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locationPermission = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val repository = remember { SynagogueRepository() }

    // Default to Jerusalem center
    val defaultPosition = LatLng(31.7683, 35.2137)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultPosition, 14f)
    }

    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var synagogues by remember { mutableStateOf<List<OverpassElement>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedSynagogue by remember { mutableStateOf<OverpassElement?>(null) }
    var showList by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    fun loadSynagogues(lat: Double, lon: Double) {
        scope.launch {
            isLoading = true
            errorMsg = null
            val results = repository.findSynagoguesNearby(lat, lon, radiusMeters = 3000)
            synagogues = results
            isLoading = false
            if (results.isEmpty()) errorMsg = "לא נמצאו בתי כנסת בטווח 3 ק\"מ"
        }
    }

    // Load location and synagogues when permission is granted
    LaunchedEffect(locationPermission.status.isGranted) {
        if (locationPermission.status.isGranted) {
            try {
                val location = fusedLocationClient.lastLocation.await()
                if (location != null) {
                    val pos = LatLng(location.latitude, location.longitude)
                    userLocation = pos
                    cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(pos, 14f))
                    loadSynagogues(location.latitude, location.longitude)
                } else {
                    loadSynagogues(defaultPosition.latitude, defaultPosition.longitude)
                }
            } catch (_: Exception) {
                loadSynagogues(defaultPosition.latitude, defaultPosition.longitude)
            }
        } else {
            locationPermission.launchPermissionRequest()
            loadSynagogues(defaultPosition.latitude, defaultPosition.longitude)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "🕍 בתי כנסת בסביבתי",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "חזור")
                    }
                },
                actions = {
                    // Toggle list/map view
                    IconButton(onClick = { showList = !showList }) {
                        Icon(
                            if (showList) Icons.Default.Map else Icons.Default.List,
                            contentDescription = if (showList) "מפה" else "רשימה"
                        )
                    }
                    // Refresh
                    IconButton(onClick = {
                        val pos = userLocation ?: defaultPosition
                        loadSynagogues(pos.latitude, pos.longitude)
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "רענן")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (showList) {
                // ── List view ──────────────────────────────────────────
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (synagogues.isEmpty() && !isLoading) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    errorMsg ?: "טוען...",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    items(synagogues) { syn ->
                        SynagogueListItem(
                            synagogue = syn,
                            isSelected = selectedSynagogue?.id == syn.id,
                            onClick = {
                                selectedSynagogue = syn
                                showList = false
                                scope.launch {
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newLatLngZoom(LatLng(syn.lat, syn.lon), 16f)
                                    )
                                }
                            }
                        )
                    }
                }
            } else {
                // ── Map view ───────────────────────────────────────────
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = true,
                        myLocationButtonEnabled = locationPermission.status.isGranted
                    ),
                    properties = MapProperties(
                        isMyLocationEnabled = locationPermission.status.isGranted
                    )
                ) {
                    // Synagogue markers
                    synagogues.forEach { syn ->
                        val pos = LatLng(syn.lat, syn.lon)
                        Marker(
                            state = MarkerState(position = pos),
                            title = syn.name,
                            snippet = syn.address.ifBlank { null },
                            icon = BitmapDescriptorFactory.defaultMarker(
                                if (selectedSynagogue?.id == syn.id)
                                    BitmapDescriptorFactory.HUE_GOLD
                                else
                                    BitmapDescriptorFactory.HUE_AZURE
                            ),
                            onClick = {
                                selectedSynagogue = syn
                                false // let default info window show
                            }
                        )
                    }

                    // User location marker
                    userLocation?.let { pos ->
                        Circle(
                            center = pos,
                            radius = 50.0,
                            fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            strokeColor = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3f
                        )
                    }
                }

                // Synagogue count badge
                if (synagogues.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shadowElevation = 4.dp
                    ) {
                        Text(
                            text = "נמצאו ${synagogues.size} בתי כנסת",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Selected synagogue info card
                selectedSynagogue?.let { syn ->
                    Card(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🕍", fontSize = 22.sp)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = syn.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (syn.address.isNotBlank()) {
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = syn.address,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(onClick = { selectedSynagogue = null }) {
                                Icon(Icons.Default.Close, contentDescription = "סגור")
                            }
                        }
                    }
                }
            }

            // Loading indicator
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            // Error message
            errorMsg?.let { msg ->
                if (!isLoading) {
                    Snackbar(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                    ) {
                        Text(msg)
                    }
                }
            }
        }
    }
}

@Composable
private fun SynagogueListItem(
    synagogue: OverpassElement,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🕍", fontSize = 24.sp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = synagogue.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                if (synagogue.address.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = synagogue.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (isSelected) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
