
@file:DependsOn("org.javamoney:moneta:1.3@pom")

import org.javamoney.moneta.spi.MoneyUtils

println("getBigDecimal(1L): " + MoneyUtils.getBigDecimal(1L))

