package com.appga.depcare.clients

import com.appga.depcare.utils.Metrics
import mu.KLogging
import okhttp3.OkHttpClient
import javax.enterprise.context.Dependent
import javax.inject.Inject

@Dependent
class SonaTypeRepoClient(
	@Inject
    private val metrics: Metrics
) {
    private companion object : KLogging()

    private val httpClient = OkHttpClient()
}