package com.appdev.inventoryapp.core.di

import com.appdev.inventoryapp.BuildConfig
import com.appdev.inventoryapp.data.repository.AuditLogRepositoryImpl
import com.appdev.inventoryapp.data.repository.InventoryRepositoryImpl
import com.appdev.inventoryapp.data.repository.LoginRepositoryImpl
import com.appdev.inventoryapp.data.repository.SignUpRepositoryImpl
import com.appdev.inventoryapp.data.repository.UserRepositoryImpl
import com.appdev.inventoryapp.domain.repository.AuditLogRepository
import com.appdev.inventoryapp.domain.repository.InventoryRepository
import com.appdev.inventoryapp.domain.repository.LoginRepository
import com.appdev.inventoryapp.domain.repository.SignUpRepository
import com.appdev.inventoryapp.domain.repository.UserRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideAuthRepository(supabaseClient: SupabaseClient): LoginRepository {
        return LoginRepositoryImpl(supabaseClient)
    }

    @Provides
    @Singleton
    fun provideInventoryRepository(supabaseClient: SupabaseClient): InventoryRepository {
        return InventoryRepositoryImpl(supabaseClient)
    }


    @Provides
    @Singleton
    fun provideAuditLogRepository(supabaseClient: SupabaseClient): AuditLogRepository {
        return AuditLogRepositoryImpl(supabaseClient)
    }

    @Provides
    @Singleton
    fun provideUserRepository(supabaseClient: SupabaseClient): UserRepository {
        return UserRepositoryImpl(supabaseClient)
    }

    @Provides
    @Singleton
    fun provideSignUpRepository(supabaseClient: SupabaseClient): SignUpRepository {
        return SignUpRepositoryImpl(supabaseClient)
    }
}