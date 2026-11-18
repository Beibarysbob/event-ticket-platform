package kz.kimep.ticketing.shared.config;

import java.time.Duration;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@Configuration
public class RedisConfig {
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
            .disableCachingNullValues().entryTtl(Duration.ofMinutes(5));
        Map<String, RedisCacheConfiguration> settings = Map.of(
            "events", defaults.entryTtl(Duration.ofMinutes(15)),
            "eventLists", defaults.entryTtl(Duration.ofMinutes(5)),
            "venues", defaults.entryTtl(Duration.ofHours(1)));
        return RedisCacheManager.builder(factory).cacheDefaults(defaults)
            .withInitialCacheConfigurations(settings).build();
    }
}
