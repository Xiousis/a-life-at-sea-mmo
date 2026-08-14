package com.alifeatseammo

import com.alifeatseammo.data.model.StatType
import com.alifeatseammo.data.repository.AdminRepository
import com.alifeatseammo.data.repository.AuthRepository
import com.alifeatseammo.data.repository.GameRepository
import com.alifeatseammo.ui.GameViewModel
import com.google.firebase.auth.FirebaseUser
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {

    private lateinit var viewModel: GameViewModel
    private val authRepository: AuthRepository = mockk(relaxed = true)
    private val gameRepository: GameRepository = mockk(relaxed = true)
    private val adminRepository: AdminRepository = mockk(relaxed = true)
    
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        val user: FirebaseUser = mockk()
        every { user.uid } returns "test_uid"
        every { authRepository.currentUser } returns MutableStateFlow(user)
        
        viewModel = GameViewModel(authRepository, gameRepository, adminRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testTrainSuccess() = runTest {
        coEvery { gameRepository.train(StatType.Strength) } returns true
        
        viewModel.train(StatType.Strength)
        advanceUntilIdle()
        
        assertEquals(null, viewModel.errorMessage.value)
    }

    @Test
    fun testTrainFailure() = runTest {
        val errorMessage = "Not enough energy"
        coEvery { gameRepository.train(StatType.Strength) } throws Exception(errorMessage)
        
        viewModel.train(StatType.Strength)
        advanceUntilIdle()
        
        assertEquals(errorMessage, viewModel.errorMessage.value)
    }
}
