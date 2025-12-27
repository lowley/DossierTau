package lorry.dossiertau.data.intelligenceService.utils2.repo

import kotlinx.serialization.Serializable
import lorry.dossiertau.support.littleClasses.TauPath

interface ISpyRepo {

    fun getIdOf(path: TauPath): FileId

}