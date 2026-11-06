package com.example.sidehustle

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.sidehustle.data.local.LocalDataSource
import com.example.sidehustle.data.local.SideHustleDatabase
import com.example.sidehustle.data.model.ClientRequest
import com.example.sidehustle.data.model.ClientResponse
import com.example.sidehustle.data.model.CreateExpenseRequest
import com.example.sidehustle.data.model.CreateIncomeRequest
import com.example.sidehustle.data.model.CreateInvoiceRequest
import com.example.sidehustle.data.model.CreateProfileRequest
import com.example.sidehustle.data.model.HealthResponse
import com.example.sidehustle.data.model.JobRequest
import com.example.sidehustle.data.model.SyncRequest
import com.example.sidehustle.data.model.SyncResponse
import com.example.sidehustle.data.model.UserProfileResponse
import com.example.sidehustle.data.remote.ApiResult
import com.example.sidehustle.data.remote.RemoteDataSource
import com.example.sidehustle.data.repository.SideHustleRepositoryImpl
import com.example.sidehustle.network.SideHustleApi
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.UUID
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@RunWith(AndroidJUnit4::class)
class OfflineRepositoryTest {
    @Test
    fun apiFailureFallsBackToRoomAndKeepsLinkedDataAcrossReopen() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "offline-test-${UUID.randomUUID()}.db"
        var user = "alice"
        var status = SyncStatus.ONLINE
        val remote = stub { path, _ ->
            if (path == "/health") HealthResponse("ok", "sidehustle-api", "error")
            else throw IOException("API unavailable")
        }
        fun repository(db: SideHustleDatabase) =
            SideHustleRepositoryImpl(remote, LocalDataSource(db), { user }, { "$user@example.test" }, { status = it })

        try {
            withDatabase(Room.databaseBuilder(context, SideHustleDatabase::class.java, name).build()) { db ->
                val repo = repository(db)
                repo.refresh()
                assertEquals(SyncStatus.OFFLINE, status)
                repo.createProfile(CreateProfileRequest("Alice")).value()
                val client = repo.createClient(ClientRequest("Studio", "", "", "", "")).value()
                val job = repo.createJob(JobRequest(client.clientId, "Design", "", "Completed", "", "",
                    "2026-11-05", 750.0)).value()
                val invoice = repo.createInvoice(CreateInvoiceRequest(client.clientId, job.jobId)).value()
                repo.updateInvoiceStatus(invoice.id, "Sent").value()
                repo.createIncome(CreateIncomeRequest(600.0, "2026-11-05", "EFT",
                    clientId = client.clientId, jobId = job.jobId, invoiceId = invoice.id)).value()
                repo.createExpense(CreateExpenseRequest(125.0, "2026-11-05", "Software")).value()
                assertEquals(7, LocalDataSource(db).pending(user).size)
            }
            withDatabase(Room.databaseBuilder(context, SideHustleDatabase::class.java, name).build()) { db ->
                val repo = repository(db)
                val invoice = repo.fetchInvoices().value().single()
                assertEquals(invoice.id, repo.fetchIncome().value().single().invoiceId)
                assertEquals("Sent", invoice.status)
                assertEquals(475.0, repo.fetchDashboard().value().netProfit, 0.0)
                assertEquals(750.0, repo.fetchDashboard().value().outstandingPayments, 0.0)
                val clientId = repo.fetchClients().value().single().clientId
                user = "bob"
                assertTrue(repo.fetchClients().value().isEmpty())
                assertTrue(LocalDataSource(db).pending(user).isEmpty())
                user = "alice"
                repo.deleteClient(clientId).value()
                assertTrue(repo.fetchClients().value().isEmpty())
                assertTrue(repo.fetchJobs().value().isEmpty())
                assertTrue(repo.fetchInvoices().value().isEmpty())
                val income = repo.fetchIncome().value().single()
                assertNull(income.clientId)
                assertNull(income.jobId)
                assertNull(income.invoiceId)
            }
        } finally {
            context.deleteDatabase(name)
        }
    }

    @Test
    fun recoveryReplaysSameOperationAfterLostResponse() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        var serverClient: ClientResponse? = null
        val operationIds = mutableListOf<String>()
        var status = SyncStatus.ONLINE
        val remote = stub { path, body ->
            when (path) {
                "/health" -> HealthResponse("ok", "sidehustle-api", "connected")
                "/clients" -> listOfNotNull(serverClient)
                "/me" -> UserProfileResponse("alice", "alice@example.test", "Alice",
                    preferredCurrency = "ZAR", preferredLanguage = "en")
                "/jobs", "/expenses", "/income", "/invoices" -> emptyList<Any>()
                "/sync" -> {
                    val operation = Gson().fromJson(body, SyncRequest::class.java).operations.single()
                    operationIds.add(operation.operationId)
                    if (operationIds.size == 1) {
                        serverClient = ClientResponse(operation.recordId, "Server copy")
                        throw IOException("Response lost after commit")
                    }
                    val value = Gson().toJsonTree(serverClient).asJsonObject
                    SyncResponse(listOf(operation.operationId), mapOf(operation.operationId to value))
                }
                else -> throw IOException("Unexpected API call: $path")
            }
        }
        withDatabase(Room.inMemoryDatabaseBuilder(context, SideHustleDatabase::class.java).build()) { db ->
            val local = LocalDataSource(db)
            val repo = SideHustleRepositoryImpl(remote, local, { "alice" }, { "alice@example.test" }, { status = it })
            val pending = repo.createClient(ClientRequest("Local copy", "", "", "", "")).value()
            assertEquals(SyncStatus.OFFLINE, status)
            assertEquals(1, local.pending("alice").size)
            assertEquals(pending, repo.fetchClient(pending.clientId).value())
            repo.refresh()
            assertEquals(SyncStatus.ONLINE, status)
            assertEquals(2, operationIds.size)
            assertEquals(operationIds.first(), operationIds.last())
            assertTrue(local.pending("alice").isEmpty())
            assertEquals(serverClient, repo.fetchClients().value().single())
        }
    }

    private suspend fun withDatabase(db: SideHustleDatabase, test: suspend (SideHustleDatabase) -> Unit) {
        try {
            test(db)
        } finally {
            db.close()
        }
    }

    private fun stub(call: (String, String) -> Any): RemoteDataSource {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            val buffer = Buffer()
            request.body?.writeTo(buffer)
            val body = call(request.url.encodedPath, buffer.readUtf8())
            Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body(Gson().toJson(body).toResponseBody("application/json".toMediaType())).build()
        }.build()
        val api = Retrofit.Builder().baseUrl("https://sidehustle.test/").client(client)
            .addConverterFactory(GsonConverterFactory.create()).build().create(SideHustleApi::class.java)
        return RemoteDataSource(api)
    }

    private fun <T> ApiResult<T>.value(): T = when (this) {
        is ApiResult.Success -> data
        is ApiResult.Error -> error(message)
    }
}
