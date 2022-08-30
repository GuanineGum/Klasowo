package io.github.wulkanowy.data.repositories

import io.github.wulkanowy.data.db.dao.MailboxDao
import io.github.wulkanowy.data.db.entities.Mailbox
import io.github.wulkanowy.data.db.entities.Student
import io.github.wulkanowy.data.mappers.mapToEntities
import io.github.wulkanowy.sdk.Sdk
import io.github.wulkanowy.utils.AutoRefreshHelper
import io.github.wulkanowy.utils.getRefreshKey
import io.github.wulkanowy.utils.init
import io.github.wulkanowy.utils.uniqueSubtract
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MailboxRepository @Inject constructor(
    private val mailboxDao: MailboxDao,
    private val sdk: Sdk,
    private val refreshHelper: AutoRefreshHelper,
) {
    private val cacheKey = "mailboxes"

    suspend fun refreshMailboxes(student: Student) {
        val new = sdk.init(student).getMailboxes().mapToEntities(student)
        val old = mailboxDao.loadAll(student.userLoginId)

        mailboxDao.deleteAll(old uniqueSubtract new)
        mailboxDao.insertAll(new uniqueSubtract old)

        refreshHelper.updateLastRefreshTimestamp(getRefreshKey(cacheKey, student))
    }

    suspend fun getMailbox(student: Student): Mailbox {
        val isExpired = refreshHelper.shouldBeRefreshed(getRefreshKey(cacheKey, student))
        val mailboxes = mailboxDao.loadAll(student.userLoginId)
        val mailbox = mailboxes.filterByStudent(student)

        return if (isExpired || mailbox == null) {
            refreshMailboxes(student)
            val newMailbox = mailboxDao.loadAll(student.userLoginId).filterByStudent(student)

            requireNotNull(newMailbox) {
                "Mailbox for ${student.userName} - ${student.studentName} not found! Saved mailboxes: $mailboxes"
            }

            newMailbox
        } else mailbox
    }

    private fun List<Mailbox>.filterByStudent(student: Student): Mailbox? = find {
        it.studentName.trim() == student.studentName.trim()
    }
}
