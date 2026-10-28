package com.example.splitsmart.service;

public interface ResultCallback<T> {
    void onSuccess(T result);
    void onError(Exception e);

    default void onFailure(Exception e) {
        onError(e);
    }
}
