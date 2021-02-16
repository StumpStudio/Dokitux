package ru.armagidon.dokitux.utils;

public interface DownloadCallback
{
    void success();
    void fail(String message);
}
