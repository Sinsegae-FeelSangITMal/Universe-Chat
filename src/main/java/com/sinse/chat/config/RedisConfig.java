
package com.sinse.chat.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory cf) {
        // LocalDateTime 직렬화 지원
        ObjectMapper om = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // << 여기! >> setObjectMapper 안 쓰고, GenericJackson2JsonRedisSerializer 사용
        RedisSerializer<Object> json = new GenericJackson2JsonRedisSerializer(om);

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(cf);

        template.setKeySerializer(RedisSerializer.string());
        template.setValueSerializer(json);
        template.setHashKeySerializer(RedisSerializer.string());
        template.setHashValueSerializer(json);

        // (선택) 기본 직렬화자도 동일하게
        template.setDefaultSerializer(json);

        template.afterPropertiesSet();
        return template;
    }
}