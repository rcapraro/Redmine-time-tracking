package com.ps.redmine.di

import com.ps.redmine.api.KtorRedmineClient
import com.ps.redmine.api.RedmineClientInterface
import com.ps.redmine.update.UpdateManager
import com.ps.redmine.update.UpdateService
import org.koin.dsl.module

val appModule = module {
    // Register the KtorRedmineClient as the implementation
    single<RedmineClientInterface> {
        KtorRedmineClient(
            initialUri = getProperty("redmine.uri"),
            initialApiKey = getProperty("redmine.apiKey")
        )
    }

    // Register update-related services
    single { UpdateService() }
    single { UpdateManager(get()) }
}
