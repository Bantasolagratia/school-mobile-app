package com.sch.sekolah_mobile_app.ui.screens.activation

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
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sch.sekolah_mobile_app.data.model.UserProfileResponse
import com.sch.sekolah_mobile_app.data.repository.AuthRepository
import com.sch.sekolah_mobile_app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ActivationScreen(
    authRepository: AuthRepository,
    currentProfile: UserProfileResponse?,
    onActivationSuccess: (UserProfileResponse) -> Unit,
    onSignOut: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val initialUsername = currentProfile?.effectiveUsername ?: ""

    var otpCode by remember { mutableStateOf("") }
    var newUsername by remember { mutableStateOf(initialUsername) }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    val userFullName = currentProfile?.identities?.firstOrNull()?.name
        ?: currentProfile?.effectiveUsername
        ?: "Pengguna"

    val roleLabel = when {
        currentProfile?.isRoleGuru == true -> "Pendidik / Guru"
        currentProfile?.isRoleMurid == true -> "Peserta Didik / Siswa"
        else -> "Pengguna"
    }

    fun performActivation() {
        val cleanOtp = otpCode.trim()
        if (cleanOtp.isBlank()) {
            errorMessage = "Kode OTP Aktivasi wajib diisi."
            return
        }

        if (newPassword.isBlank()) {
            errorMessage = "Kata sandi baru tidak boleh kosong."
            return
        }

        if (newPassword.length < 6) {
            errorMessage = "Kata sandi baru minimal harus 6 karakter."
            return
        }

        if (newPassword != confirmPassword) {
            errorMessage = "Konfirmasi kata sandi tidak cocok."
            return
        }

        isLoading = true
        errorMessage = null
        successMessage = null

        coroutineScope.launch {
            try {
                val updated = authRepository.completeOnboarding(
                    otpCode = cleanOtp,
                    newUsername = newUsername.trim(),
                    newPassword = newPassword
                )
                successMessage = "Aktivasi akun berhasil! Mengalihkan ke menu utama..."
                isLoading = false
                onActivationSuccess(updated)
            } catch (e: Exception) {
                isLoading = false
                errorMessage = e.message ?: "Terjadi kesalahan saat aktivasi akun."
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = LightBackground
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(PrimaryTealContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Aktivasi Akun",
                        tint = PrimaryTeal,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Aktivasi Akun Pengguna",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkNavy,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Selamat datang, $userFullName ($roleLabel). Akun Anda perlu diaktivasi dengan kode OTP sebelum dapat mengakses aplikasi.",
                    fontSize = 13.sp,
                    color = SlateGray,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Form Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        // OTP Code Field
                        Text(
                            text = "KODE OTP AKTIVASI",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlateGray,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = otpCode,
                            onValueChange = { if (it.length <= 8) otpCode = it.trim() },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Contoh: 123456", color = SlateLight) },
                            leadingIcon = {
                                Icon(Icons.Default.Key, contentDescription = null, tint = PrimaryTeal)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            ),
                            shape = RoundedCornerShape(10.dp),
                            textStyle = LocalTextStyle.current.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Username Field (Editable)
                        Text(
                            text = "USERNAME LOGIN",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlateGray,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = newUsername,
                            onValueChange = { newUsername = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Username akun Anda", color = SlateLight) },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = SlateGray)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // New Password Field
                        Text(
                            text = "KATA SANDI BARU PERMANEN",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlateGray,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Minimal 6 karakter", color = SlateLight) },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = SlateGray)
                            },
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = SlateGray
                                    )
                                }
                            },
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Next
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Confirm Password Field
                        Text(
                            text = "KONFIRMASI KATA SANDI BARU",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlateGray,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Ulangi kata sandi baru", color = SlateLight) },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = SlateGray)
                            },
                            trailingIcon = {
                                IconButton(onClick = { isConfirmPasswordVisible = !isConfirmPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isConfirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = SlateGray
                                    )
                                }
                            },
                            visualTransformation = if (isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { performActivation() }),
                            shape = RoundedCornerShape(10.dp)
                        )

                        // Error Banner
                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Surface(
                                color = Color(0xFFFEF2F2),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA))
                            ) {
                                Text(
                                    text = errorMessage ?: "",
                                    color = ErrorRed,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        // Success Banner
                        if (successMessage != null) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Surface(
                                color = Color(0xFFF0FDF4),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0))
                            ) {
                                Text(
                                    text = successMessage ?: "",
                                    color = Color(0xFF15803D),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Submit Button
                        Button(
                            onClick = { performActivation() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            enabled = !isLoading,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Mengaktivasi Akun...", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Aktivasi & Masuk Aplikasi", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Sign Out Option
                TextButton(
                    onClick = onSignOut,
                    enabled = !isLoading
                ) {
                    Icon(
                        Icons.Default.Logout,
                        contentDescription = "Keluar",
                        tint = SlateGray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Keluar atau Gunakan Akun Lain",
                        fontSize = 13.sp,
                        color = SlateGray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

