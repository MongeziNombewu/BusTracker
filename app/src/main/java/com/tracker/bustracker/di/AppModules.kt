package com.tracker.bustracker.di

import com.tracker.bustracker.BuildConfig
import com.tracker.bustracker.data.remote.TflApiService
import com.tracker.bustracker.data.remote.interceptor.ApiKeyInterceptor
import com.tracker.bustracker.data.repository.ArrivalsRepository
import com.tracker.bustracker.data.repository.JourneyRepository
import com.tracker.bustracker.data.repository.RouteRepository
import com.tracker.bustracker.domain.usecase.GetLiveArrivalsUseCase
import com.tracker.bustracker.domain.usecase.PlanJourneyUseCase
import com.tracker.bustracker.domain.usecase.ResolveBusPositionsUseCase
import com.tracker.bustracker.domain.usecase.SearchStopPointsUseCase
import com.tracker.bustracker.presentation.journeyresults.JourneyResultsViewModel
import com.tracker.bustracker.presentation.search.SearchViewModel
import com.tracker.bustracker.presentation.tracking.TrackingViewModel
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

private const val BASE_URL = "https://api.tfl.gov.uk/"

val networkModule = module {
    single {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }

    single {
        OkHttpClient.Builder()
            .addInterceptor(ApiKeyInterceptor(BuildConfig.TFL_API_KEY))
            .addInterceptor(
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
            )
            .build()
    }

    single {
        val json: Json = get()
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(get())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    single { get<Retrofit>().create(TflApiService::class.java) }
}

val repositoryModule = module {
    single { JourneyRepository(get(), get()) }
    single { ArrivalsRepository(get()) }
    single { RouteRepository(get()) }
}

val useCaseModule = module {
    factory { SearchStopPointsUseCase(get()) }
    factory { PlanJourneyUseCase(get()) }
    factory { GetLiveArrivalsUseCase(get()) }
    factory { ResolveBusPositionsUseCase(get(), get()) }
}

val viewModelModule = module {
    viewModel { SearchViewModel(get()) }
    viewModel { JourneyResultsViewModel(get()) }
    viewModel { params -> TrackingViewModel(params.get(), get(), get()) }
}

val appModules = listOf(networkModule, repositoryModule, useCaseModule, viewModelModule)
