package com.company.mysqlaccess;

public interface OnComplete<T> {
    void onSuccess(T feedback);
    void onFailure();
}
