package ge.tiger8bit.spec.common

import java.util.*

object TestData {

    object Emails {
        const val BOSS = "boss@test.com"
        const val WORKER = "worker@test.com"
        const val APP_OWNER = "app-owner@test.com"
        const val GOOGLE_USER = "google.user@test.com"

        fun boss(suffix: String = "") = if (suffix.isEmpty()) BOSS else "boss-$suffix@test.com"
        fun worker(suffix: String = "") = if (suffix.isEmpty()) WORKER else "worker-$suffix@test.com"
        fun unique(prefix: String = "user") = "$prefix-${UUID.randomUUID().toString().take(8)}@test.com"
    }

    object Orgs {
        const val DEFAULT = "Test Org"
        const val ACCEPT = "Accept Org"
        const val FORBIDDEN = "Forbidden Org"

        fun unique(prefix: String = "Org") = "$prefix ${UUID.randomUUID().toString().take(8)}"
    }

    object Sites {
        const val DEFAULT = "Test Site"

        fun unique(prefix: String = "Site") = "$prefix ${UUID.randomUUID().toString().take(8)}"
    }

    object Invitations {
        const val BOSS_INVITE_EMAIL = "boss@test.com"
        const val WORKER_INVITE_EMAIL = "worker@test.com"
        const val WORKER_ACCEPT_EMAIL = "worker-accept@test.com"
    }
}

