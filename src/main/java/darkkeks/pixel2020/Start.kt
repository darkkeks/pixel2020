package darkkeks.pixel2020

import darkkeks.pixel2019.Controller
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors.toList
import javax.imageio.ImageIO

fun loadImage(path: Path): BufferedImage {
    return ImageIO.read(path.toFile())
}

fun main() {
    val templatePath = Path.of("template.png")
    val urlsPath = Path.of("urls.txt")

    val image = loadImage(templatePath)
    val urls = Files.lines(urlsPath)
        .filter{ it.startsWith("?") }
        .map { Credentials(it) }
        .collect(toList())

    val iterator = urls.listIterator()
    val controller = Controller(iterator.next(), Template(image))

    iterator.remove()
    iterator.forEachRemaining {
        controller.addAccount(it)
    }

    // Watch dir for changes
    DirectoryWatcher(Path.of(".")).run { event ->
        if (event.kind().name() != "ENTRY_DELETE") {
            if (event.context().toString() == "template.png") {
                println("New template is loading")
                controller.updateTemplate(Template(loadImage(templatePath)))
            }
        }
    }
}
