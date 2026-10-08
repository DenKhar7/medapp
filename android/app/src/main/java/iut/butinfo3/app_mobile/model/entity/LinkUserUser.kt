package iut.butinfo3.app_mobile.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "link_user_user",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["parent_user_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["child_user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["parent_user_id"]),
        Index(value = ["child_user_id"])
    ]
)
data class LinkUserUser(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "parent_user_id") val parentUserId: Int?,
    @ColumnInfo(name = "child_user_id") val childUserId: Int?,
    @ColumnInfo(name = "create_at") val createdAt: Date?,
    @ColumnInfo(name = "relation_type") val relationType: String?
)


