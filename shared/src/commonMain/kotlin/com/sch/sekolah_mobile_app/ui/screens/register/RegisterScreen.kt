package com.sch.sekolah_mobile_app.ui.screens.register

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sch.sekolah_mobile_app.data.model.IdentityResponse
import com.sch.sekolah_mobile_app.data.remote.ApiConfig
import com.sch.sekolah_mobile_app.data.repository.AuthRepository
import com.sch.sekolah_mobile_app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(
    authRepository: AuthRepository,
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    var nis by remember { mutableStateOf("") }
    var verifiedPerson by remember { mutableStateOf<IdentityResponse?>(null) }
    var isVerifying by remember { mutableStateOf(false) }

    var email by remember { mutableStateOf("") }
    var telp by remember { mutableStateOf("") }
    var wa by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var showHostDialog by remember { mutableStateOf(false) }
    var hostText by remember { mutableStateOf(ApiConfig.getHost()) }

    fun handleVerifyNis() {
        val cleanNis = nis.trim()
        if (cleanNis.isBlank()) {
            errorMessage = "Nomor Induk Siswa (NIS) wajib diisi."
            return
        }
        if (!cleanNis.all { it.isDigit() } || cleanNis.length < 4 || cleanNis.length > 20) {
            errorMessage = "Format NIS tidak valid. NIS hanya boleh berisi angka (4-20 digit)."
            return
        }

        errorMessage = null
        successMessage = null
        isVerifying = true

        coroutineScope.launch {
            try {
                val result = authRepository.verifyStudentIdentity(cleanNis)
                verifiedPerson = result
                telp = result.telp ?: ""
                wa = result.wa ?: result.telp ?: ""
                successMessage = "Identitas ditemukan: ${result.name} (${result.detail ?: "Siswa"})"
                isVerifying = false
            } catch (e: Exception) {
                isVerifying = false
                verifiedPerson = null
                errorMessage = e.message ?: "Data siswa tidak ditemukan di sistem sekolah."
            }
        }
    }

    fun handleRegister() {
        errorMessage = null
        successMessage = null

        val currentVerified = verifiedPerson
        if (currentVerified == null) {
            errorMessage = "Silakan periksa dan verifikasi NIS Anda terlebih dahulu."
            return
        }

        val cleanEmail = email.trim()
        if (cleanEmail.isBlank()) {
            errorMessage = "Email siswa wajib diisi."
            return
        }
        if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            errorMessage = "Format email tidak valid."
            return
        }

        if (password.length < 6) {
            errorMessage = "Kata sandi minimal harus 6 karakter."
            return
        }

        if (password != confirmPassword) {
            errorMessage = "Konfirmasi kata sandi tidak cocok."
            return
        }

        isLoading = true
        coroutineScope.launch {
            try {
                authRepository.registerStudent(
                    nis = currentVerified.identifier ?: nis.trim(),
                    email = cleanEmail,
                    password = password,
                    telp = telp.trim(),
                    wa = wa.trim()
                )
                isLoading = false
                onRegisterSuccess()
            } catch (e: Exception) {
                isLoading = false
                errorMessage = e.message ?: "Pendaftaran gagal. Silakan coba lagi."
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 500.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 28.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Logo
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(PrimaryTealContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = "Logo",
                        tint = PrimaryTeal,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Aktivasi Akun Siswa",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkNavy
                )

                Text(
                    text = "Pendaftaran & Aktivasi Portal Murid Sekolah",
                    fontSize = 13.sp,
                    color = SlateGray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Error Banner
                if (errorMessage != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = ErrorRed,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Success Banner
                if (successMessage != null && errorMessage == null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Sukses",
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = successMessage ?: "",
                                color = Color(0xFF065F46),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Step 1: Input NIS & Cek Button
                Text(
                    text = "Langkah 1: Masukkan NIS Terdaftar",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DarkNavy,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = nis,
                        onValueChange = {
                            nis = it
                            errorMessage = null
                            successMessage = null
                        },
                        label = { Text("Nomor Induk Siswa (NIS)") },
                        placeholder = { Text("Contoh: 202610001") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = "NIS", tint = SlateGray)
                        },
                        enabled = verifiedPerson == null && !isVerifying,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { handleVerifyNis() }),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { handleVerifyNis() },
                        enabled = verifiedPerson == null && !isVerifying && nis.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                        modifier = Modifier.height(56.dp)
                    ) {
                        if (isVerifying) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Search, contentDescription = "Cek", tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cek", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Verified Student Badge Card
                if (verifiedPerson != null) {
                    val person = verifiedPerson!!
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF86EFAC))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFDCFCE7)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Verified",
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "DATA TERVERIFIKASI",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF15803D)
                                    )
                                }
                                Text(
                                    text = person.name ?: "Siswa Terdaftar",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkNavy
                                )
                                Text(
                                    text = "NIS: ${person.identifier} • ${person.detail ?: "Kelas Aktif"}",
                                    fontSize = 12.sp,
                                    color = SlateGray
                                )
                            }

                            TextButton(
                                onClick = {
                                    verifiedPerson = null
                                    successMessage = null
                                    errorMessage = null
                                    password = ""
                                    confirmPassword = ""
                                }
                            ) {
                                Text("Ganti", color = Color(0xFFDC2626), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Langkah 2: Buat Akun & Kata Sandi",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DarkNavy,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // Email Field
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; errorMessage = null },
                        label = { Text("Email Siswa") },
                        placeholder = { Text("Contoh: ${person.name?.split(" ")?.firstOrNull()?.lowercase() ?: "siswa"}@sekolah.com") },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = "Email", tint = SlateGray)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Telepon & WhatsApp
                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = telp,
                            onValueChange = { telp = it; errorMessage = null },
                            label = { Text("No. Telepon") },
                            placeholder = { Text("0812xxxxxxxx") },
                            leadingIcon = {
                                Icon(Icons.Default.Phone, contentDescription = "Telp", tint = SlateGray)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        OutlinedTextField(
                            value = wa,
                            onValueChange = { wa = it; errorMessage = null },
                            label = { Text("WhatsApp") },
                            placeholder = { Text("0812xxxxxxxx") },
                            leadingIcon = {
                                Icon(Icons.Default.Phone, contentDescription = "WA", tint = SlateGray)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Password
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; errorMessage = null },
                        label = { Text("Kata Sandi (Min. 6 Karakter)") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = "Password", tint = SlateGray)
                        },
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (isPasswordVisible) "Sembunyikan" else "Tampilkan",
                                    tint = SlateGray
                                )
                            }
                        },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Confirm Password
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it; errorMessage = null },
                        label = { Text("Konfirmasi Kata Sandi") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = "Confirm Password", tint = SlateGray)
                        },
                        trailingIcon = {
                            IconButton(onClick = { isConfirmPasswordVisible = !isConfirmPasswordVisible }) {
                                Icon(
                                    imageVector = if (isConfirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (isConfirmPasswordVisible) "Sembunyikan" else "Tampilkan",
                                    tint = SlateGray
                                )
                            }
                        },
                        visualTransformation = if (isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { handleRegister() }),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Register Button
                    Button(
                        onClick = { handleRegister() },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Mengaktifkan Akun...", color = Color.White, fontSize = 15.sp)
                        } else {
                            Text(
                                text = "Daftar & Aktifkan Akun Siswa",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Back to Login Link
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sudah memiliki akun siswa?",
                        fontSize = 13.sp,
                        color = SlateGray
                    )
                    TextButton(onClick = onNavigateToLogin) {
                        Text(
                            text = "Masuk di sini",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryTeal
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Server Host Config Button
                TextButton(
                    onClick = {
                        hostText = ApiConfig.getHost()
                        showHostDialog = true
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Dns,
                        contentDescription = "Server Host",
                        tint = SlateGray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Host Server: ${ApiConfig.getHost()}",
                        fontSize = 12.sp,
                        color = SlateGray
                    )
                }
            }
        }
    }

    if (showHostDialog) {
        AlertDialog(
            onDismissRequest = { showHostDialog = false },
            title = { Text("Konfigurasi Host Server") },
            text = {
                Column {
                    Text(
                        text = "Gunakan 10.0.2.2 untuk Android Emulator, localhost untuk iOS Simulator, atau IP LAN komputer Anda.",
                        fontSize = 12.sp,
                        color = SlateGray
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = hostText,
                        onValueChange = { hostText = it },
                        label = { Text("Host / IP Address") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        ApiConfig.setHost(hostText)
                        showHostDialog = false
                    }
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showHostDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}
