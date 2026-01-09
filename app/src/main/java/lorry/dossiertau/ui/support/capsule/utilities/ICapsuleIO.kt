package lorry.dossiertau.ui.support.capsule.utilities

interface ICapsuleIO {
    suspend fun getCapsule(filePath: String): CapsuleData?
    suspend fun replaceCapsule(filePath: String, capsule: CapsuleData?
    ): Boolean
}