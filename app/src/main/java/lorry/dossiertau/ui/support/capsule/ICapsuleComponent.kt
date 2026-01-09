package lorry.dossiertau.ui.support.capsule

import lorry.dossiertau.support.littleClasses.TauPath
import lorry.dossiertau.ui.support.capsule.utilities.CapsuleData
import lorry.dossiertau.ui.support.capsule.utilities.IElementInCapsule
import lorry.dossiertau.ui.support.capsule.utilities.IElementReader

interface ICapsuleComponent {
    suspend fun save(
        element: IElementInCapsule,
        targetPath: TauPath,
        useOld: Boolean = false)

    suspend fun getCapsule(
        targetPath: TauPath,
        useOld: Boolean = false
    ): CapsuleData?

    /**
     * lecture à chaque fois de l'info dans le fichier/dossier
     */
    suspend fun <T> getElement(
        reader: IElementReader<T>,
        targetPath: TauPath): T?
}