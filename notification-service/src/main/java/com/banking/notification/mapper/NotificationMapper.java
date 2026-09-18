package com.banking.notification.mapper;

import com.banking.notification.dto.NotificationResponse;
import com.banking.notification.entity.Notification;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    NotificationResponse toResponse(Notification notification);

    List<NotificationResponse> toResponseList(List<Notification> notifications);
}
