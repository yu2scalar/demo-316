package com.example.demo_316.exception;

import com.scalar.db.exception.transaction.TransactionException;

public class CustomException extends TransactionException {

    private Integer errorCode;

    public Integer getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(Integer errorCode) {
        this.errorCode = errorCode;
    }

    public CustomException(TransactionException ex, Integer errorCode ) {
        super(ex.getMessage(), ex.getCause(), null);
        this.setErrorCode(errorCode);
    }

    public CustomException(Exception ex, Integer errorCode ) {
        super(ex.getMessage(), ex.getCause(), null);
        this.setErrorCode(errorCode);
    }

    public CustomException(RuntimeException ex, Integer errorCode ) {
        super(ex.getMessage(), ex.getCause(), null);
        this.setErrorCode(errorCode);
    }
    
    public CustomException(Integer errorCode, String message) {
        super(message, (Throwable) null, null);
        this.setErrorCode(errorCode);
    }
}