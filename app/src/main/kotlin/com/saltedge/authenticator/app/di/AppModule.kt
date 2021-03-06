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
package com.saltedge.authenticator.app.di

import android.content.Context
import android.os.Build
import com.saltedge.authenticator.model.db.ConnectionsRepository
import com.saltedge.authenticator.model.db.ConnectionsRepositoryAbs
import com.saltedge.authenticator.model.repository.PreferenceRepository
import com.saltedge.authenticator.model.repository.PreferenceRepositoryAbs
import com.saltedge.authenticator.sdk.AuthenticatorApiManager
import com.saltedge.authenticator.sdk.AuthenticatorApiManagerAbs
import com.saltedge.authenticator.sdk.tools.biometric.BiometricTools
import com.saltedge.authenticator.sdk.tools.biometric.BiometricToolsAbs
import com.saltedge.authenticator.sdk.tools.biometric.isBiometricPromptV28Enabled
import com.saltedge.authenticator.sdk.tools.crypt.CryptoTools
import com.saltedge.authenticator.sdk.tools.crypt.CryptoToolsAbs
import com.saltedge.authenticator.sdk.tools.keystore.KeyStoreManager
import com.saltedge.authenticator.sdk.tools.keystore.KeyStoreManagerAbs
import com.saltedge.authenticator.tool.secure.PasscodeTools
import com.saltedge.authenticator.tool.secure.PasscodeToolsAbs
import com.saltedge.authenticator.widget.biometric.BiometricPromptAbs
import com.saltedge.authenticator.widget.biometric.BiometricPromptManagerV28
import com.saltedge.authenticator.widget.biometric.BiometricsInputDialog
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class AppModule(context: Context) {

    private var _context: Context = context
    private val preferenceRepository = PreferenceRepository.initObject(context)

    @Provides
    @Singleton
    fun provideAppContext(): Context = _context

    @Provides
    @Singleton
    fun provideBiometricTools(): BiometricToolsAbs = BiometricTools(_context, provideKeyStoreManager())

    @Provides
    fun provideBiometricPrompt(biometricTools: BiometricToolsAbs): BiometricPromptAbs? {
        return when {
            isBiometricPromptV28Enabled() -> BiometricPromptManagerV28()
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> BiometricsInputDialog(biometricTools)
            else -> null
        }
    }

    @Provides
    @Singleton
    fun providePasscodeTools(): PasscodeToolsAbs = PasscodeTools

    @Provides
    @Singleton
    fun provideCryptoTools(): CryptoToolsAbs = CryptoTools

    @Provides
    @Singleton
    fun provideAuthenticatorApiManager(): AuthenticatorApiManagerAbs = AuthenticatorApiManager

    @Provides
    @Singleton
    fun provideConnectionsRepository(): ConnectionsRepositoryAbs = ConnectionsRepository

    @Provides
    @Singleton
    fun providePreferenceRepository(): PreferenceRepositoryAbs = preferenceRepository

    @Provides
    @Singleton
    fun provideKeyStoreManager(): KeyStoreManagerAbs = KeyStoreManager
}
