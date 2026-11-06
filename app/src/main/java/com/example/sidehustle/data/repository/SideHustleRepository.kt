package com.example.sidehustle.data.repository

import android.os.SystemClock
import com.example.sidehustle.SyncStatus
import com.example.sidehustle.data.local.CachedRecord
import com.example.sidehustle.data.local.QueuedWrite
import com.example.sidehustle.data.model.ClientRequest
import com.example.sidehustle.data.model.Attachment
import com.example.sidehustle.data.model.ClientResponse
import com.example.sidehustle.data.model.CreateProfileRequest
import com.example.sidehustle.data.model.CreateExpenseRequest
import com.example.sidehustle.data.model.DashboardResponse
import com.example.sidehustle.data.model.DeleteResult
import com.example.sidehustle.data.model.HealthResponse
import com.example.sidehustle.data.model.JobRequest
import com.example.sidehustle.data.model.JobResponse
import com.example.sidehustle.data.model.UpdateProfileRequest
import com.example.sidehustle.data.model.UserProfileResponse
import com.example.sidehustle.data.model.CreateInvoiceRequest
import com.example.sidehustle.data.model.InvoiceClient
import com.example.sidehustle.data.model.InvoiceJob
import com.example.sidehustle.data.model.InvoiceResponse
import com.example.sidehustle.data.model.CreateIncomeRequest
import com.example.sidehustle.data.model.IncomeOptionsResponse
import com.example.sidehustle.data.model.IncomeResponse
import com.example.sidehustle.data.model.IncomeClientOption
import com.example.sidehustle.data.model.IncomeJobOption
import com.example.sidehustle.data.model.IncomeInvoiceOption
import com.example.sidehustle.data.model.SyncOperation
import com.example.sidehustle.data.model.SyncRequest
import com.example.sidehustle.data.model.UpdateInvoiceStatusRequest
import com.example.sidehustle.data.remote.ApiResult
import com.example.sidehustle.data.remote.RemoteDataSource
import com.example.sidehustle.data.local.LocalDataSource
import com.example.sidehustle.data.remote.ErrorKind
import com.google.gson.JsonObject
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

import com.example.sidehustle.data.model.ExpenseResponse

interface SideHustleRepository {
    suspend fun fetchHealth(): ApiResult<HealthResponse>
    suspend fun fetchProfile(): ApiResult<UserProfileResponse>
    suspend fun createProfile(request: CreateProfileRequest): ApiResult<UserProfileResponse>
    suspend fun updateProfile(request: UpdateProfileRequest): ApiResult<UserProfileResponse>
    suspend fun fetchDashboard(): ApiResult<DashboardResponse>
    suspend fun fetchClients(): ApiResult<List<ClientResponse>>
    suspend fun fetchClient(clientId: String): ApiResult<ClientResponse>
    suspend fun createClient(request: ClientRequest): ApiResult<ClientResponse>
    suspend fun updateClient(clientId: String, request: ClientRequest): ApiResult<ClientResponse>
    suspend fun deleteClient(clientId: String): ApiResult<DeleteResult>
    suspend fun fetchJobs(): ApiResult<List<JobResponse>>
    suspend fun fetchJob(jobId: String): ApiResult<JobResponse>
    suspend fun createJob(request: JobRequest): ApiResult<JobResponse>
    suspend fun updateJob(jobId: String, request: JobRequest): ApiResult<JobResponse>
    suspend fun fetchExpenses(): ApiResult<List<ExpenseResponse>>
    suspend fun createExpense(request: CreateExpenseRequest): ApiResult<ExpenseResponse>
    suspend fun fetchIncomeOptions(): ApiResult<IncomeOptionsResponse>
    suspend fun fetchIncome(): ApiResult<List<IncomeResponse>>
    suspend fun createIncome(request: CreateIncomeRequest): ApiResult<IncomeResponse>
    suspend fun fetchInvoiceClients(): ApiResult<List<InvoiceClient>>
    suspend fun fetchInvoiceJobs(clientId: String): ApiResult<List<InvoiceJob>>
    suspend fun fetchInvoices(): ApiResult<List<InvoiceResponse>>
    suspend fun createInvoice(request: CreateInvoiceRequest): ApiResult<InvoiceResponse>
    suspend fun updateInvoiceStatus(invoiceId: String, status: String): ApiResult<InvoiceResponse>
}

