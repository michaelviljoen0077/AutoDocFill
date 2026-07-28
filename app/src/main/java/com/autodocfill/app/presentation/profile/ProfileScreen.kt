package com.autodocfill.app.presentation.profile

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.autodocfill.app.data.model.Profile

/**
 * Modern Profile list screen with Material Design 3
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileListScreen(
    navController: NavController,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val profiles by viewModel.profiles.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val activeProfile by viewModel.activeProfile.collectAsState()
    
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingProfile by remember { mutableStateOf<Profile?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Profiles", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "${profiles.size} profile${if (profiles.size != 1) "s" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                icon = { Icon(Icons.Filled.Add, "Create") },
                text = { Text("New Profile") },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                profiles.isEmpty() -> {
                    EmptyProfileState(
                        onCreateClick = { showCreateDialog = true },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(profiles) { profile ->
                            ModernProfileCard(
                                profile = profile,
                                isActive = profile.id == activeProfile?.id,
                                onEditClick = { editingProfile = profile },
                                onDeleteClick = { viewModel.deleteProfile(profile) },
                                onSetActiveClick = { viewModel.setActiveProfile(profile) }
                            )
                        }
                        
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
            
            // Success/Error messages
            uiState.successMessage?.let { message ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(message)
                }
            }
            
            uiState.errorMessage?.let { message ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(message, color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }
    }
    
    if (showCreateDialog || editingProfile != null) {
        ModernProfileFormDialog(
            profile = editingProfile,
            onDismiss = {
                showCreateDialog = false
                editingProfile = null
            },
            onSave = { profile ->
                if (editingProfile != null) {
                    viewModel.updateProfile(profile)
                } else {
                    viewModel.createProfile(profile)
                }
                showCreateDialog = false
                editingProfile = null
            }
        )
    }
}

@Composable
fun EmptyProfileState(
    onCreateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Outlined.AccountCircle,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "No profiles yet",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Create your first profile to start\nautofilling forms in seconds",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        FilledTonalButton(
            onClick = onCreateClick,
            modifier = Modifier.height(56.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(12.dp))
            Text("Create Profile", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernProfileCard(
    profile: Profile,
    isActive: Boolean,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onSetActiveClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = if (isActive) {
            CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        } else {
            CardDefaults.elevatedCardColors()
        },
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = if (isActive) 4.dp else 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        color = if (isActive) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (isActive) Icons.Filled.Person else Icons.Outlined.Person,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = if (isActive) 
                                    MaterialTheme.colorScheme.onPrimary 
                                else 
                                    MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                    
                    Column(modifier = Modifier.weight(1f)) {
                        if (isActive) {
                            AssistChip(
                                onClick = { },
                                label = { 
                                    Text(
                                        "ACTIVE",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    ) 
                                },
                                leadingIcon = { 
                                    Icon(
                                        Icons.Filled.CheckCircle, 
                                        null, 
                                        modifier = Modifier.size(14.dp)
                                    ) 
                                },
                                modifier = Modifier.height(24.dp),
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    labelColor = MaterialTheme.colorScheme.onPrimary,
                                    leadingIconContentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        
                        Text(
                            profile.profileName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "${profile.firstName} ${profile.lastName}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Filled.MoreVert, "Options")
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        if (!isActive) {
                            DropdownMenuItem(
                                text = { Text("Set as Active") },
                                onClick = {
                                    onSetActiveClick()
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Filled.CheckCircle, null) }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = {
                                onEditClick()
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Filled.Edit, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                onDeleteClick()
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Filled.Delete, null) },
                            colors = MenuDefaults.itemColors(
                                textColor = MaterialTheme.colorScheme.error,
                                leadingIconColor = MaterialTheme.colorScheme.error
                            )
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Profile details
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (profile.email.isNotEmpty()) {
                    ProfileDetailRow(
                        icon = Icons.Outlined.Email,
                        label = "Email",
                        value = profile.email
                    )
                }
                if (profile.phoneNumber.isNotEmpty()) {
                    ProfileDetailRow(
                        icon = Icons.Outlined.Phone,
                        label = "Phone",
                        value = "${profile.cellphoneCountryCode} ${profile.phoneNumber}".trim()
                    )
                }
                if (profile.company.isNotEmpty()) {
                    ProfileDetailRow(
                        icon = Icons.Outlined.Business,
                        label = "Company",
                        value = "${profile.company}${if (profile.jobTitle.isNotEmpty()) " • ${profile.jobTitle}" else ""}"
                    )
                }
                if (profile.city.isNotEmpty()) {
                    ProfileDetailRow(
                        icon = Icons.Outlined.LocationOn,
                        label = "Location",
                        value = "${profile.city}${if (profile.state.isNotEmpty()) ", ${profile.state}" else ""}"
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Column {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernProfileFormDialog(
    profile: Profile?,
    onDismiss: () -> Unit,
    onSave: (Profile) -> Unit
) {
    var currentSection by remember { mutableStateOf(0) }
    var profileName by remember { mutableStateOf(profile?.profileName ?: "My Profile") }
    
    // Personal Info
    var firstName by remember { mutableStateOf(profile?.firstName ?: "") }
    var middleName by remember { mutableStateOf(profile?.middleName ?: "") }
    var lastName by remember { mutableStateOf(profile?.lastName ?: "") }
    var dateOfBirth by remember { mutableStateOf(profile?.dateOfBirth ?: "") }
    var gender by remember { mutableStateOf(profile?.gender ?: "") }
    var nationality by remember { mutableStateOf(profile?.nationality ?: "") }
    
    // Contact Info
    var email by remember { mutableStateOf(profile?.email ?: "") }
    var phoneNumber by remember { mutableStateOf(profile?.phoneNumber ?: "") }
    var countryCode by remember { mutableStateOf(profile?.cellphoneCountryCode ?: "+1") }
    var alternatePhone by remember { mutableStateOf(profile?.alternatePhone ?: "") }
    var altCountryCode by remember { mutableStateOf(profile?.alternatePhoneCountryCode ?: "+1") }
    
    // Residential Address
    var addressLine1 by remember { mutableStateOf(profile?.addressLine1 ?: "") }
    var addressLine2 by remember { mutableStateOf(profile?.addressLine2 ?: "") }
    var city by remember { mutableStateOf(profile?.city ?: "") }
    var state by remember { mutableStateOf(profile?.state ?: "") }
    var zipCode by remember { mutableStateOf(profile?.zipCode ?: "") }
    var country by remember { mutableStateOf(profile?.country ?: "") }
    
    // Postal Address
    var postalAddressLine1 by remember { mutableStateOf(profile?.postalAddressLine1 ?: "") }
    var postalAddressLine2 by remember { mutableStateOf(profile?.postalAddressLine2 ?: "") }
    var postalCity by remember { mutableStateOf(profile?.postalCity ?: "") }
    var postalState by remember { mutableStateOf(profile?.postalState ?: "") }
    var postalZipCode by remember { mutableStateOf(profile?.postalZipCode ?: "") }
    var postalCountry by remember { mutableStateOf(profile?.postalCountry ?: "") }
    
    // Employment
    var company by remember { mutableStateOf(profile?.company ?: "") }
    var jobTitle by remember { mutableStateOf(profile?.jobTitle ?: "") }
    var occupation by remember { mutableStateOf(profile?.occupation ?: "") }
    var employmentStatus by remember { mutableStateOf(profile?.employmentStatus ?: "") }
    var workAddressLine1 by remember { mutableStateOf(profile?.workAddressLine1 ?: "") }
    var workAddressLine2 by remember { mutableStateOf(profile?.workAddressLine2 ?: "") }
    var workCity by remember { mutableStateOf(profile?.workCity ?: "") }
    var workState by remember { mutableStateOf(profile?.workState ?: "") }
    var workZipCode by remember { mutableStateOf(profile?.workZipCode ?: "") }
    var workCountry by remember { mutableStateOf(profile?.workCountry ?: "") }
    var workPhone by remember { mutableStateOf(profile?.workPhone ?: "") }
    var workPhoneCountryCode by remember { mutableStateOf(profile?.workPhoneCountryCode ?: "+1") }
    var workEmail by remember { mutableStateOf(profile?.workEmail ?: "") }
    var annualIncome by remember { mutableStateOf(profile?.annualIncome ?: "") }
    var startDate by remember { mutableStateOf(profile?.startDate ?: "") }
    
    // Identification
    var idNumber by remember { mutableStateOf(profile?.idNumber ?: "") }
    var passportNumber by remember { mutableStateOf(profile?.passportNumber ?: "") }
    var driverLicenseNumber by remember { mutableStateOf(profile?.driverLicenseNumber ?: "") }
    var taxNumber by remember { mutableStateOf(profile?.taxNumber ?: "") }
    
    // Custom Fields
    var customFields by remember { mutableStateOf(profile?.customFields?.toMutableMap() ?: mutableMapOf()) }
    var showAddCustomField by remember { mutableStateOf(false) }
    
    val sections = listOf("Basic", "Contact", "Address", "Postal", "Work", "IDs", "Custom")
    
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .fillMaxHeight(0.9f)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Header
                Text(
                    if (profile != null) "Edit Profile" else "Create New Profile",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Fill in your information to autofill forms",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Section tabs
                ScrollableTabRow(
                    selectedTabIndex = currentSection,
                    edgePadding = 0.dp
                ) {
                    sections.forEachIndexed { index, section ->
                        Tab(
                            selected = currentSection == index,
                            onClick = { currentSection = index },
                            text = { Text(section) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Form content
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (currentSection) {
                        0 -> { // Basic Info
                            item {
                                OutlinedTextField(
                                    value = profileName,
                                    onValueChange = { profileName = it },
                                    label = { Text("Profile Name *") },
                                    leadingIcon = { Icon(Icons.Outlined.Badge, null) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                            item {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = firstName,
                                        onValueChange = { firstName = it },
                                        label = { Text("First Name *") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = middleName,
                                        onValueChange = { middleName = it },
                                        label = { Text("Middle") },
                                        modifier = Modifier.weight(0.7f),
                                        singleLine = true
                                    )
                                }
                            }
                            item {
                                OutlinedTextField(
                                    value = lastName,
                                    onValueChange = { lastName = it },
                                    label = { Text("Last Name *") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                            item {
                                OutlinedTextField(
                                    value = dateOfBirth,
                                    onValueChange = { dateOfBirth = it },
                                    label = { Text("Date of Birth (YYYY-MM-DD)") },
                                    leadingIcon = { Icon(Icons.Outlined.CalendarToday, null) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    placeholder = { Text("1990-01-15") }
                                )
                            }
                            item {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = gender,
                                        onValueChange = { gender = it },
                                        label = { Text("Gender") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = nationality,
                                        onValueChange = { nationality = it },
                                        label = { Text("Nationality") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }
                            }
                        }
                        
                        1 -> { // Contact Info
                            item {
                                OutlinedTextField(
                                    value = email,
                                    onValueChange = { email = it },
                                    label = { Text("Email") },
                                    leadingIcon = { Icon(Icons.Outlined.Email, null) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                            item {
                                Text(
                                    "Primary Phone",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            item {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = countryCode,
                                        onValueChange = { countryCode = it },
                                        label = { Text("Code") },
                                        modifier = Modifier.weight(0.3f),
                                        singleLine = true,
                                        placeholder = { Text("+1") }
                                    )
                                    OutlinedTextField(
                                        value = phoneNumber,
                                        onValueChange = { phoneNumber = it },
                                        label = { Text("Phone Number") },
                                        leadingIcon = { Icon(Icons.Outlined.Phone, null) },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }
                            }
                            item {
                                Text(
                                    "Alternate Phone (Optional)",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            item {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = altCountryCode,
                                        onValueChange = { altCountryCode = it },
                                        label = { Text("Code") },
                                        modifier = Modifier.weight(0.3f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = alternatePhone,
                                        onValueChange = { alternatePhone = it },
                                        label = { Text("Alternate Phone") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }
                            }
                        }
                        
                        2 -> { // Residential Address
                            item {
                                Text(
                                    "Residential Address",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            item {
                                OutlinedTextField(
                                    value = addressLine1,
                                    onValueChange = { addressLine1 = it },
                                    label = { Text("Address Line 1") },
                                    leadingIcon = { Icon(Icons.Outlined.Home, null) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                            item {
                                OutlinedTextField(
                                    value = addressLine2,
                                    onValueChange = { addressLine2 = it },
                                    label = { Text("Address Line 2 (Apt, Suite)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                            item {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = city,
                                        onValueChange = { city = it },
                                        label = { Text("City") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = state,
                                        onValueChange = { state = it },
                                        label = { Text("State") },
                                        modifier = Modifier.weight(0.7f),
                                        singleLine = true
                                    )
                                }
                            }
                            item {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = zipCode,
                                        onValueChange = { zipCode = it },
                                        label = { Text("ZIP Code") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = country,
                                        onValueChange = { country = it },
                                        label = { Text("Country") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }
                            }
                        }
                        
                        3 -> { // Postal Address
                            item {
                                Text(
                                    "Postal/Mailing Address",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "Leave blank if same as residential",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            item {
                                OutlinedTextField(
                                    value = postalAddressLine1,
                                    onValueChange = { postalAddressLine1 = it },
                                    label = { Text("Address Line 1") },
                                    leadingIcon = { Icon(Icons.Outlined.Mail, null) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                            item {
                                OutlinedTextField(
                                    value = postalAddressLine2,
                                    onValueChange = { postalAddressLine2 = it },
                                    label = { Text("Address Line 2") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                            item {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = postalCity,
                                        onValueChange = { postalCity = it },
                                        label = { Text("City") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = postalState,
                                        onValueChange = { postalState = it },
                                        label = { Text("State") },
                                        modifier = Modifier.weight(0.7f),
                                        singleLine = true
                                    )
                                }
                            }
                            item {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = postalZipCode,
                                        onValueChange = { postalZipCode = it },
                                        label = { Text("ZIP Code") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = postalCountry,
                                        onValueChange = { postalCountry = it },
                                        label = { Text("Country") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }
                            }
                        }
                        
                        4 -> { // Employment
                            item {
                                Text(
                                    "Employment Information",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            item {
                                OutlinedTextField(
                                    value = company,
                                    onValueChange = { company = it },
                                    label = { Text("Company Name") },
                                    leadingIcon = { Icon(Icons.Outlined.Business, null) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                            item {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = jobTitle,
                                        onValueChange = { jobTitle = it },
                                        label = { Text("Job Title") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = occupation,
                                        onValueChange = { occupation = it },
                                        label = { Text("Occupation") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }
                            }
                            item {
                                OutlinedTextField(
                                    value = employmentStatus,
                                    onValueChange = { employmentStatus = it },
                                    label = { Text("Employment Status") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    placeholder = { Text("Full-time, Part-time, Self-employed, etc.") }
                                )
                            }
                            item {
                                Text(
                                    "Work Contact",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            item {
                                OutlinedTextField(
                                    value = workEmail,
                                    onValueChange = { workEmail = it },
                                    label = { Text("Work Email") },
                                    leadingIcon = { Icon(Icons.Outlined.Email, null) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                            item {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = workPhoneCountryCode,
                                        onValueChange = { workPhoneCountryCode = it },
                                        label = { Text("Code") },
                                        modifier = Modifier.weight(0.3f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = workPhone,
                                        onValueChange = { workPhone = it },
                                        label = { Text("Work Phone") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }
                            }
                            item {
                                Text(
                                    "Work Address",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            item {
                                OutlinedTextField(
                                    value = workAddressLine1,
                                    onValueChange = { workAddressLine1 = it },
                                    label = { Text("Address Line 1") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                            item {
                                OutlinedTextField(
                                    value = workAddressLine2,
                                    onValueChange = { workAddressLine2 = it },
                                    label = { Text("Address Line 2") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                            item {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = workCity,
                                        onValueChange = { workCity = it },
                                        label = { Text("City") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = workState,
                                        onValueChange = { workState = it },
                                        label = { Text("State") },
                                        modifier = Modifier.weight(0.7f),
                                        singleLine = true
                                    )
                                }
                            }
                            item {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = workZipCode,
                                        onValueChange = { workZipCode = it },
                                        label = { Text("ZIP") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = workCountry,
                                        onValueChange = { workCountry = it },
                                        label = { Text("Country") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }
                            }
                            item {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = annualIncome,
                                        onValueChange = { annualIncome = it },
                                        label = { Text("Annual Income") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        placeholder = { Text("$50,000") }
                                    )
                                    OutlinedTextField(
                                        value = startDate,
                                        onValueChange = { startDate = it },
                                        label = { Text("Start Date") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        placeholder = { Text("2020-01-15") }
                                    )
                                }
                            }
                        }
                        
                        5 -> { // IDs
                            item {
                                Text(
                                    "Identification Numbers",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "Securely encrypted on your device",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            item {
                                OutlinedTextField(
                                    value = idNumber,
                                    onValueChange = { idNumber = it },
                                    label = { Text("National ID / SSN") },
                                    leadingIcon = { Icon(Icons.Outlined.Badge, null) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                            item {
                                OutlinedTextField(
                                    value = passportNumber,
                                    onValueChange = { passportNumber = it },
                                    label = { Text("Passport Number") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                            item {
                                OutlinedTextField(
                                    value = driverLicenseNumber,
                                    onValueChange = { driverLicenseNumber = it },
                                    label = { Text("Driver's License") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                            item {
                                OutlinedTextField(
                                    value = taxNumber,
                                    onValueChange = { taxNumber = it },
                                    label = { Text("Tax ID / TIN") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                        }
                        
                        6 -> { // Custom Fields
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Custom Fields",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            "Add your own fields for any data you frequently need",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    FilledTonalButton(
                                        onClick = { showAddCustomField = true },
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Add", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                            
                            if (customFields.isEmpty()) {
                                item {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(24.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                Icons.Outlined.Extension,
                                                null,
                                                modifier = Modifier.size(48.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                            )
                                            Text(
                                                "No custom fields yet",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                "Examples: Maiden Name, Spouse Name, Previous Address",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            } else {
                                customFields.forEach { (key, value) ->
                                    item {
                                        CustomFieldRow(
                                            fieldName = key.replace("_", " ").split(" ")
                                                .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } },
                                            fieldValue = value,
                                            onValueChange = { newValue ->
                                                customFields = customFields.toMutableMap().apply { 
                                                    put(key, newValue) 
                                                }
                                            },
                                            onDelete = {
                                                customFields = customFields.toMutableMap().apply { 
                                                    remove(key) 
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Divider()
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    
                    FilledTonalButton(
                        onClick = {
                            val newProfile = (profile ?: Profile()).copy(
                                profileName = profileName,
                                firstName = firstName,
                                middleName = middleName,
                                lastName = lastName,
                                fullName = "$firstName ${if (middleName.isNotEmpty()) "$middleName " else ""}$lastName".trim(),
                                dateOfBirth = dateOfBirth,
                                gender = gender,
                                nationality = nationality,
                                email = email,
                                phoneNumber = phoneNumber,
                                cellphoneCountryCode = countryCode,
                                alternatePhone = alternatePhone,
                                alternatePhoneCountryCode = altCountryCode,
                                addressLine1 = addressLine1,
                                addressLine2 = addressLine2,
                                city = city,
                                state = state,
                                zipCode = zipCode,
                                country = country,
                                postalAddressLine1 = postalAddressLine1,
                                postalAddressLine2 = postalAddressLine2,
                                postalCity = postalCity,
                                postalState = postalState,
                                postalZipCode = postalZipCode,
                                postalCountry = postalCountry,
                                company = company,
                                jobTitle = jobTitle,
                                occupation = occupation,
                                employmentStatus = employmentStatus,
                                workAddressLine1 = workAddressLine1,
                                workAddressLine2 = workAddressLine2,
                                workCity = workCity,
                                workState = workState,
                                workZipCode = workZipCode,
                                workCountry = workCountry,
                                workPhone = workPhone,
                                workPhoneCountryCode = workPhoneCountryCode,
                                workEmail = workEmail,
                                annualIncome = annualIncome,
                                startDate = startDate,
                                idNumber = idNumber,
                                passportNumber = passportNumber,
                                driverLicenseNumber = driverLicenseNumber,
                                taxNumber = taxNumber,
                                customFields = customFields.toMap()
                            )
                            onSave(newProfile)
                        },
                        enabled = firstName.isNotBlank() && lastName.isNotBlank() && profileName.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Save, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Profile")
                    }
                }
            }
        }
    }
    
    // Add Custom Field Dialog
    if (showAddCustomField) {
        AddCustomFieldDialog(
            onDismiss = { showAddCustomField = false },
            onAdd = { fieldName, fieldValue ->
                val key = fieldName.lowercase().replace(" ", "_")
                customFields = customFields.toMutableMap().apply { 
                    put(key, fieldValue) 
                }
                showAddCustomField = false
            }
        )
    }
}

@Composable
fun CustomFieldRow(
    fieldName: String,
    fieldValue: String,
    onValueChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.Extension,
                null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            if (isEditing) {
                OutlinedTextField(
                    value = fieldValue,
                    onValueChange = onValueChange,
                    label = { Text(fieldName) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            } else {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        fieldName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        fieldValue.ifEmpty { "Not set" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (fieldValue.isEmpty()) 
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        else 
                            MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            IconButton(
                onClick = { isEditing = !isEditing },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    if (isEditing) Icons.Filled.Check else Icons.Outlined.Edit,
                    if (isEditing) "Save" else "Edit",
                    modifier = Modifier.size(18.dp)
                )
            }
            
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Outlined.Delete,
                    "Delete",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomFieldDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit
) {
    var fieldName by remember { mutableStateOf("") }
    var fieldValue by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Add Custom Field",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Create a field for data you frequently need",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                OutlinedTextField(
                    value = fieldName,
                    onValueChange = { fieldName = it },
                    label = { Text("Field Name") },
                    leadingIcon = { Icon(Icons.Outlined.Label, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("e.g., Maiden Name") },
                    supportingText = { Text("This will appear as the field label") }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = fieldValue,
                    onValueChange = { fieldValue = it },
                    label = { Text("Value (Optional)") },
                    leadingIcon = { Icon(Icons.Outlined.Edit, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("You can fill this later") }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    
                    FilledTonalButton(
                        onClick = { 
                            if (fieldName.isNotBlank()) {
                                onAdd(fieldName.trim(), fieldValue.trim())
                            }
                        },
                        enabled = fieldName.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Field")
                    }
                }
            }
        }
    }
}
