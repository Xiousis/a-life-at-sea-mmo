package com.alifeatseammo.ui

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.DigitalCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetDigitalCredentialOption
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alifeatseammo.data.model.Gender
import com.alifeatseammo.data.model.Race
import com.alifeatseammo.data.repository.AuthRepository
import com.alifeatseammo.data.repository.AuthResult
import com.alifeatseammo.data.repository.GameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.security.SecureRandom
import java.util.Base64
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val gameRepository: GameRepository
) : ViewModel() {

    val currentUser = authRepository.currentUser

    private val _authResult = MutableStateFlow<AuthResult?>(null)
    val authResult: StateFlow<AuthResult?> = _authResult.asStateFlow()

    private val _createCharacterResult = MutableStateFlow<AuthResult?>(null)
    val createCharacterResult: StateFlow<AuthResult?> = _createCharacterResult.asStateFlow()

    fun signIn() {
        viewModelScope.launch {
            _authResult.value = AuthResult.Loading
            _authResult.value = authRepository.signInAnonymously()
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authResult.value = AuthResult.Loading
            _authResult.value = authRepository.signIn(email, password)
        }
    }

    fun signUp(email: String, password: String, username: String) {
        viewModelScope.launch {
            _authResult.value = AuthResult.Loading
            _authResult.value = authRepository.signUp(email, password, username)
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            _authResult.value = AuthResult.Loading
            _authResult.value = authRepository.sendPasswordResetEmail(email)
        }
    }

    fun upgradeGuestAccount(email: String, password: String) {
        viewModelScope.launch {
            _authResult.value = AuthResult.Loading
            _authResult.value = authRepository.upgradeGuestAccount(email, password)
        }
    }

    @OptIn(androidx.credentials.ExperimentalDigitalCredentialApi::class)
    fun startVerifiedEmailUpgrade(activity: Activity) {
        viewModelScope.launch {
            _authResult.value = AuthResult.Loading
            try {
                val credentialManager = CredentialManager.create(activity)
                val nonce = generateNonce()
                
                val openId4vpRequest = """
                    {
                      "requests": [
                        {
                          "protocol": "openid4vp-v1-unsigned",
                          "data": {
                            "response_type": "vp_token",
                            "response_mode": "dc_api",
                            "nonce": "$nonce",
                            "dcql_query": {
                              "credentials": [
                                {
                                  "id": "user_info_query",
                                  "format": "dc+sd-jwt",
                                   "meta": { 
                                      "vct_values": ["UserInfoCredential"] 
                                   },
                                  "claims": [ 
                                    {"path": ["email"]}, 
                                    {"path": ["name"]},  
                                    {"path": ["email_verified"]}
                                  ]
                                }
                              ]
                            }
                          }
                        }
                      ]
                    }
                """.trimIndent()

                val getDigitalCredentialOption = GetDigitalCredentialOption(requestJson = openId4vpRequest)
                val request = GetCredentialRequest(listOf(getDigitalCredentialOption))
                
                val result = credentialManager.getCredential(activity, request)
                
                when (val credential = result.credential) {
                    is DigitalCredential -> {
                        val responseJsonString = credential.credentialJson
                        // In a real app, we would send this to our backend for validation.
                        // Here we'll simulate success by extracting the email if possible.
                        val email = extractEmailFromSdJwt(responseJsonString)
                        if (email != null) {
                            // Link the guest account with this email (using a random password for now)
                            _authResult.value = authRepository.upgradeGuestAccount(email, "quick-upgrade-${System.currentTimeMillis()}")
                        } else {
                            _authResult.value = AuthResult.Error("Could not retrieve verified email.")
                        }
                    }
                    else -> _authResult.value = AuthResult.Error("Unexpected credential type.")
                }
            } catch (e: Exception) {
                _authResult.value = AuthResult.Error(e.localizedMessage ?: "Verified Email retrieval failed")
            }
        }
    }

    private fun generateNonce(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun extractEmailFromSdJwt(responseJson: String): String? {
        return try {
            val responseData = JSONObject(responseJson)
            val vpToken = responseData.getJSONObject("vp_token")
            val credentialId = vpToken.keys().next()
            val rawSdJwt = vpToken.getJSONArray(credentialId).getString(0)
            
            // Simulating SD-JWT parsing. Real parsing involves splitting by '~'
            // and decoding base64 disclosures.
            // For demo purposes, we look for a part that looks like an email.
            val parts = rawSdJwt.split("~")
            parts.forEach { part ->
                val decoded = String(Base64.getUrlDecoder().decode(part.toByteArray()))
                if (decoded.contains("@")) {
                    // SD-JWT disclosures are [salt, name, value]
                    val jsonArray = JSONObject(decoded)
                    // This is a simplification
                    return jsonArray.optString("email") ?: decoded.substringAfterLast("\"").substringBeforeLast("\"")
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    fun clearAuthResult() {
        _authResult.value = null
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                gameRepository.explicitLogout()
            } catch (_: Exception) {}
            authRepository.signOut()
            _authResult.value = null
        }
    }

    fun createCharacter(name: String, gender: Gender, race: Race) {
        viewModelScope.launch {
            _createCharacterResult.value = AuthResult.Loading
            try {
                gameRepository.createCharacter(name, gender, race)
                delay(2000)
                _createCharacterResult.value = null
            } catch (e: Exception) {
                val message = if (e is com.google.firebase.functions.FirebaseFunctionsException && e.code == com.google.firebase.functions.FirebaseFunctionsException.Code.NOT_FOUND) {
                    "Function not found. Please ensure backend is deployed and on Blaze plan."
                } else {
                    e.message ?: "Failed to create character. Name may be taken."
                }
                _createCharacterResult.value = AuthResult.Error(message)
            }
        }
    }

    fun clearCreateCharacterResult() {
        _createCharacterResult.value = null
    }
}
