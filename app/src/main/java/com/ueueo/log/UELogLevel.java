package com.ueueo.log;

/**
 * Log级别
 */
public class UELogLevel {
    //用来打印输出价值比较低的信息
    public static final int VERBOSE = 1;
    //用来打印调试信息
    public static final int DEBUG = 2;
    //用来打印一般提示信息
    public static final int INFO = 3;
    //用来打印警告信息，这种信息一般是提示开发者需要注意，有可能会出现问题！
    public static final int WARN = 4;
    //用来打印错误崩溃日志信息，例如在try-catch的catch中输出捕获的错误信息。
    public static final int ERROR = 5;
    //用来打印不太可能发生的错误，表明当前问题是个严重的等级
    public static final int ASSERT = 6;

    /**
     * 不打印日志
     */
    public static final int NONE = 7;
}
