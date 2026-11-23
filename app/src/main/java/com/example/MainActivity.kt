package com.example

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.authStateFlow
import com.example.core.configureAppCheck
import com.example.core.signOutUser
import com.example.data.model.UiState
import com.example.data.repository.AdminControlRepository
import com.example.data.repository.DocumentRepository
import com.example.data.repository.QuizRepository
import com.example.data.repository.SessionRepository
import com.example.ui.screens.AdminShell
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BlockedAccessScreen
import com.example.ui.screens.StudentNameOnboardingScreen
import com.example.ui.screens.StudentShell
import com.example.ui.theme.DarbarTheme
import com.example.ui.theme.ThemeMode
import com.example.ui.viewmodel.AdminViewModel
import com.example.ui.viewmodel.QuizViewModel
import com.example.ui.viewmodel.SessionViewModel
import com.example.ui.viewmodel.StudentViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        setContent {
            DarbarRootApp(application = application)
        }
    }
}

@Composable
private fun DarbarRootApp(application: Application) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val authFlow = remember { Firebase.auth.authStateFlow() }
    val currentUser by authFlow.collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideLayout = maxWidth >= 600.dp

        if (currentUser == null) {
            DarbarTheme(themeMode = ThemeMode.SYSTEM) {
                AuthScreen(onAuthSuccess = {})
            }
        } else {
            val uid = currentUser!!.uid
            key(uid) {
                val db = remember(application) {
                    FirebaseFirestore.getInstance(application.getString(R.string.firestore_database_id))
                }
                val sessionRepo = remember(application, db) { SessionRepository(application, db) }
                val docRepo = remember(application, db) { DocumentRepository(application, db) }
                val quizRepo = remember(db) { QuizRepository(db) }
                val adminControlRepo = remember(db) { AdminControlRepository(db) }

                val factory = remember(uid, sessionRepo, docRepo, quizRepo, adminControlRepo) {
                    object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
                            modelClass.isAssignableFrom(SessionViewModel::class.java) ->
                                SessionViewModel(sessionRepo, uid) as T
                            modelClass.isAssignableFrom(StudentViewModel::class.java) ->
                                StudentViewModel(docRepo, adminControlRepo, uid) as T
                            modelClass.isAssignableFrom(QuizViewModel::class.java) ->
                                QuizViewModel(quizRepo, uid) as T
                            modelClass.isAssignableFrom(AdminViewModel::class.java) ->
                                AdminViewModel(docRepo, quizRepo, adminControlRepo) as T
                            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                        }
                    }
                }

                val sessionViewModel: SessionViewModel = viewModel(key = "session_$uid", factory = factory)
                val studentViewModel: StudentViewModel = viewModel(key = "student_$uid", factory = factory)
                val quizViewModel: QuizViewModel = viewModel(key = "quiz_$uid", factory = factory)
                val adminViewModel: AdminViewModel = viewModel(key = "admin_$uid", factory = factory)

                val adminState by sessionViewModel.adminState.collectAsStateWithLifecycle()
                val studentState by sessionViewModel.studentState.collectAsStateWithLifecycle()
                val appSettings by sessionViewModel.appSettings.collectAsStateWithLifecycle()
                val userThemeMode by sessionViewModel.themeMode.collectAsStateWithLifecycle()
                val reducedMotion by sessionViewModel.reducedMotion.collectAsStateWithLifecycle()
                val rememberedName by sessionViewModel.rememberedStudentName.collectAsStateWithLifecycle()
                val adminConsoleOpen by sessionViewModel.adminConsoleOpen.collectAsStateWithLifecycle()
                val isSavingName by sessionViewModel.isSavingStudentName.collectAsStateWithLifecycle()
                val sessionMsg by sessionViewModel.sessionMessage.collectAsStateWithLifecycle()

                val effectiveTheme = remember(userThemeMode, appSettings.defaultTheme) {
                    if (userThemeMode != ThemeMode.SYSTEM) userThemeMode
                    else ThemeMode.entries.firstOrNull { it.name == appSettings.defaultTheme } ?: ThemeMode.SYSTEM
                }

                val handleSignOut: () -> Unit = {
                    signOutUser(
                        credentialManager = credentialManager,
                        scope = scope,
                        onBeforeSignOut = { sessionViewModel.clearLocalStudentRemembered() }
                    )
                }

                val adminUser = (adminState as? UiState.Success)?.data
                val studentProfile = (studentState as? UiState.Success)?.data

                DarbarTheme(themeMode = effectiveTheme) {
                    when {
                        adminConsoleOpen && adminUser != null && adminUser.active -> {
                            AdminShell(
                                adminUser = adminUser,
                                appSettings = appSettings,
                                reducedMotion = reducedMotion,
                                isWideLayout = isWideLayout,
                                adminViewModel = adminViewModel,
                                onSwitchToStudentView = { sessionViewModel.setAdminConsoleOpen(false) },
                                onLogout = handleSignOut
                            )
                        }
                        appSettings.maintenanceMode || studentProfile?.disabled == true -> {
                            BlockedAccessScreen(
                                isMaintenance = appSettings.maintenanceMode,
                                isDisabledStudent = studentProfile?.disabled == true,
                                adminUser = adminUser,
                                onOpenAdminConsole = { sessionViewModel.setAdminConsoleOpen(true) },
                                onSignOut = handleSignOut
                            )
                        }
                        studentState is UiState.Loading -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                        studentProfile == null -> {
                            StudentNameOnboardingScreen(
                                initialName = rememberedName,
                                isSaving = isSavingName,
                                errorMessage = sessionMsg,
                                adminUser = adminUser,
                                onContinue = { enteredName -> sessionViewModel.saveStudentName(enteredName) },
                                onOpenAdminConsole = { sessionViewModel.setAdminConsoleOpen(true) },
                                onSignOut = handleSignOut
                            )
                        }
                        else -> {
                            StudentShell(
                                student = studentProfile,
                                adminUser = adminUser,
                                appSettings = appSettings,
                                themeMode = effectiveTheme,
                                reducedMotion = reducedMotion,
                                isWideLayout = isWideLayout,
                                sessionViewModel = sessionViewModel,
                                studentViewModel = studentViewModel,
                                quizViewModel = quizViewModel,
                                onOpenAdminConsole = { sessionViewModel.setAdminConsoleOpen(true) },
                                onLogout = handleSignOut
                            )
                        }
                    }
                }
            }
        }
    }
}
