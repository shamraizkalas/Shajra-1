package pk.shajranasab.pdf
import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream
import pk.shajranasab.model.Person

object PdfExporter {
    fun personLine(root:Person,people:List<Person>):String{
        val byId=people.associateBy{it.id}; val parts=mutableListOf<String>(); var p:Person?=root
        while(p!=null){ parts.add("${p.name}${if(p.fatherName.isNotBlank())" ولد ${p.fatherName}" else ""}"); p=byId[p.parentId] }
        return parts.reversed().joinToString(" / ")
    }
    fun export(context:Context,title:String,body:String):File{
        val doc=PdfDocument(); val page=doc.startPage(PdfDocument.PageInfo.Builder(595,842,1).create())
        val c=page.canvas; val paint=Paint().apply{textSize=22f}
        c.drawText(title,50f,70f,paint); paint.textSize=15f
        body.split("\n").forEachIndexed{ i,line->c.drawText(line,50f,110f+i*28f,paint)}
        doc.finishPage(page); val f=File(context.cacheDir,"shajra_${System.currentTimeMillis()}.pdf")
        FileOutputStream(f).use{doc.writeTo(it)}; doc.close(); return f
    }
}
