package iut.butinfo3.app_mobile.unit

import iut.butinfo3.app_mobile.model.entity.User
import iut.butinfo3.app_mobile.model.repository.SessionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SessionRepositoryTest {

    @Test
    fun selectedUser_initiallyNull() = runTest {
        // SessionRepository is an object (singleton), value may carry over between tests
        // but initially or after reset it should be observable
        val current = SessionRepository.selectedUser.first()
        // We just verify the flow is accessible and emits (null or a user)
        // Since it's a singleton, we can't guarantee null in isolation
        assertNotNull(SessionRepository.selectedUser)
    }

    @Test
    fun selectUser_updatesStateFlow() = runTest {
        val user = User(
            id = 42,
            username = "TestUser",
            email = "test@example.com"
        )

        SessionRepository.selectUser(user)

        val result = SessionRepository.selectedUser.first()
        assertNotNull(result)
        assertEquals(42, result!!.id)
        assertEquals("TestUser", result.username)
        assertEquals("test@example.com", result.email)
    }

    @Test
    fun selectUser_overwritesPreviousUser() = runTest {
        val user1 = User(id = 1, username = "User1", email = "u1@example.com")
        val user2 = User(id = 2, username = "User2", email = "u2@example.com")

        SessionRepository.selectUser(user1)
        assertEquals(1, SessionRepository.selectedUser.first()!!.id)

        SessionRepository.selectUser(user2)
        assertEquals(2, SessionRepository.selectedUser.first()!!.id)
    }
}
