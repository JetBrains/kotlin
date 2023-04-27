package coffee

import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module(includes = arrayOf(PumpModule::class))
class DripCoffeeModule {
    @Provides @Singleton
    fun provideHeater(): Heater {
        return ElectricHeater()
    }
}
