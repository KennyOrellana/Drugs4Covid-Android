package app.kaisa.drugs4covid.di

import android.content.Context
import app.kaisa.drugs4covid.db.D4CDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {
    @Singleton
    @Provides
    internal fun provideDatabase(
        @ApplicationContext context: Context,
    ): D4CDatabase {
        return D4CDatabase.initialize(context)
    }
}
