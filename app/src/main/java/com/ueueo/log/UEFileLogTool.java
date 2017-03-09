package com.ueueo.log;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

/**
 * 文件输出Log
 * <p>
 * 将日志输出到文件中
 * <p>
 * 日志文件存储在外部存储空间的UELOG文件夹下
 */
public class UEFileLogTool implements UELogTool {

    // 日志文件保存的文件夹目录
    private static String LOG_DIR_PATH;

    private static HashMap<String, File> mLogFiles = new HashMap<>();

    /**
     * 日志文件存储路径为外部存储UELog文件夹下
     */
    public UEFileLogTool() {
        File file = new File(Environment.getExternalStorageDirectory(), "UELOG");
        LOG_DIR_PATH = file.getAbsolutePath();
    }

    @Override
    public void d(String tag, String message) {
        writeToFile(Log.DEBUG, tag, message);
    }

    @Override
    public void e(String tag, String message) {
        writeToFile(Log.ERROR, tag, message);
    }

    @Override
    public void w(String tag, String message) {
        writeToFile(Log.WARN, tag, message);
    }

    @Override
    public void i(String tag, String message) {
        writeToFile(Log.INFO, tag, message);
    }

    @Override
    public void v(String tag, String message) {
        writeToFile(Log.VERBOSE, tag, message);
    }

    @Override
    public void wtf(String tag, String message) {
        writeToFile(Log.ERROR, tag, message);
    }

    /**
     * 将日志写入文件中
     *
     * @param priority
     * @param tag
     * @param msg
     */
    private synchronized void writeToFile(int priority, String tag, String msg) {
        String trueTag = tag.split("\\[")[0];
        File logFile = mLogFiles.get(trueTag);
        if (logFile == null) {
            File logDir = new File(LOG_DIR_PATH, trueTag);
            if (!logDir.exists()) {
                logDir.mkdirs();
            }

            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            Date date = new Date(System.currentTimeMillis());
            String fileName = format.format(date) + ".log";
            logFile = new File(logDir, fileName);
            if (!logFile.exists()) {
                try {
                    logFile.createNewFile();
                } catch (IOException e) {
                }
            }
            mLogFiles.put(trueTag, logFile);
        }
        BufferedWriter bufWriter = null;
        OutputStreamWriter out = null;
        try {
            out = new OutputStreamWriter(new FileOutputStream(logFile, true), "UTF-8");
            bufWriter = new BufferedWriter(out);
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            Date date = new Date(System.currentTimeMillis());
            String priorityName = null;
            if (priority == Log.VERBOSE) {
                priorityName = "V";
            } else if (priority == Log.INFO) {
                priorityName = "I";
            } else if (priority == Log.DEBUG) {
                priorityName = "D";
            } else if (priority == Log.WARN) {
                priorityName = "W";
            } else if (priority == Log.ERROR) {
                priorityName = "E";
            } else {
                priorityName = "V";
            }
            bufWriter.write(format.format(date) + ": " + priorityName + "/" + tag + ": " + msg + "\r\n");
        } catch (Exception e) {
        } catch (Error error) {
        } finally {
            if (bufWriter != null) {
                try {
                    bufWriter.close();
                } catch (Exception e) {
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (Exception e) {
                }
            }
        }
    }
}
