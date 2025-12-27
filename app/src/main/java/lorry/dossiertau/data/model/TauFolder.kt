package lorry.dossiertau.data.model

import lorry.dossiertau.data.intelligenceService.utils.events.ItemType
import lorry.dossiertau.data.intelligenceService.utils2.repo.FileId
import lorry.dossiertau.data.planes.DbItem
import lorry.dossiertau.support.littleClasses.TauDate
import lorry.dossiertau.support.littleClasses.TauIdentifier
import lorry.dossiertau.support.littleClasses.TauItemName
import lorry.dossiertau.support.littleClasses.TauPath
import lorry.dossiertau.support.littleClasses.TauPicture
import lorry.dossiertau.support.littleClasses.path

sealed class TauFolder private constructor(): TauItem {
    inline val asData: Data? get() = this as? Data

    data object EMPTY : TauFolder()
    data class Data(
        override val id: TauIdentifier = TauIdentifier.random(),
        override val parentPath: TauPath,
        override val name: TauItemName,
        override val picture: TauPicture = TauPicture.NONE,
        override val modificationDate: TauDate = TauDate.now(),
        override val fileId: FileId,

        val children: List<TauItem> = emptyList()

    ) : TauDataCommon, TauFolder() {

        constructor(
            id: TauIdentifier = TauIdentifier.random(),
            fullPath: TauPath,
            picture: TauPicture = TauPicture.NONE,
            modificationDate: TauDate = TauDate.now(),
            children: List<TauItem> = emptyList<TauItem>(),
            fileId: FileId = FileId.EMPTY
        ) : this(
            parentPath = splitParentAndName(fullPath).first,
            name = splitParentAndName(fullPath).second,
            picture = picture,
            modificationDate = modificationDate,
            children = children,
            id = id,
            fileId = fileId
        )

        override fun toString(): String {
            val name = name.value
            val hasPicture = if (picture != TauPicture.NONE) "✅" else "❌"
            val modificationDate = modificationDate.toddMMyyyyHHmmss()
            val children = children.size
            val path = parentPath
            val id = fileId.id

            return "\uD835\uDED5Folder ▶ 🪧($name) 🌿$path 🪁(${children}) 🖼️${hasPicture} 📆${modificationDate} \uD83D\uDD11 (DEV ${id?.dev}, INO ${id?.ino}) ◀"
        }
    }

    companion object {
        operator fun invoke(
            id: TauIdentifier = TauIdentifier.random(),
            fullPath: TauPath,
            picture: TauPicture = TauPicture.NONE,
            modificationDate: TauDate = TauDate.now(),
            children: List<TauItem> = emptyList<TauItem>(),
            fileId: FileId = FileId.EMPTY
        ): TauFolder {
            return Data(
                id = id,
                parentPath = splitParentAndName(fullPath).first,
                name = splitParentAndName(fullPath).second,
                picture = picture,
                modificationDate = modificationDate,
                children = children,
                fileId = fileId
            )
        }

        operator fun invoke(
            id: TauIdentifier = TauIdentifier.random(),
            parentPath: TauPath,
            name: TauItemName,
            picture: TauPicture = TauPicture.NONE,
            modificationDate: TauDate = TauDate.now(),
            children: List<TauItem> = emptyList<TauItem>(),
            fileId: FileId = FileId.EMPTY
        ): TauFolder {
            return Data(
                id = id,
                parentPath = parentPath,
                name = name,
                picture = picture,
                modificationDate = modificationDate,
                children = children,
                fileId = fileId
            )
        }

        private fun splitParentAndName(full: TauPath): Pair<TauPath, TauItemName> {
            val s = full.path
            val i = s.lastIndexOf('/')
            val parent = if (i <= 0) TauPath.EMPTY else TauPath.of(s.take(i))
            val base = if (i < 0) s else s.substring(i + 1)
            return parent to TauItemName(base)
        }
    }

    fun addItem(itemToAdd: TauItem): TauFolder{

        val children = asData?.children
        //sans path, on ne créé pas de nouvel élément
        if (this is EMPTY)
            return this

        val data = this as Data
        if (itemToAdd?.asDataCommon?.fullPath in children!!.map { it.asDataCommon?.fullPath }) {
            return this
        }

        return data.copy(children = children!!.plus(itemToAdd))
    }

    fun removeItem(itemToRemove: TauItem): TauFolder{

        println("REMOVE ITEM: ${itemToRemove.name?.value}")

        val children = asData?.children
        //sans path, on ne créé pas de nouvel élément
        if (this is EMPTY) {
            println("             folder empty -> return")
            return this
        }

        val data = this as Data
        if (itemToRemove?.asDataCommon?.fullPath !in children!!.map { it.asDataCommon?.fullPath }) {
            println("remove nothing as not found")
            return this
        }

        println("remove one item: ${itemToRemove.name?.value}")
        val newChildren = children!!.filter { it.fullPath != itemToRemove.fullPath }
        return data.copy(children = newChildren)
    }

    fun modifyItem(itemToModify: TauItem): TauFolder{

        val children = asData?.children
        //sans path, on ne créé pas de nouvel élément
        if (this is EMPTY)
            return this

        val data = this as Data
        if (itemToModify?.asDataCommon?.fullPath !in children!!.map { it.asDataCommon?.fullPath }) {
            return this
        }

        return data.copy(
            children = children!!.filter { item -> item.asDataCommon?.fullPath != itemToModify.asDataCommon?.fullPath }.plus(itemToModify)
        )
    }


    override fun toString(): String {
        val PB = "\uD835\uDED5Folder(PB)"

        return asData?.toString() ?: PB
    }
}

inline val TauFolder.name: TauItemName get() = asData?.name ?: TauItemName.EMPTY
inline val TauFolder.parentPath: TauPath get() = asData?.parentPath ?: TauPath.EMPTY
inline val TauFolder.picture: TauPicture get() = asData?.picture ?: TauPicture.NONE
inline val TauFolder.modificationDate: TauDate
    get() = asData?.modificationDate ?: TauDate.fromLong(0L)
inline val TauFolder.fileId: FileId get() = asData?.fileId ?: FileId.EMPTY

inline val TauFolder.children: List<TauItem> get() = asData?.children ?: emptyList()

fun TauFolder.toDbFile() = DbItem(
    fullPath = this.fullPath,
    modificationDate = this.modificationDate,
    type = ItemType.FOLDER
)