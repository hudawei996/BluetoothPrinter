package com.wuyr.bluetoothprinter.model;

import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;

/**
 * Created by wuyr on 17-8-10 下午11:26.
 */

public abstract class RequestListener<T> implements Observer<T> {

    @Override
    public abstract void onNext(@NonNull T t) ;

    @Override
    public abstract void onError(@NonNull Throwable e);

    @Override
    public void onComplete() {

    }

    @Override
    public void onSubscribe(@NonNull Disposable d) {
    }
}