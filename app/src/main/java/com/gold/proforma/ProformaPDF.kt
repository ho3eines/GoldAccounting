class ProformaPDF {
    fun generate(context: Context, htmlPrices: String, out: File) {
        val doc = PdfDocument()
        val page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
        val c = page.canvas
        c.drawText("پیش‌فاکتور طلا", 50f, 60f, Paint())
        c.drawText(htmlPrices, 50f, 120f, Paint())
        doc.finishPage(page)
        FileOutputStream(out).use { doc.writeTo(it) }
        doc.close()
    }
}
