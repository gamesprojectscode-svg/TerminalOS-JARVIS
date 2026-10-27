package com.terminalos.app

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.webkit.*
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : Activity() {
    private lateinit var web: WebView
    private val rootDir: File by lazy { File(filesDir, "workspace").apply { mkdirs() } }
    private val prefs by lazy { getSharedPreferences("terminalos", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()
        if (!Python.isStarted()) Python.start(AndroidPlatform(this))
        web = WebView(this)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.allowFileAccess = true
        web.settings.allowContentAccess = false
        web.webViewClient = WebViewClient()
        web.addJavascriptInterface(Bridge(), "Native")
        setContentView(web)
        web.loadUrl("file:///android_asset/index.html")
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 77)
        }
    }

    private fun jsResult(value: String): String = JSONObject.quote(value)
    private fun safeFile(path: String): File {
        val clean = path.trim().replace("\\", "/").removePrefix("/")
        val f = File(rootDir, clean).canonicalFile
        require(f.path.startsWith(rootDir.canonicalPath + File.separator) || f == rootDir.canonicalFile) { "Caminho fora da pasta de trabalho bloqueado." }
        return f
    }

    inner class Bridge {
        @JavascriptInterface fun appInfo(): String = JSONObject().apply {
            put("model", Build.MODEL); put("android", Build.VERSION.RELEASE)
            put("workspace", "workspace"); put("version", "2.0.0")
        }.toString()

        @JavascriptInterface fun command(raw: String): String {
            return try {
                val parts = raw.trim().split(Regex("\\s+"), limit = 2)
                val cmd = parts.firstOrNull()?.lowercase() ?: ""
                val arg = parts.getOrElse(1) { "" }.trim().trim('"', '\'')
                when (cmd) {
                    "", "help" -> "Comandos: help, clear, date, device, pwd, ls [pasta], mkdir nome, touch arquivo, cat arquivo, write arquivo|texto, rm arquivo, python codigo, ipconfig, wifi, bluetooth, settings, reminder texto|minutos"
                    "date" -> SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())
                    "device" -> "Dispositivo: ${Build.MANUFACTURER} ${Build.MODEL}\\nAndroid ${Build.VERSION.RELEASE}\\nTerminal OS 2.0.0"
                    "pwd" -> "/workspace"
                    "ls" -> {
                        val dir = safeFile(arg.ifBlank { "." })
                        if (!dir.exists()) "Pasta não encontrada."
                        else if (!dir.isDirectory) "Não é uma pasta."
                        else dir.listFiles()?.sortedBy { it.name }?.joinToString("\\n") { (if (it.isDirectory) "[DIR] " else "[FILE] ") + it.name } ?: "(vazio)"
                    }
                    "mkdir" -> { require(arg.isNotBlank()) { "Uso: mkdir nome" }; val f=safeFile(arg); if (f.mkdirs() || f.isDirectory) "Pasta criada: $arg" else "Não foi possível criar." }
                    "touch" -> { require(arg.isNotBlank()) { "Uso: touch arquivo.txt" }; val f=safeFile(arg); f.parentFile?.mkdirs(); if (f.exists()) "Arquivo já existe." else if (f.createNewFile()) "Arquivo criado: $arg" else "Falha ao criar arquivo." }
                    "cat" -> { val f=safeFile(arg); if (f.isFile) f.readText().take(20000) else "Arquivo não encontrado." }
                    "write" -> {
                        val split = arg.split("|", limit=2); require(split.size==2) { "Uso: write arquivo.txt|conteúdo" }
                        val f=safeFile(split[0]); f.parentFile?.mkdirs(); f.writeText(split[1]); "Salvo: ${split[0]}"
                    }
                    "rm" -> { val f=safeFile(arg); if (!f.exists()) "Arquivo não encontrado." else if (f.isDirectory) "Por segurança, rm não apaga pastas. Apague os arquivos primeiro." else { f.delete(); "Arquivo removido." } }
                    "ipconfig", "ifconfig" -> networkInfo()
                    "wifi" -> { startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)); "Abrindo configurações de Wi-Fi. Senhas salvas não podem ser lidas diretamente por apps comuns." }
                    "bluetooth" -> { startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)); "Abrindo configurações de Bluetooth." }
                    "settings" -> { startActivity(Intent(Settings.ACTION_SETTINGS)); "Abrindo configurações do Android." }
                    "python" -> runPython(arg)
                    "reminder" -> {
                        val a=arg.split("|", limit=2); require(a.size==2) { "Uso: reminder texto|minutos" }
                        val mins=a[1].toLongOrNull() ?: throw IllegalArgumentException("Informe minutos. Ex.: reminder Estudar|10")
                        scheduleReminder(a[0], mins); "Lembrete agendado para daqui a $mins minuto(s)."
                    }
                    "clear" -> "__CLEAR__"
                    else -> "Comando não reconhecido: $cmd\\nDigite help para ver os comandos."
                }
            } catch (e: Exception) { "Erro: ${e.message ?: "operação não concluída"}" }
        }

        @JavascriptInterface fun listFiles(path: String): String = try {
            val dir=safeFile(path.ifBlank { "." })
            val arr=org.json.JSONArray()
            dir.listFiles()?.sortedBy { it.name }?.forEach { arr.put(JSONObject().put("name",it.name).put("dir",it.isDirectory).put("size",it.length())) }
            arr.toString()
        } catch (e: Exception) { "[]" }

        @JavascriptInterface fun readFile(path: String): String = try { safeFile(path).readText().take(100000) } catch (e: Exception) { "" }
        @JavascriptInterface fun saveFile(path: String, content: String): String = try {
            val f=safeFile(path); f.parentFile?.mkdirs(); f.writeText(content); "OK"
        } catch (e: Exception) { "ERRO: ${e.message}" }

        @JavascriptInterface fun scheduleReminder(title: String, minutes: Int): String = try {
            scheduleReminder(title, minutes.toLong()); "Lembrete agendado."
        } catch (e: Exception) { "Não foi possível agendar: ${e.message}" }

        @JavascriptInterface fun runPython(code: String): String = runPythonInternal(code)
    }

    private fun runPython(code: String): String = runPythonInternal(code)
    private fun runPythonInternal(code: String): String = try {
        val module = Python.getInstance().getModule("runner")
        module.callAttr("run_code", code).toString()
    } catch (e: Exception) { "Erro Python: ${e.message ?: e.javaClass.simpleName}" }

    private fun networkInfo(): String {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val net = cm.activeNetwork
        val caps = cm.getNetworkCapabilities(net)
        val wifi = getSystemService(WIFI_SERVICE) as WifiManager
        val sb = StringBuilder()
        sb.append("Conexão ativa: ").append(if (caps != null) "sim" else "não").append("\n")
        sb.append("Wi-Fi ligado: ").append(if (wifi.isWifiEnabled) "sim" else "não").append("\n")
        sb.append("Android: ").append(Build.VERSION.RELEASE).append("\n")
        sb.append("IP local Wi-Fi: ")
        try {
            @Suppress("DEPRECATION")
            val ip = wifi.connectionInfo.ipAddress
            sb.append(if (ip == 0) "indisponível" else String.format(Locale.US, "%d.%d.%d.%d", ip and 255, ip shr 8 and 255, ip shr 16 and 255, ip shr 24 and 255))
        } catch (_: Exception) { sb.append("indisponível") }
        sb.append("\nGateway/DNS podem estar ocultos por restrições do Android.")
        return sb.toString()
    }

    private fun scheduleReminder(title: String, minutes: Long) {
        require(minutes > 0) { "O intervalo precisa ser maior que zero." }
        val intent = Intent(this, ReminderReceiver::class.java).putExtra("title", title)
        val id = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        val pi = PendingIntent.getBroadcast(this, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val alarm = getSystemService(ALARM_SERVICE) as AlarmManager
        alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + minutes * 60_000L, pi)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel("reminders", "Lembretes Terminal OS", NotificationManager.IMPORTANCE_HIGH)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}

class ReminderReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Lembrete"
        val notification = NotificationCompat.Builder(context, "reminders")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Terminal OS • Lembrete")
            .setContentText(title)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
    }
}
