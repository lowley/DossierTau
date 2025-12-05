package lorry.dossiertau.data.intelligenceService

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lorry.dossiertau.data.intelligenceService.utils.events.AtomicEventType
import lorry.dossiertau.data.intelligenceService.utils.events.AtomicUpdateEvent
import lorry.dossiertau.data.intelligenceService.utils.events.ItemType
import lorry.dossiertau.data.intelligenceService.utils.TauFileObserver
import lorry.dossiertau.data.intelligenceService.utils.events.GlobalUpdateEvent
import lorry.dossiertau.data.intelligenceService.utils.events.IUpdateEvent
import lorry.dossiertau.support.littleClasses.TauDate
import lorry.dossiertau.support.littleClasses.TauPath

class Spy(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ISpy {

    private val scope: CoroutineScope = CoroutineScope(dispatcher + SupervisorJob())

    ////////////////////////////////////
    // interrupteur de fonctionnement //
    ////////////////////////////////////
    val _enabledFlow = MutableStateFlow(false)
    override val enabledFlow = _enabledFlow.asStateFlow()

    override fun startSurveillance() {
        _enabledFlow.update { true }
    }

    override fun stopSurveillance() {
        _enabledFlow.update { false }
    }

    override fun setSurveillance(enabled: Boolean) {
        _enabledFlow.update { enabled }
    }


    ////////////////////////////////////////
    // répertoire observé -> surveillance //
    ////////////////////////////////////////
    val _observedFolderFlow = MutableStateFlow<TauPath>(TauPath.EMPTY)
    override val observedFolderFlow = _observedFolderFlow.asStateFlow()

    override fun setObservedFolder(folderPath: TauPath) {
        _observedFolderFlow.update { folderPath }
    }

    //////////////////
    // surveillance //
    //////////////////
    var fileObserver: TauFileObserver? = null

    ///////////////////////////////////////////////////////////////////////
    // évènements créés par l'espion suite à une opération sur le disque //
    ///////////////////////////////////////////////////////////////////////
    val _updateEventFlow = MutableSharedFlow<IUpdateEvent>()

    override val updateEventFlow: SharedFlow<IUpdateEvent> = _updateEventFlow.asSharedFlow()

    override fun emitIncomingEvent(event: IUpdateEvent) {
        scope.launch {
            _updateEventFlow.emit(event)
        }
    }

    override fun emitFake_CREATEFILE(
        fileToEmit: TauPath,
        itemType: ItemType,
        modificationDate: TauDate
    ) {
        val fakeEvent = AtomicUpdateEvent(
            eventType = AtomicEventType.CREATE,
            path = fileToEmit,
            itemType = itemType,
            modificationDate = modificationDate,
        )

        emitIncomingEvent(fakeEvent)
    }

    suspend fun doOnEvent(atomicUpdateEvent: AtomicUpdateEvent) {
        emitIncomingEvent(atomicUpdateEvent)

    }

    init {

        //////////////
        // réglages //
        //////////////
        observedFolderFlow.onEach { folderPath ->
            disableObservation()
            fileObserver = createObserverWith(folderPath)

            //TODO une méthode pour décider de quoi faire dans CIA? existe déjà?
            //ici on émet un GlobalScan
            val event = GlobalUpdateEvent(folderPath)
            emitIncomingEvent(event)

        }.launchIn(scope)

        enabledFlow.onEach { newEnabled ->
            if (newEnabled)
                fileObserver?.startWatching()
            else
                fileObserver?.stopWatching()
        }.launchIn(scope)
    }
}