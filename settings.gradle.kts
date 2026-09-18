rootProject.name = "banking-microservices-demo"

include(
    "eureka-server",
    "config-server",
    "api-gateway",
    "dummy-service",
    "auth-service",
    "account-service",
    "payment-service",
    "transaction-service",
    "notification-service"
)
