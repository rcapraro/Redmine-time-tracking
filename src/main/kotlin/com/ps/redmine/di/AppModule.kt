package com.ps.redmine.di

import com.ps.redmine.api.RedmineClient
import org.koin.dsl.module

val appModule = module {
    single { 
        RedmineClient(
            uri = getProperty("redmine.uri"),
            username = getProperty("redmine.username"),
            password = getProperty("redmine.password")
        )
    }
}
