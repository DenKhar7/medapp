package iut.butinfo3.app_mobile.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

// Entite representant une ordonnance scannee et stockee localement
@Entity(
    tableName = "ordonnance",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId")]
)
data class Ordonnance(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    
    @ColumnInfo(name = "userId") val userId: Int,
    
    @ColumnInfo(name = "doctor_name") val doctorName: String? = null,
    
    @ColumnInfo(name = "doctor_address") val doctorAddress: String? = null,
    
    @ColumnInfo(name = "prescription_date") val prescriptionDate: Date? = null,
    
    @ColumnInfo(name = "scan_date") val scanDate: Date,
    
    @ColumnInfo(name = "full_text") val fullText: String,
    
    @ColumnInfo(name = "image_path") val imagePath: String? = null,
    
    @ColumnInfo(name = "notes") val notes: String? = null
)
