package com.colaboard.config;

import com.colaboard.service.RedisRoomEventSubscriber;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
public class RedisConfig {

    public static final String ROOM_EVENTS_TOPIC = "colaboard:room-events";

    @Bean
    public ChannelTopic roomEventsTopic() {
        return new ChannelTopic(ROOM_EVENTS_TOPIC);
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            RedisRoomEventSubscriber subscriber,
            ChannelTopic roomEventsTopic
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(subscriber, roomEventsTopic);
        return container;
    }
}
