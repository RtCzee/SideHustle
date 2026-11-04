package com.example.sidehustle.data.model

import com.example.sidehustle.network.ApiJson
import com.example.sidehustle.network.SideHustleApi
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import java.lang.reflect.Type

/** The response DTOs are also the UI's models; there is no separate domain-mapping layer. */
class ApiJsonMappingTest {
    private val retrofit = Retrofit.Builder().baseUrl("https://example.test/")
        .addConverterFactory(ApiJson.converterFactory).build()

    @Test fun `profile maps snake case fields and explicit settings`() {
        val profile: UserProfileResponse = parse("profile.json", "getProfile")
        assertEquals(UserProfileResponse(
            userId = "firebase-user-123", email = "lerato@example.com", fullName = "Lerato Molefe",
            phoneNumber = "+27821234567", profilePictureUrl = "https://example.com/lerato.png",
            preferredCurrency = "ZAR", preferredLanguage = "en", notificationsEnabled = true,
        ), profile)
    }

    @Test fun `client list maps contact fields IDs and timestamps`() {
        val clients: List<ClientResponse> = parse("clients.json", "getClients")
        assertEquals(3, clients.size)
        assertEquals(ClientResponse(
            clientId = "11111111-1111-4111-8111-111111111111", name = "Mokoena Studio",
            email = "accounts@example.com", phoneNumber = "+27831234567", address = "12 Market Street",
            notes = "Email invoices", createdAt = "2026-10-02T09:00:00.000Z", updatedAt = "2026-10-03T10:00:00.000Z",
        ), clients[0])
    }

    @Test fun `omitted and explicit null client contact fields stay optional`() {
        val clients: List<ClientResponse> = parse("clients.json", "getClients")
        assertEquals(ClientResponse("22222222-2222-4222-8222-222222222222", "Walk-in client"), clients[1])
        assertEquals(ClientResponse("33333333-3333-4333-8333-333333333333", "New client"), clients[2])
        assertNull(clients[1].email)
        assertNull(clients[2].email)
    }

    @Test fun `jobs map client links amounts statuses and dates`() {
        val jobs: List<JobResponse> = parse("jobs.json", "getJobs")
        assertEquals(2, jobs.size)
        assertEquals(JobResponse(
            jobId = "44444444-4444-4444-8444-444444444444", clientId = "11111111-1111-4111-8111-111111111111",
            clientName = "Mokoena Studio", title = "Portfolio website", description = "Five-page site",
            status = "Completed", startDate = "2026-10-03", dueDate = "2026-10-20", completedDate = "2026-10-19",
            agreedAmount = 2750.75, currency = "ZAR",
        ), jobs[0])
        assertEquals(JobResponse(
            jobId = "55555555-5555-4555-8555-555555555555", clientId = "22222222-2222-4222-8222-222222222222",
            clientName = "Walk-in client", title = "Logo concept", status = "Pending", agreedAmount = 0.0, currency = "ZAR",
        ), jobs[1])
    }

    @Test fun `income maps decimal and whole amounts payment method and nullable description`() {
        val income: List<IncomeResponse> = parse("income.json", "getIncome")
        assertEquals(listOf(
            IncomeResponse("66666666-6666-4666-8666-666666666666", 1250.55, "ZAR", "2026-10-20", "EFT", "Website deposit"),
            IncomeResponse("77777777-7777-4777-8777-777777777777", 200.0, "ZAR", "2026-10-21", "Cash", null),
        ), income)
    }

    @Test fun `expenses map category currency date amount and nullable description`() {
        val expenses: List<ExpenseResponse> = parse("expenses.json", "getExpenses")
        assertEquals(listOf(
            ExpenseResponse("88888888-8888-4888-8888-888888888888", 349.95, "ZAR", "2026-10-10", "Software", "Design licence"),
            ExpenseResponse("99999999-9999-4999-8999-999999999999", 45.0, "ZAR", "2026-10-11", "Transport", null),
        ), expenses)
    }

    private fun <T> parse(fixture: String, endpoint: String): T {
        // Suspend endpoints declare their response type on Continuation<? super T>.
        // Read that exact type, including List<T>, rather than duplicating the API contract.
        val method = SideHustleApi::class.java.declaredMethods.single { it.name == endpoint }
        val continuation = method.genericParameterTypes.last() as java.lang.reflect.ParameterizedType
        val response = continuation.actualTypeArguments.single() as java.lang.reflect.WildcardType
        val type: Type = response.lowerBounds.single()
        val converter = retrofit.responseBodyConverter<T>(type, method.annotations)
        val json = checkNotNull(javaClass.getResource("/api/$fixture")).readText()
        return json.toResponseBody("application/json".toMediaType()).use { body -> checkNotNull(converter.convert(body)) }
    }
}
