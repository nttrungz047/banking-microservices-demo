package com.banking.account.exception;

public class OptimisticLockingFailureException extends RuntimeException {

    public OptimisticLockingFailureException(String message, Throwable cause) {
        super(message, cause);
    }
}
