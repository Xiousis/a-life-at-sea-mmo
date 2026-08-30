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
import kotlin.time.Duration.Companion.seconds
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
    private val gameRepository: GameRepository,
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
                
                // Constructing the OpenID4VP request for Verified Email
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
                                    {"path": ["given_name"]},
                                    {"path": ["family_name"]},
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
                        
                        // IMPORTANT: For production, you MUST send responseJsonString and nonce
                        // to your backend for cryptographic validation of the SD-JWT.
                        // val serverResult = authRepository.verifyAndUpgrade(responseJsonString, nonce)
                        
                        // For demonstration, we'll perform a preliminary parse on the client
                        val email = extractEmailFromSdJwt(responseJsonString)
                        if (email != null) {
                            // TODO: This should be a secure upgrade call that validates the token
                            _authResult.value = authRepository.upgradeGuestAccount(email, "quick-upgrade-${System.currentTimeMillis()}")
                        } else {
                            _authResult.value = AuthResult.Error("Could not retrieve verified email from credential.")
                        }
                    }
                    else -> _authResult.value = AuthResult.Error("Unexpected credential type: ${credential::class.java.simpleName}")
                }
            } catch (e: Exception) {
                android.util.Log.e("AuthViewModel", "Verified Email retrieval failed", e)
                _authResult.value = AuthResult.Error(e.localizedMessage ?: "Verified Email retrieval failed")
            }
        }
    }

    private fun extractEmailFromSdJwt(responseJson: String): String? {
        return try {
            val responseData = JSONObject(responseJson)
            val vpToken = responseData.getJSONObject("vp_token")
            // The key here corresponds to the 'id' in dcql_query ("user_info_query")
            val queryId = "user_info_query" 
            if (!vpToken.has(queryId)) return null
            
            val rawSdJwt = vpToken.getJSONArray(queryId).getString(0)
            
            // SD-JWT format: Issuer JWT ~ Disclosures ~ Key Binding JWT
            val parts = rawSdJwt.split("~")
            
            // Disclosures are base64-encoded JSON arrays [salt, name, value]
            for (i in 1 until (parts.size - 1)) { // Skip first (Issuer JWT) and last (Key Binding)
                try {
                    val part = parts[i]
                    if (part.isEmpty()) continue
                    
                    val decoded = String(Base64.getUrlDecoder().decode(part.toByteArray()))
                    val jsonArray = org.json.JSONArray(decoded)
                    
                    if (jsonArray.length() == 3 && jsonArray.getString(1) == "email") {
                        return jsonArray.getString(2)
                    }
                } catch (_: Exception) {
                    // Not all parts are disclosures we can parse this way
                }
            }
            null
        } catch (e: Exception) {
            android.util.Log.e("AuthViewModel", "Error parsing SD-JWT", e)
            null
        }
    }

    private fun generateNonce(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
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
                delay(2.seconds)
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
