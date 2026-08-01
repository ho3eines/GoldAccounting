// درایور janino برای کامپایل همگانی جاوا بدون javac
// استفاده: java -cp jlaunch.jar:janino.jar:commons.jar LauncherKt <android-lite> <dest-dir> <src-dir>...
import org.codehaus.commons.compiler.util.resource.FileResource
import org.codehaus.commons.compiler.util.resource.Resource
import org.codehaus.janino.Compiler
import java.io.File

fun main(args: Array<String>) {
    if (args.size < 3) {
        System.err.println("Usage: LauncherKt <android-lite-jar> <dest-dir> <src-dir> [<gen-dir>...]")
        kotlin.system.exitProcess(2)
    }
    val androidLite = File(args[0])
    val dest = File(args[1])
    val sources = mutableListOf<File>()
    for (i in 2 until args.size) {
        File(args[i]).walkTopDown()
            .filter { it.isFile && it.name.endsWith(".java") }
            .forEach { sources.add(it) }
    }
    sources.sortWith(compareBy { it.absolutePath })
    dest.mkdirs()

    val c = Compiler()
    c.setSourcePath(arrayOf<File>())
    c.setClassPath(arrayOf(androidLite))
    c.setDestinationDirectory(dest, true)
    c.setCharacterEncoding("UTF-8")
    // بایت‌کد جاوا ۷ — سازگار با D8
    c.setTargetVersion(7)
    c.setVerbose(false)

    val res = sources.map { org.codehaus.commons.compiler.util.resource.FileResource(it) }
        .toTypedArray<org.codehaus.commons.compiler.util.resource.Resource>()
    try {
        c.compile(res)
    } catch (t: Throwable) {
        System.err.println("JANINO_FAILED: " + t.message)
        t.printStackTrace(System.err)
        kotlin.system.exitProcess(1)
    }
    println("JANINO_OK classes=" + sources.size)
}
