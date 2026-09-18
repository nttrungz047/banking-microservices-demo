package com.banking.notification.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.notification.mail")
public class NotificationMailConfig {

    private String from = "noreply@banking.local";
    private boolean enabled = true;
}
