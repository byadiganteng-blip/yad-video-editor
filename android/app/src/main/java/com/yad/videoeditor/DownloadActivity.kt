package com.yad.videoeditor

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

/**
 * Download - dari GitHub releases
 * Created by KARYADI, Coding by KARYADI
 */
class DownloadActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_download)
        } catch (e: Exception) { finish(); return }

        val recycler = findViewById<RecyclerView>(R.id.recyclerDownloads)
        val btnRefresh = findViewById<Button>(R.id.btnRefreshDownloads)
        val progressBar = findViewById<ProgressBar>(R.id.progressDownloads)

        recycler.layoutManager = LinearLayoutManager(this)

        fun load() {
            progressBar.visibility = View.VISIBLE
            lifecycleScope.launch {
                val (ok, runs) = AdminApi.listRuns()
                progressBar.visibility = View.GONE
                if (ok) {
                    recycler.adapter = RunAdapter(runs) { run ->
                        downloadAsset(run.htmlUrl, "run_${run.runNumber}")
                    }
                }
            }
        }

        btnRefresh.setOnClickListener { load() }
        load()
    }

    private fun downloadAsset(url: String, name: String) {
        try {
            val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val req = DownloadManager.Request(Uri.parse(url))
                .setTitle(name)
                .setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS, "$name.html")
            dm.enqueue(req)
            Toast.makeText(this, "Download mulai: $name", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    inner class RunAdapter(
        val items: List<AdminApi.WorkflowRun>,
        val onClick: (AdminApi.WorkflowRun) -> Unit
    ) : RecyclerView.Adapter<RunAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvName: TextView = v.findViewById(R.id.tvName)
            val tvStatus: TextView = v.findViewById(R.id.tvStatus)
            val tvTime: TextView = v.findViewById(R.id.tvTime)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_run, parent, false))
        override fun onBindViewHolder(holder: VH, position: Int) {
            val run = items[position]
            holder.tvName.text = "#${run.runNumber} ${run.name}"
            holder.tvStatus.text = run.status
            holder.tvTime.text = run.createdAt
            holder.itemView.setOnClickListener { onClick(run) }
        }
        override fun getItemCount() = items.size
    }
}
