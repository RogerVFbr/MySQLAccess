package com.company.mysqlaccess;

import java.util.List;

public interface OnGetComplete<T> {
    void onSuccess(List<T> data);
    void onFailure();
}
