/* Copyright 2018 charlag
 *
 * This file is a part of Tusky.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * Tusky is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Tusky; if not,
 * see <http://www.gnu.org/licenses>. */

package com.keylesspalace.tusky.di

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.keylesspalace.tusky.db.AccountManager
import com.keylesspalace.tusky.json.Rfc3339DateJsonAdapter
import com.keylesspalace.tusky.network.ConnectionManager
import com.keylesspalace.tusky.network.ConnectionManagerImpl
import dagger.Module
import dagger.Provides
import java.util.Date
import javax.inject.Singleton

/**
 * Created by charlag on 3/24/18.
 */

@Module
class NetworkModule {

    @Provides
    @Singleton
    fun providesGson(): Gson = GsonBuilder()
        .registerTypeAdapter(Date::class.java, Rfc3339DateJsonAdapter())
        .create()

    @Provides
    @Singleton
    fun providesConnectionClientManager(
        accountManager: AccountManager,
        context: Context,
        preferences: SharedPreferences,
        gson: Gson
    ): ConnectionManager = ConnectionManagerImpl(accountManager, context.cacheDir, preferences, gson)
}
