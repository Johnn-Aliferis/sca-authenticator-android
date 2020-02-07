/*
 * This file is part of the Salt Edge Authenticator distribution
 * (https://github.com/saltedge/sca-authenticator-android).
 * Copyright (c) 2020 Salt Edge Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 or later.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * For the additional permissions granted for Salt Edge Authenticator
 * under Section 7 of the GNU General Public License see THIRD_PARTY_NOTICES.md
 */
package com.saltedge.authenticator.features.actions

import com.saltedge.authenticator.app.KEY_GUID
import com.saltedge.authenticator.features.actions.SubmitActionFragment.Companion.KEY_ACTION_DEEP_LINK_DATA
import com.saltedge.authenticator.sdk.model.ActionDeepLinkData
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SubmitActionFragmentTest {

    @Test
    @Throws(Exception::class)
    fun newInstanceTestCase() {
        val actionDeepLinkData = ActionDeepLinkData(
            actionUuid = "actionUuid",
            connectUrl = "connectUrl",
            returnTo = "returnTo"
        )
        val arguments = SubmitActionFragment.newInstance(
            connectionGuid = "guid1",
            actionDeepLinkData = actionDeepLinkData
        ).arguments

        assertThat(arguments?.getString(KEY_GUID), equalTo("guid1"))
        assertThat(arguments?.getSerializable(KEY_ACTION_DEEP_LINK_DATA) as? ActionDeepLinkData,
            equalTo(actionDeepLinkData))
    }
}