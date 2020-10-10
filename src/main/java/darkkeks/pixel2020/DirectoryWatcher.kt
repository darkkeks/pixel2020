package darkkeks.pixel2020

import java.nio.file.*

class DirectoryWatcher(private val path: Path) {
    fun run(block: (WatchEvent<*>) -> Unit) {
        val watchService = FileSystems.getDefault().newWatchService()
        val path = Paths.get(".")
        path.register(
            watchService,
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_DELETE,
            StandardWatchEventKinds.ENTRY_MODIFY
        )
        var key: WatchKey
        while (watchService.take().also { key = it } != null) {
            for (event in key.pollEvents()) {
                block(event)
            }
            key.reset()
        }
    }
}
