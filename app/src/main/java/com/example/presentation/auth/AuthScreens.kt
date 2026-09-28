package com.example.presentation.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.example.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.MockDataStore
import com.example.presentation.components.VyaparButton
import com.example.presentation.components.VyaparPasswordTextField
import com.example.presentation.components.VyaparTextField
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onNavigate: (route: String) -> Unit) {
    val currentUser by MockDataStore.currentUser.collectAsState()
    val currentBusiness by MockDataStore.currentBusiness.collectAsState()

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2000)
        if (currentUser == null) {
            onNavigate("login")
        } else if (currentBusiness == null && currentUser?.businessId == null) {
            onNavigate("onboarding")
        } else {
            onNavigate("dashboard")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BlueNavy),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(BlueLight, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.str_vs), fontSize = 36.sp, fontWeight = FontWeight.Bold, color = BlueNavy)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                stringResource(R.string.str_vyaparsetu),
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                stringResource(R.string.str_run_your_businessnunderstand_y),
                color = BlueLight.copy(alpha = 0.8f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
        }
    }
}

@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var loginError by remember { mutableStateOf<String?>(null) }
    
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BlueBackground)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(64.dp))
        
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(BlueNavy, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(stringResource(R.string.str_vs), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.str_welcome_back),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.str_log_in_to_manage_your_business),
            fontSize = 14.sp,
            color = TextSecondary
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        VyaparTextField(
            value = email,
            onValueChange = { 
                email = it
                emailError = null
                loginError = null
            },
            label = "Email or Phone Number",
            keyboardType = KeyboardType.Email,
            isError = emailError != null,
            errorMessage = emailError
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        VyaparPasswordTextField(
            value = password,
            onValueChange = { 
                password = it
                passwordError = null
                loginError = null
            },
            isError = passwordError != null,
            errorMessage = passwordError
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = { /* TODO: Forgot Password */ }) {
                Text(stringResource(R.string.str_forgot_password), color = BlueAccent, fontWeight = FontWeight.Medium)
            }
        }
        
        if (loginError != null) {
            Text(
                text = loginError!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        VyaparButton(
            text = "Login",
            isLoading = isLoading,
            onClick = {
                var isValid = true
                if (email.isBlank()) {
                    emailError = "Please enter your email or phone"
                    isValid = false
                }
                if (password.isBlank()) {
                    passwordError = "Please enter your password"
                    isValid = false
                }
                
                if (isValid) {
                    isLoading = true
                    scope.launch {
                        val result = MockDataStore.login(email, password)
                        isLoading = false
                        if (result.isSuccess) {
                            onLoginSuccess()
                        } else {
                            loginError = "Invalid credentials. Please try again."
                        }
                    }
                }
            }
        )
        
        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(32.dp))
        
        Row(
            modifier = Modifier.padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.str_dont_have_an_account), color = TextSecondary)
            Text(
                stringResource(R.string.str_create_your_business_account),
                color = BlueAccent,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onNavigateToRegister() }
            )
        }
    }
}

@Composable
fun RegisterScreen(
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    
    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmError by remember { mutableStateOf<String?>(null) }
    
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BlueBackground)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = stringResource(R.string.str_create_your_account),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.str_start_managing_your_business_d),
            fontSize = 14.sp,
            color = TextSecondary,
            modifier = Modifier.align(Alignment.Start)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        VyaparTextField(
            value = name,
            onValueChange = { name = it; nameError = null },
            label = "Full Name",
            isError = nameError != null,
            errorMessage = nameError
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        VyaparTextField(
            value = email,
            onValueChange = { email = it; emailError = null },
            label = "Email Address",
            keyboardType = KeyboardType.Email,
            isError = emailError != null,
            errorMessage = emailError
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        VyaparTextField(
            value = phone,
            onValueChange = { phone = it; phoneError = null },
            label = "Phone Number",
            keyboardType = KeyboardType.Phone,
            isError = phoneError != null,
            errorMessage = phoneError
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        VyaparPasswordTextField(
            value = password,
            onValueChange = { password = it; passwordError = null },
            label = "Password",
            isError = passwordError != null,
            errorMessage = passwordError
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        VyaparPasswordTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it; confirmError = null },
            label = "Confirm Password",
            isError = confirmError != null,
            errorMessage = confirmError
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        VyaparButton(
            text = "Create Account",
            isLoading = isLoading,
            onClick = {
                var isValid = true
                if (name.isBlank()) { nameError = "Name is required"; isValid = false }
                if (email.isBlank() || !email.contains("@")) { emailError = "Valid email required"; isValid = false }
                if (phone.isBlank() || phone.length < 10) { phoneError = "Valid phone required"; isValid = false }
                if (password.length < 6) { passwordError = "Password must be at least 6 characters"; isValid = false }
                if (password != confirmPassword) { confirmError = "Passwords do not match"; isValid = false }
                
                if (isValid) {
                    isLoading = true
                    scope.launch {
                        val result = MockDataStore.register(name, email, phone, password)
                        isLoading = false
                        if (result.isSuccess) {
                            onRegisterSuccess()
                        }
                    }
                }
            }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(
            modifier = Modifier.padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.str_already_have_an_account), color = TextSecondary)
            Text(
                stringResource(R.string.str_login),
                color = BlueAccent,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onNavigateToLogin() }
            )
        }
    }
}
