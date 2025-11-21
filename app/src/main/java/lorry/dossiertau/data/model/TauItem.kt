package lorry.dossiertau.data.model

import kotlinx.serialization.Serializable
import lorry.dossiertau.support.littleClasses.TauDate
import lorry.dossiertau.support.littleClasses.TauIdentifier
import lorry.dossiertau.support.littleClasses.TauItemName
import lorry.dossiertau.support.littleClasses.TauPath
import lorry.dossiertau.support.littleClasses.TauPicture

@Serializable
class TauItem (
    val id: TauIdentifier = TauIdentifier.random(),
    val parentPath: TauPath = TauPath.EMPTY,
    val name: TauItemName = TauItemName.EMPTY,
    val picture: TauPicture = TauPicture.EMPTY,
    val modificationDate: TauDate = TauDate.now()


    ){


}