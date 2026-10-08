package iut.butinfo3.app_mobile.integration

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import iut.butinfo3.app_mobile.TestDataFactory
import iut.butinfo3.app_mobile.model.dao.UserDao
import iut.butinfo3.app_mobile.model.database.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserDaoIntegrationTest {

    private lateinit var db: AppDatabase
    private lateinit var userDao: UserDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        userDao = db.userDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertUser_thenGetById_returnsUser() = runTest {
        val user = TestDataFactory.createUser(username = "Jean", email = "jean@test.com")
        val id = userDao.insertUser(user).toInt()

        val result = userDao.getUserById(id).first()
        assertEquals("Jean", result.username)
        assertEquals("jean@test.com", result.email)
    }

    @Test
    fun getUserByEmail_existing_returnsUser() = runTest {
        val user = TestDataFactory.createUser(username = "Jean", email = "jean@test.com")
        userDao.insertUser(user)

        val result = userDao.getUserByEmail("jean@test.com")
        assertNotNull(result)
        assertEquals("Jean", result!!.username)
    }

    @Test
    fun getUserByEmail_nonExisting_returnsNull() = runTest {
        val result = userDao.getUserByEmail("unknown@test.com")
        assertNull(result)
    }

    @Test
    fun updateUser_modifiesFields() = runTest {
        val user = TestDataFactory.createUser(username = "Jean", email = "jean@test.com")
        val id = userDao.insertUser(user).toInt()
        val inserted = userDao.getUserById(id).first()

        val updated = inserted.copy(username = "Pierre")
        userDao.update(updated)

        val result = userDao.getUserById(id).first()
        assertEquals("Pierre", result.username)
    }

    @Test
    fun createLinkedProfile_createsUserAndLink() = runTest {
        val parent = TestDataFactory.createUser(username = "Parent", email = "parent@test.com")
        val parentId = userDao.insertUser(parent).toInt()

        val child = TestDataFactory.createUser(username = "Enfant", email = null)
        userDao.createLinkedProfile(child, parentId, "enfant")

        val linked = userDao.getLinkedUsers(parentId).first()
        assertEquals(1, linked.size)
        assertEquals("Enfant", linked[0].username)
    }

    @Test
    fun getLinkedUsers_returnsOnlyChildren() = runTest {
        val parent = TestDataFactory.createUser(username = "Parent", email = "parent@test.com")
        val parentId = userDao.insertUser(parent).toInt()

        val child1 = TestDataFactory.createUser(username = "Child1", email = null)
        val child2 = TestDataFactory.createUser(username = "Child2", email = null)
        userDao.createLinkedProfile(child1, parentId, "enfant")
        userDao.createLinkedProfile(child2, parentId, "enfant")

        val linked = userDao.getLinkedUsers(parentId).first()
        assertEquals(2, linked.size)
        // Parent should NOT be in the list
        assertTrue(linked.none { it.username == "Parent" })
    }

    @Test
    fun getAllMembers_returnsParentAndChildren() = runTest {
        val parent = TestDataFactory.createUser(username = "Parent", email = "parent@test.com")
        val parentId = userDao.insertUser(parent).toInt()

        val child = TestDataFactory.createUser(username = "Child", email = null)
        userDao.createLinkedProfile(child, parentId, "enfant")

        val members = userDao.getAllMembers(parentId).first()
        assertTrue(members.size >= 2)
        assertTrue(members.any { it.username == "Parent" })
        assertTrue(members.any { it.username == "Child" })
    }

    @Test(expected = SQLiteConstraintException::class)
    fun insertDuplicateEmail_throwsException() = runTest {
        val user1 = TestDataFactory.createUser(username = "User1", email = "same@test.com")
        val user2 = TestDataFactory.createUser(username = "User2", email = "same@test.com")
        userDao.insertUser(user1)
        userDao.insertUser(user2)
    }
}
