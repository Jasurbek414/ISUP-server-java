package com.isup.config;

import com.isup.netty.IsupPacketEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NettyConfig {

    @Bean
    public IsupPacketEncoder isupPacketEncoder() {
        return new IsupPacketEncoder();
    }
}
