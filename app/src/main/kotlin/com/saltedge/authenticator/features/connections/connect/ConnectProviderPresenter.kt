/*
 * This file is part of the Salt Edge Authenticator distribution
 * (https://github.com/saltedge/sca-authenticator-android).
 * Copyright (c) 2019 Salt Edge Inc.
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
package com.saltedge.authenticator.features.connections.connect

import android.content.Context
import android.webkit.URLUtil.isValidUrl
import com.saltedge.authenticator.R
import com.saltedge.authenticator.model.db.Connection
import com.saltedge.authenticator.model.db.ConnectionsRepositoryAbs
import com.saltedge.authenticator.model.toConnection
import com.saltedge.authenticator.model.repository.PreferenceRepositoryAbs
import com.saltedge.authenticator.sdk.AuthenticatorApiManager
import com.saltedge.authenticator.sdk.AuthenticatorApiManagerAbs
import com.saltedge.authenticator.sdk.contract.ConnectionCreateResult
import com.saltedge.authenticator.sdk.contract.FetchProviderConfigurationDataResult
import com.saltedge.authenticator.sdk.model.*
import com.saltedge.authenticator.sdk.model.response.CreateConnectionData
import com.saltedge.authenticator.sdk.tools.keystore.KeyStoreManagerAbs
import com.saltedge.authenticator.sdk.tools.parseRedirect
import javax.inject.Inject

class ConnectProviderPresenter @Inject constructor(
    private val appContext: Context,
    private val preferenceRepository: PreferenceRepositoryAbs,
    private val connectionsRepository: ConnectionsRepositoryAbs,
    private val keyStoreManager: KeyStoreManagerAbs,
    private val apiManager: AuthenticatorApiManagerAbs
) : ConnectProviderContract.Presenter, ConnectionCreateResult, FetchProviderConfigurationDataResult {

    override val reportProblemActionText: Int?
        get() = if (viewMode.isCompleteWithSuccess) null else R.string.actions_contact_support
    private var sessionFailMessage: String? = null
    private var connectConfigurationLink: String = ""
    private var connectQueryParam: String? = null
    private var connectUrlData: CreateConnectionData? = null
    private var connection = Connection()
    private var viewMode: ViewMode = ViewMode.START
    override var viewContract: ConnectProviderContract.View? = null
    override val shouldShowProgressView: Boolean
        get() = viewMode == ViewMode.START
    override val shouldShowWebView: Boolean
        get() = viewMode == ViewMode.WEB_ENROLL
    override val shouldShowCompleteView: Boolean
        get() = viewMode == ViewMode.COMPLETE_SUCCESS || viewMode == ViewMode.COMPLETE_ERROR
    override val logoUrl: String
        get() = connection.logoUrl
    override val iconResId: Int
        get() = if (viewMode.isCompleteWithSuccess) R.drawable.ic_complete_ok_70 else R.drawable.ic_auth_error_70
    override val mainActionTextResId: Int
        get() = if (viewMode.isCompleteWithSuccess) R.string.actions_proceed else R.string.actions_try_again
    override val completeTitle: String
        get() {
            return if (viewMode.isCompleteWithSuccess)
                appContext.getString(R.string.connect_status_provider_success).format(connection.name)
            else appContext.getString(R.string.errors_connection_failed)
        }
    override val completeMessage: String
        get() {
            return if (viewMode.isCompleteWithSuccess) {
                appContext.getString(R.string.connect_status_provider_success_description)
            } else {
                sessionFailMessage ?: appContext.getString(
                    R.string.errors_connection_failed_description
                ) ?: ""
            }
        }

    override fun setInitialData(connectConfigurationLink: String?,
                                connectQueryParam: String?,
                                connectionGuid: GUID?) {
        if (connectConfigurationLink == null && connectionGuid == null) {
            viewMode = ViewMode.COMPLETE_ERROR
        } else if (connectionGuid != null) {
            this.connection = connectionsRepository.getByGuid(connectionGuid) ?: Connection()
        } else {
            this.connectConfigurationLink = connectConfigurationLink ?: ""
            this.connectQueryParam = connectQueryParam
        }
    }

    override fun onViewCreated() {
        when (viewMode) {
            ViewMode.START -> startConnectFlow()
            ViewMode.WEB_ENROLL -> loadWebRedirectUrl()
            ViewMode.COMPLETE_SUCCESS -> Unit
            ViewMode.COMPLETE_ERROR -> Unit
        }
    }

    override fun onViewClick(viewId: Int) {
        if (viewId == R.id.mainActionView) viewContract?.closeView()
    }

    override fun fetchProviderConfigurationDataResult(result: ProviderData?, error: ApiErrorData?) {
        when {
            error != null -> viewContract?.showErrorAndFinish(error.getErrorMessage(appContext))
            result != null && result.isValid() -> {
                result.toConnection()?.let {
                    this.connection = it
                    performNewConnectionRequest()
                } ?: viewContract?.showErrorAndFinish(appContext.getString(R.string.errors_unable_connect_provider))
            }
            else -> viewContract?.showErrorAndFinish(appContext.getString(R.string.errors_unable_connect_provider))
        }
    }

    override fun onConnectionCreateFailure(error: ApiErrorData) {
        viewContract?.showErrorAndFinish(error.getErrorMessage(appContext))
    }

    override fun onConnectionCreateSuccess(response: CreateConnectionData) {
        response.redirectUrl?.let { redirectUrl ->
            if (redirectUrl.startsWith(AuthenticatorApiManager.authenticationReturnUrl)) {
                parseRedirect(
                    url = redirectUrl,
                    success = { connectionID, accessToken ->
                        webAuthFinishSuccess(connectionID, accessToken)
                    },
                    error = { errorClass, errorMessage ->
                        webAuthFinishError(errorClass, errorMessage)
                    }
                )
            } else if (isValidUrl(response.redirectUrl ?: "")) {
                connection.id = response.connectionId ?: ""
                connectUrlData = response
                viewMode = ViewMode.WEB_ENROLL
                viewContract?.updateViewsContent()
                loadWebRedirectUrl()
            }
        }

    }

    override fun onDestroyView() {
        if (connection.guid.isNotEmpty() && connection.accessToken.isEmpty()) {
            keyStoreManager.deleteKeyPair(connection.guid)
        }
    }

    override fun webAuthFinishError(errorClass: String, errorMessage: String?) {
        viewMode = ViewMode.COMPLETE_ERROR
        sessionFailMessage = errorMessage
        viewContract?.updateViewsContent()
    }

    override fun webAuthFinishSuccess(id: ConnectionID, accessToken: Token) {
        viewMode = ViewMode.COMPLETE_SUCCESS
        connection.id = id
        connection.accessToken = accessToken
        connection.status = "${ConnectionStatus.ACTIVE}"
        if (connectionsRepository.connectionExists(connection)) {
            connectionsRepository.saveModel(connection)
        } else {
            connectionsRepository.fixNameAndSave(connection)
        }
        viewContract?.updateViewsContent()
    }

    override fun getTitleResId(): Int {
        return if (this.connection.guid.isEmpty()) R.string.connections_new_connection
        else R.string.actions_reconnect
    }

    private fun startConnectFlow() {
        if (connection.guid.isEmpty()) {
            apiManager.getProviderConfigurationData(connectConfigurationLink, resultCallback = this)
        } else {
            performNewConnectionRequest()
        }
    }

    private fun performNewConnectionRequest() {
        keyStoreManager.createRsaPublicKeyAsString(appContext, connection.guid)?.let {
            apiManager.createConnectionRequest(
                baseUrl = connection.connectUrl,
                publicKey = it,
                pushToken = preferenceRepository.cloudMessagingToken,
                providerCode = connection.code,
                connectQueryParam = connectQueryParam,
                resultCallback = this
            )
        }
    }

    private fun loadWebRedirectUrl() {
        viewContract?.loadUrlInWebView(url = connectUrlData?.redirectUrl ?: "")
    }
}

enum class ViewMode {
    START, WEB_ENROLL, COMPLETE_SUCCESS, COMPLETE_ERROR;

    val isCompleteWithSuccess: Boolean
        get() = this == COMPLETE_SUCCESS
}
