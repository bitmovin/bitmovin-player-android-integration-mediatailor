package com.bitmovin.player.integration.mediatailor.network

internal interface DataSourceFactory {
    fun create(url: String): DataSource
}

internal class DefaultDataSourceFactory : DataSourceFactory {
    override fun create(url: String): DataSource = DefaultDataSource(url)
}
