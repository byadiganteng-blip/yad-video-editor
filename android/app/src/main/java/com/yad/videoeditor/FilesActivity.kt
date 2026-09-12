package com.yad.videoeditor

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

/**
 * Files - File manager
 * Created by KARYADI, Coding by KARYADI
 */
class FilesActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var tvPath: TextView
    private lateinit var tvEmpty: TextView
    private var currentDir: File = Environment.getExternalStorageDirectory()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_files)
        } catch (e: Exception) {
            finish(); return
        }

        recycler = findViewById(R.id.recyclerFiles)
        tvPath = findViewById(R.id.tvPath)
        tvEmpty = findViewById(R.id.tvEmpty)
        recycler.layoutManager = LinearLayoutManager(this)

        findViewById<Button>(R.id.btnUp)?.setOnClickListener {
            currentDir.parentFile?.let { if (it.exists()) { currentDir = it; loadFiles() } }
        }
        findViewById<Button>(R.id.btnRefresh)?.setOnClickListener { loadFiles() }

        loadFiles()
    }

    private fun loadFiles() {
        tvPath.text = currentDir.absolutePath
        val items = currentDir.listFiles()
            ?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            ?: emptyList()
        if (items.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            recycler.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            recycler.visibility = View.VISIBLE
            recycler.adapter = FileAdapter(items) { file ->
                if (file.isDirectory) { currentDir = file; loadFiles() }
                else showFileOptions(file)
            }
        }
    }

    private fun showFileOptions(file: File) {
        android.app.AlertDialog.Builder(this)
            .setTitle(file.name)
            .setItems(arrayOf("Open", "Share", "Delete")) { _, which ->
                when (which) {
                    0 -> openFile(file)
                    1 -> shareFile(file)
                    2 -> deleteFile(file)
                }
            }
            .show()
    }

    private fun openFile(file: File) {
        try {
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            startActivity(Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, getMimeType(file.name))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            })
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal buka: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareFile(file: File) {
        try {
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            startActivity(Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = getMimeType(file.name)
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }, "Share"))
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal share", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteFile(file: File) {
        android.app.AlertDialog.Builder(this)
            .setTitle("Hapus?")
            .setMessage(file.name)
            .setPositiveButton("Hapus") { _, _ ->
                if (file.delete()) { loadFiles(); Toast.makeText(this, "Terhapus", Toast.LENGTH_SHORT).show() }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun getMimeType(name: String): String = when {
        name.endsWith(".mp4") -> "video/mp4"
        name.endsWith(".mp3") -> "audio/mpeg"
        name.endsWith(".jpg") || name.endsWith(".jpeg") -> "image/jpeg"
        name.endsWith(".png") -> "image/png"
        name.endsWith(".pdf") -> "application/pdf"
        else -> "*/*"
    }

    inner class FileAdapter(val items: List<File>, val onClick: (File) -> Unit)
        : RecyclerView.Adapter<FileAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvIcon: TextView = v.findViewById(R.id.tvIcon)
            val tvName: TextView = v.findViewById(R.id.tvName)
            val tvInfo: TextView = v.findViewById(R.id.tvInfo)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_file, parent, false))
        override fun onBindViewHolder(holder: VH, position: Int) {
            val file = items[position]
            holder.tvIcon.text = if (file.isDirectory) "[D]" else "[F]"
            holder.tvName.text = file.name
            holder.tvInfo.text = if (file.isDirectory) "${file.listFiles()?.size ?: 0} item"
                else "${file.length() / 1024} KB"
            holder.itemView.setOnClickListener { onClick(file) }
        }
        override fun getItemCount() = items.size
    }
}
