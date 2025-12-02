package ge.tiger8bit

import ge.tiger8bit.domain.Invitation
import ge.tiger8bit.service.EmailService
import ge.tiger8bit.service.InvitationEmailSender
import io.micronaut.context.annotation.Replaces
import jakarta.inject.Singleton
import java.util.concurrent.CopyOnWriteArrayList

@Singleton
@Replaces(EmailService::class)
class StubInvitationEmailSender : InvitationEmailSender {
    val sent: MutableList<Pair<Invitation, String>> = CopyOnWriteArrayList()

    override fun sendInvitation(invitation: Invitation, inviterName: String) {
        sent += invitation to inviterName
    }
}

