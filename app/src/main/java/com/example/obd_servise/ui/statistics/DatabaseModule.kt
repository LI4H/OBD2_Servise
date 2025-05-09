//package com.example.obd_servise.ui.statistics
//
//
//
//import android.content.Context
//import androidx.room.Room
//import com.example.obd_servise.ui.statistics.AppDatabase
//import com.example.obd_servise.ui.statistics.TripDao
//import dagger.Module
//import dagger.Provides
//import dagger.hilt.InstallIn
//import dagger.hilt.components.SingletonComponent
//import dagger.hilt.android.qualifiers.ApplicationContext
//import javax.inject.Singleton
//
//@Module
//@InstallIn(SingletonComponent::class)
//object DatabaseModule {
//
//    @Provides
//    @Singleton
//    fun provideAppDatabase(
//        @ApplicationContext context: Context
//    ): AppDatabase = Room.databaseBuilder(
//        context,
//        AppDatabase::class.java,
//        "app_database"
//    ).fallbackToDestructiveMigration().build()
//
//    @Provides
//    fun provideTripDao(appDatabase: AppDatabase): TripDao = appDatabase.tripDao()
//}