/** Serializes foreground edits and replay so a refresh cannot overwrite unsent changes. */
class SideHustleRepositoryImpl(
    private val remote: RemoteDataSource,
    private val local: LocalDataSource,
    private val currentUserId: () -> String?,
    private val currentEmail: () -> String?,
    private val setStatus: (SyncStatus) -> Unit,
    private val currentDisplayName: () -> String? = { null },
) : SideHustleRepository {
    private val mutex = Mutex()
    private var retryAfter = 0L
    private var unavailable = false
    private var availabilityKnown = false
    private var blockedUser: String? = null

    override suspend fun fetchHealth() = remote.fetchHealth()

    suspend fun refresh() = mutex.withLock {
        val user = currentUserId() ?: return@withLock
        if (!checkHealth()) return@withLock
        if (flush(user) != null) return@withLock
        if (currentUserId() != user) return@withLock
        setStatus(SyncStatus.ONLINE)
        when (val profile = remote.fetchProfile()) {
            is ApiResult.Success -> if (currentUserId() == user) local.save(user, PROFILE, user, profile.data)
            is ApiResult.Error -> if (isUnavailable(profile) || profile.httpCode == 401) return@withLock
        }
        mirror(user, CLIENTS, remote::fetchClients, ClientResponse::clientId)
        mirror(user, JOBS, remote::fetchJobs, JobResponse::jobId)
        mirror(user, EXPENSES, remote::fetchExpenses, ExpenseResponse::expenseId)
        mirror(user, INCOME, remote::fetchIncome, IncomeResponse::id)
        mirror(user, INVOICES, remote::fetchInvoices, InvoiceResponse::id)
    }

    private suspend fun checkHealth(): Boolean {
        val health = remote.fetchHealth()
        if (health is ApiResult.Success && health.data.status == "ok" && health.data.database == "connected") {
            unavailable = false
            availabilityKnown = true
            return true
        }
        markOffline()
        return false
    }

    private fun markOffline() {
        availabilityKnown = true
        unavailable = true
        retryAfter = SystemClock.elapsedRealtime() + 10_000
        setStatus(SyncStatus.OFFLINE)
    }

    private suspend fun isUnavailable(error: ApiResult.Error): Boolean {
        if (error.kind == ErrorKind.NETWORK || error.kind == ErrorKind.TIMEOUT ||
            error.httpCode in listOf(502, 503, 504)
        ) {
            markOffline()
            return true
        }
        return error.httpCode?.let { it in 500..599 } == true && !checkHealth()
    }

    private suspend fun canContactApi(): Boolean = when {
        !availabilityKnown -> checkHealth()
        !unavailable -> true
        SystemClock.elapsedRealtime() >= retryAfter -> checkHealth()
        else -> false
    }

    private suspend fun prepare(user: String): Boolean {
        if (!canContactApi() || currentUserId() != user) return false
        if (blockedUser == user && SystemClock.elapsedRealtime() < retryAfter) return false
        if (flush(user) != null) return false
        if (currentUserId() != user) return false
        setStatus(SyncStatus.ONLINE)
        return true
    }

    private suspend fun replay(write: QueuedWrite): ApiResult<JsonObject> {
        val user = write.userId
        if (currentUserId() != user) return sessionChanged()
        val operation = SyncOperation(write.operationId, write.kind, write.action, write.recordId,
            local.gson.fromJson(write.payload, JsonObject::class.java))
        return when (val result = remote.sync(SyncRequest(user, listOf(operation)))) {
            is ApiResult.Error -> result
            is ApiResult.Success -> result.data.results?.get(write.operationId)?.let { ApiResult.Success(it) }
                ?: ApiResult.Error("The server did not confirm the saved change.")
        }
    }

    private suspend fun flush(user: String): ApiResult.Error? {
        val writes = local.pending(user)
        if (writes.isNotEmpty()) setStatus(SyncStatus.SYNCING)
        for (write in writes) {
            when (val result = replay(write)) {
                is ApiResult.Success -> confirm(write, result.data)
                is ApiResult.Error -> {
                    if (currentUserId() == user && !isUnavailable(result)) {
                        blockedUser = user
                        retryAfter = SystemClock.elapsedRealtime() + 10_000
                        setStatus(SyncStatus.BLOCKED)
                    }
                    return result
                }
            }
        }
        blockedUser = null
        return null
    }

    private suspend fun confirm(write: QueuedWrite, value: JsonObject) = local.transaction {
        val (user, kind, id) = Triple(write.userId, write.kind, write.recordId)
        local.remove(write.operationId)
        if (write.action == "remove") {
            deleteClientLocally(user, id, clientRecords(user, id))
        } else if (local.pending(user).none {
                (it.kind == kind && it.recordId == id) ||
                    (it.kind == CLIENTS && it.action == "remove" &&
                        kind in setOf(JOBS, INVOICES) && value.get("client_id")?.asString == it.recordId)
            }) {
            local.dao.put(CachedRecord(user, kind, id, value.toString()))
        }
    }

    private suspend fun clientRecords(user: String, clientId: String): List<CachedRecord> {
        val jobs = local.readAll<JobResponse>(user, JOBS).filter { it.clientId == clientId }
        val invoices = local.readAll<InvoiceResponse>(user, INVOICES).filter { it.clientId == clientId }
        val jobIds = jobs.map { it.jobId }.toSet()
        val invoiceIds = invoices.map { it.id }.toSet()
        val income = local.readAll<IncomeResponse>(user, INCOME).filter {
            it.clientId == clientId || it.jobId in jobIds || it.invoiceId in invoiceIds
        }
        val keys = listOf(CLIENTS to clientId) + jobs.map { JOBS to it.jobId } +
            invoices.map { INVOICES to it.id } + income.map { INCOME to it.id }
        return keys.mapNotNull { (kind, id) ->
            local.dao.get(user, kind, id)?.let { CachedRecord(user, kind, id, it) }
        }
    }

    private suspend fun deleteClientLocally(user: String, clientId: String, records: List<CachedRecord>) {
        val jobIds = records.filter { it.kind == JOBS }.map { it.id }.toSet()
        val invoiceIds = records.filter { it.kind == INVOICES }.map { it.id }.toSet()
        for (record in records) {
            if (record.kind == INCOME) {
                val income = local.gson.fromJson(record.json, IncomeResponse::class.java)
                local.save(user, INCOME, income.id, income.copy(
                    clientId = income.clientId.takeUnless { it == clientId },
                    jobId = income.jobId.takeUnless { it in jobIds },
                    invoiceId = income.invoiceId.takeUnless { it in invoiceIds },
                ))
            } else {
                local.delete(user, record.kind, record.id)
            }
        }
    }

    private fun sessionChanged() = ApiResult.Error("The active account changed. Please try again.", 401)
    private fun missing(message: String) = ApiResult.Error(message, 404)
    private fun invalid(message: String) = ApiResult.Error(message, 400)

    private suspend fun <T> read(
        fetch: suspend () -> ApiResult<T>,
        save: suspend (String, T) -> Unit,
        fallback: suspend (String) -> ApiResult<T>,
    ): ApiResult<T> = mutex.withLock {
        val user = currentUserId() ?: return@withLock sessionChanged()
        if (!prepare(user)) {
            return@withLock if (currentUserId() == user) fallback(user) else sessionChanged()
        }
        val result = fetch()
        if (currentUserId() != user) return@withLock sessionChanged()
        when (result) {
            is ApiResult.Success -> { save(user, result.data); result }
            is ApiResult.Error -> if (isUnavailable(result)) fallback(user) else result
        }
    }

    private suspend inline fun <reified T> list(
        kind: String, noinline fetch: suspend () -> ApiResult<List<T>>, noinline id: (T) -> String,
    ): ApiResult<List<T>> = list(kind, fetch, id) { _, items -> items }

    private suspend inline fun <reified T> list(
        kind: String, noinline fetch: suspend () -> ApiResult<List<T>>, noinline id: (T) -> String,
        crossinline localItems: suspend (String, List<T>) -> List<T>,
    ): ApiResult<List<T>> = read(fetch, { user, items -> local.replace(user, kind, items, id) }) { user ->
        ApiResult.Success(localItems(user, local.readAll(user, kind)))
    }

    private suspend inline fun <reified T> detail(
        kind: String, id: String, noinline fetch: suspend () -> ApiResult<T>, message: String,
    ): ApiResult<T> = detail(kind, id, fetch, message) { _, item -> item }

    private suspend inline fun <reified T> detail(
        kind: String, id: String, noinline fetch: suspend () -> ApiResult<T>, message: String,
        crossinline localItem: suspend (String, T) -> T,
    ): ApiResult<T> = read(fetch, { user, item -> local.save(user, kind, id, item) }) { user ->
        local.read<T>(user, kind, id)?.let { ApiResult.Success(localItem(user, it)) } ?: missing(message)
    }

    private suspend fun <T> mirror(user: String, kind: String, fetch: suspend () -> ApiResult<List<T>>, id: (T) -> String) {
        if (currentUserId() != user || unavailable) return
        val result = fetch()
        if (currentUserId() != user || unavailable) return
        when (result) {
            is ApiResult.Success -> local.replace(user, kind, result.data, id)
            is ApiResult.Error -> isUnavailable(result)
        }
    }

    private suspend inline fun <reified T> write(
        kind: String, id: String, action: String, request: Any,
        crossinline preview: suspend (String) -> ApiResult<T>,
    ): ApiResult<T> = mutex.withLock {
        val user = currentUserId() ?: return@withLock sessionChanged()
        val online = prepare(user)
        if (currentUserId() != user) return@withLock sessionChanged()
        if (!online && kind != PROFILE && offlineProfile(user) == null) {
            return@withLock missing("Profile is unavailable on this device.")
        }
        val localValue = preview(user)
        if (localValue is ApiResult.Error && !online) return@withLock localValue

        val operation = QueuedWrite(userId = user, kind = kind, action = action, recordId = id,
            payload = local.gson.toJson(request))
        val previous = if (action == "remove") clientRecords(user, id)
        else listOfNotNull(local.dao.get(user, kind, id)?.let { CachedRecord(user, kind, id, it) })
        local.transaction {
            if (localValue is ApiResult.Success) {
                if (action == "remove") deleteClientLocally(user, id, previous)
                else local.save(user, kind, id, localValue.data)
            }
            local.enqueue(operation)
        }
        if (!online) return@withLock localValue
        when (val result = replay(operation)) {
            is ApiResult.Success -> {
                confirm(operation, result.data)
                if (currentUserId() != user) sessionChanged()
                else ApiResult.Success(local.gson.fromJson(result.data, T::class.java))
            }
            is ApiResult.Error -> {
                if (currentUserId() != user) return@withLock sessionChanged()
                if (isUnavailable(result) || result.httpCode == null) {
                    setStatus(if (unavailable) SyncStatus.OFFLINE else SyncStatus.BLOCKED)
                    return@withLock if (localValue is ApiResult.Success) localValue else result
                }
                local.transaction {
                    local.remove(operation.operationId)
                    local.delete(user, kind, id)
                    local.dao.putAll(previous)
                }
                result
            }
        }
    }

    override suspend fun fetchProfile(): ApiResult<UserProfileResponse> {
        return read(remote::fetchProfile, { active, item ->
            if (active == item.userId) local.save(active, PROFILE, active, item)
        }) { active ->
            offlineProfile(active)?.let { ApiResult.Success(it) }
                ?: ApiResult.Error("Connect to the server to load your profile on this device.", kind = ErrorKind.NETWORK)
        }
    }

    private suspend fun offlineProfile(user: String): UserProfileResponse? {
        local.read<UserProfileResponse>(user, PROFILE, user)?.let { return it }
        if (currentUserId() != user) return null
        val email = currentEmail()?.takeIf { it.isNotBlank() } ?: return null
        val name = currentDisplayName()?.trim()?.takeIf { it.isNotEmpty() } ?: email.substringBefore('@')
        val request = CreateProfileRequest(name.take(255))
        val profile = UserProfileResponse(user, email, request.fullName,
            preferredCurrency = request.preferredCurrency, preferredLanguage = request.preferredLanguage)
        local.transaction {
            local.save(user, PROFILE, user, profile)
            local.enqueue(QueuedWrite(userId = user, kind = PROFILE, action = "ensure", recordId = user,
                payload = local.gson.toJson(request)))
        }
        return profile
    }

    override suspend fun createProfile(request: CreateProfileRequest): ApiResult<UserProfileResponse> {
        val user = currentUserId() ?: return sessionChanged()
        return write(PROFILE, user, "create", request) { active ->
            if (active != user) return@write sessionChanged()
            val email = currentEmail() ?: return@write invalid("A verified email is required.")
            ApiResult.Success(UserProfileResponse(user, email, request.fullName, request.phoneNumber,
                preferredCurrency = request.preferredCurrency, preferredLanguage = request.preferredLanguage))
        }
    }

    override suspend fun updateProfile(request: UpdateProfileRequest): ApiResult<UserProfileResponse> {
        val user = currentUserId() ?: return sessionChanged()
        return write(PROFILE, user, "update", request) { active ->
            if (active != user) return@write sessionChanged()
            val profile = local.read<UserProfileResponse>(user, PROFILE, user)
                ?: return@write missing("Profile is unavailable on this device.")
            ApiResult.Success(profile.copy(fullName = request.fullName, phoneNumber = request.phoneNumber,
                preferredCurrency = request.preferredCurrency, preferredLanguage = request.preferredLanguage,
                notificationsEnabled = request.notificationsEnabled))
        }
    }

    override suspend fun fetchClients() = list(CLIENTS, remote::fetchClients, ClientResponse::clientId) { _, clients ->
        clients.sortedBy { it.name.lowercase() }
    }

    override suspend fun fetchClient(clientId: String) =
        detail(CLIENTS, clientId, { remote.fetchClient(clientId) }, "Client not found.")

    override suspend fun createClient(request: ClientRequest) = saveClient(newId(), "create", request)

    override suspend fun updateClient(clientId: String, request: ClientRequest) = saveClient(clientId, "update", request)

    private suspend fun saveClient(id: String, action: String, request: ClientRequest) =
        write(CLIENTS, id, action, request) { user ->
            val existing = local.read<ClientResponse>(user, CLIENTS, id)
            if (action == "update" && existing == null) return@write missing("Client not found.")
            ApiResult.Success(ClientResponse(id, request.name.trim(), request.email.ifBlank { null },
                request.phoneNumber, request.address, request.notes, existing?.createdAt))
        }

    override suspend fun deleteClient(clientId: String) = write(CLIENTS, clientId, "remove", emptyMap<String, String>()) {
        ApiResult.Success(DeleteResult(true))
    }

    override suspend fun fetchJobs() = list(JOBS, remote::fetchJobs, JobResponse::jobId) { user, jobs ->
        val clients = local.readAll<ClientResponse>(user, CLIENTS).associateBy { it.clientId }
        jobs.map { it.copy(clientName = clients[it.clientId]?.name ?: it.clientName) }
    }

    override suspend fun fetchJob(jobId: String) =
        detail(JOBS, jobId, { remote.fetchJob(jobId) }, "Project not found.") { user, job: JobResponse ->
            val client = local.read<ClientResponse>(user, CLIENTS, job.clientId)
            job.copy(clientName = client?.name ?: job.clientName)
        }

    override suspend fun createJob(request: JobRequest) = saveJob(newId(), "create", request)

    override suspend fun updateJob(jobId: String, request: JobRequest) = saveJob(jobId, "update", request)

    private suspend fun saveJob(id: String, action: String, request: JobRequest) =
        write(JOBS, id, action, request) { user ->
            val client = local.read<ClientResponse>(user, CLIENTS, request.clientId)
                ?: return@write invalid("Select a client available on this device.")
            val existing = local.read<JobResponse>(user, JOBS, id)
            if (action == "update" && existing == null) return@write missing("Project not found.")
            val currency = existing?.currency ?: local.read<UserProfileResponse>(user, PROFILE, user)?.preferredCurrency ?: "ZAR"
            ApiResult.Success(JobResponse(id, client.clientId, client.name, request.title.trim(), request.description,
                request.status, request.startDate.ifBlank { null }, request.dueDate.ifBlank { null },
                request.completedDate.ifBlank { null }, cents(request.agreedAmount), currency))
        }

    override suspend fun fetchExpenses() = list(EXPENSES, remote::fetchExpenses, ExpenseResponse::expenseId) { _, expenses ->
        expenses.sortedByDescending(ExpenseResponse::expenseDate)
    }

    override suspend fun createExpense(request: CreateExpenseRequest): ApiResult<ExpenseResponse> {
        val id = newId()
        return write(EXPENSES, id, "create", request) {
            ApiResult.Success(ExpenseResponse(id, cents(request.amount), "ZAR", request.expenseDate,
                request.category, request.description))
        }
    }

    override suspend fun fetchIncome() = list(INCOME, remote::fetchIncome, IncomeResponse::id) { _, income ->
        income.sortedByDescending(IncomeResponse::dateReceived)
    }

    override suspend fun createIncome(request: CreateIncomeRequest): ApiResult<IncomeResponse> {
        val id = newId()
        return write(INCOME, id, "create", request) { user ->
            if ((request.clientId != null && local.read<ClientResponse>(user, CLIENTS, request.clientId) == null) ||
                (request.jobId != null && local.read<JobResponse>(user, JOBS, request.jobId) == null) ||
                (request.invoiceId != null && local.read<InvoiceResponse>(user, INVOICES, request.invoiceId) == null)
            ) return@write invalid("A linked record is unavailable on this device.")
            ApiResult.Success(IncomeResponse(id, cents(request.amount), "ZAR", request.dateReceived,
                request.paymentMethod, request.description, request.clientId, request.jobId, request.invoiceId))
        }
    }

    override suspend fun fetchInvoices() = list(INVOICES, remote::fetchInvoices, InvoiceResponse::id) { user, invoices ->
        val clients = local.readAll<ClientResponse>(user, CLIENTS).associateBy { it.clientId }
        invoices.map { it.copy(clientName = clients[it.clientId]?.name ?: it.clientName) }
    }

    override suspend fun createInvoice(request: CreateInvoiceRequest): ApiResult<InvoiceResponse> {
        val id = newId()
        return write(INVOICES, id, "create", request) { user ->
            val job = local.read<JobResponse>(user, JOBS, request.jobId)
            val client = local.read<ClientResponse>(user, CLIENTS, request.clientId)
            if (client == null || job == null || job.clientId != client.clientId) {
                return@write invalid("The selected job does not belong to this client.")
            }
            ApiResult.Success(InvoiceResponse(id, "Pending " + id.take(8), job.agreedAmount, "Draft",
                client.name, client.clientId, job.jobId, currency = job.currency))
        }
    }

    override suspend fun updateInvoiceStatus(invoiceId: String, status: String) =
        write(INVOICES, invoiceId, "update", UpdateInvoiceStatusRequest(status)) { user ->
            val invoice = local.read<InvoiceResponse>(user, INVOICES, invoiceId)
                ?: return@write missing("Invoice not found.")
            ApiResult.Success(invoice.copy(status = status))
        }

    override suspend fun fetchInvoiceClients() = when (val clients = fetchClients()) {
        is ApiResult.Error -> clients
        is ApiResult.Success -> ApiResult.Success(clients.data.map { InvoiceClient(it.clientId, it.name) })
    }

    override suspend fun fetchInvoiceJobs(clientId: String) = when (val jobs = fetchJobs()) {
        is ApiResult.Error -> jobs
        is ApiResult.Success -> ApiResult.Success(jobs.data.filter { it.clientId == clientId }
            .map { InvoiceJob(it.jobId, it.title, it.agreedAmount, it.currency) })
        }

    override suspend fun fetchIncomeOptions() = read(remote::fetchIncomeOptions, { _, _ -> }) { user ->
        ApiResult.Success(IncomeOptionsResponse(
            local.readAll<ClientResponse>(user, CLIENTS).sortedBy { it.name.lowercase() }
                .map { IncomeClientOption(it.clientId, it.name) },
            local.readAll<JobResponse>(user, JOBS).map { IncomeJobOption(it.jobId, it.title) },
            local.readAll<InvoiceResponse>(user, INVOICES).map { IncomeInvoiceOption(it.id, it.number) },
        ))
    }

    suspend fun fetchAttachments(type: String, recordId: String): ApiResult<List<Attachment>> =
        list("attachments/$type/$recordId", { remote.fetchAttachments(type, recordId) }, Attachment::id)

    override suspend fun fetchDashboard() = read(remote::fetchDashboard, { _, _ -> }) { user ->
        val profile = offlineProfile(user)
            ?: return@read missing("Profile is unavailable on this device.")
        val monthStart = SimpleDateFormat("yyyy-MM", Locale.ROOT).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date()) + "-01"
        ApiResult.Success(localDashboard(profile, local.readAll(user, INCOME), local.readAll(user, EXPENSES),
            local.readAll(user, JOBS), local.readAll(user, INVOICES), monthStart))
    }

    private fun newId() = UUID.randomUUID().toString()

    private companion object {
        const val PROFILE = "me"
        const val CLIENTS = "clients"
        const val JOBS = "jobs"
        const val EXPENSES = "expenses"
        const val INCOME = "income"
        const val INVOICES = "invoices"
    }
}
