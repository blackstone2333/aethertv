// SPDX-License-Identifier: GPL-2.0-only
package app.orchard.tv.cinema

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class AccountStore(context: Context) {
    private val prefs = context.getSharedPreferences("cinema_accounts", Context.MODE_PRIVATE)
    val device: String = prefs.getString("device",null) ?: UUID.randomUUID().toString().also { prefs.edit().putString("device",it).apply() }
    var active: String?
        get() = prefs.getString("active",null)
        set(v) { prefs.edit().putString("active",v).apply() }
    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        return (ks.getKey("cinema.tokens.v1",null) as? SecretKey) ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder("cinema.tokens.v1",KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    private fun seal(raw: String): String {
        val c = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE,key()) }
        return Base64.encodeToString(c.iv + c.doFinal(raw.toByteArray(Charsets.UTF_8)),Base64.NO_WRAP)
    }
    private fun open(raw: String): String {
        val bytes = Base64.decode(raw,Base64.NO_WRAP)
        val c = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,bytes.copyOfRange(0,12))) }
        return String(c.doFinal(bytes.copyOfRange(12,bytes.size)),Charsets.UTF_8)
    }
    fun all(): List<Account> = runCatching {
        JSONArray(open(prefs.getString("accounts",null) ?: return emptyList())).objects().map { Account(it.getString("id"),Backend.valueOf(it.getString("backend")),it.getString("server"),it.getString("user"),it.getString("name"),it.getString("token"),device,it.text("version")) }
    }.getOrDefault(emptyList())
    fun save(account: Account) { write((all().filterNot { it.id == account.id || (it.server == account.server && it.userId == account.userId && it.backend == account.backend) } + account).takeLast(8)); active = account.id }
    fun remove(id: String) { write(all().filterNot { it.id == id }); if (active == id) active = null }
    private fun write(accounts: List<Account>) {
        val array = JSONArray()
        accounts.forEach { array.put(obj("id" to it.id,"backend" to it.backend.name,"server" to it.server,"user" to it.userId,"name" to it.name,"token" to it.token,"version" to it.version)) }
        prefs.edit().putString("accounts",seal(array.toString())).apply()
    }
}

class ResumeStore(context: Context) {
    private val prefs = context.getSharedPreferences("cinema_resume",Context.MODE_PRIVATE)
    fun position(a: Account,id: String): Long? = prefs.getString("${a.id}:$id",null)?.substringBefore(':')?.toLongOrNull()
    fun save(a: Account,id: String,ticks: Long) {
        val key="${a.id}:$id"
        val obsolete=prefs.all.entries.filter {it.key.startsWith(a.id+":") && it.key!=key}
            .sortedByDescending {it.value.toString().substringAfter(':',"0").toLongOrNull() ?: 0}.drop(511)
        prefs.edit().apply {obsolete.forEach {remove(it.key)};putString(key,"$ticks:${System.currentTimeMillis()}")}.apply()
    }
    fun clear(a: Account) { prefs.edit().apply { prefs.all.keys.filter { it.startsWith(a.id+":") }.forEach { remove(it) } }.apply() }
    fun overlay(a: Account,m: Media) = position(a,m.id)?.let { m.copy(resume=it.coerceAtMost(m.ticks.takeIf { n -> n > 0 } ?: Long.MAX_VALUE)) } ?: m
}
