package iut.butinfo3.app_mobile.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "user",
    indices = [Index(value = ["email"], unique = true)]
)
data class User (
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "username") val username: String,
    @ColumnInfo(name = "email") val email: String? = null,
    @ColumnInfo(name = "create_at") val createdAt: Date? = null,
    @ColumnInfo(name = "sex") val sexe: SexEnum? = null, // TODO: Pour l'instant ce n'est pas utilisé a voir pour futur améliorations.
    @ColumnInfo(name = "birth_date") val birthDate: Date? = null,
    @ColumnInfo(name="weight") val weight : Int? = null,
    @ColumnInfo(name = "password_hash") val passwordHash: String? = null,
    @ColumnInfo(name = "password_salt") val passwordSalt: String? = null,
    @ColumnInfo(name = "is_active") val isActive: Boolean = true
)
