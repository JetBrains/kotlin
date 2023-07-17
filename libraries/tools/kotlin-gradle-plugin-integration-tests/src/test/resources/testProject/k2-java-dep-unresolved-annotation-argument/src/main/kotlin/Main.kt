import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.actuate.autoconfigure.jdbc.DataSourceHealthContributorAutoConfiguration
import org.springframework.boot.actuate.autoconfigure.jdbc.DataSourceHealthIndicatorProperties
import org.springframework.boot.actuate.health.HealthContributor
import org.springframework.boot.jdbc.metadata.DataSourcePoolMetadataProvider
import javax.sql.DataSource

class MyDataSourceHealthContributorAutoConfiguration(
        metadataProviders: ObjectProvider<DataSourcePoolMetadataProvider>,
) : DataSourceHealthContributorAutoConfiguration(metadataProviders) {
    override fun dbHealthContributor(
            dataSources: MutableMap<String, DataSource>,
            dataSourceHealthIndicatorProperties: DataSourceHealthIndicatorProperties,
    ): HealthContributor? {
        return super.dbHealthContributor(dataSources, dataSourceHealthIndicatorProperties)
    }
}

