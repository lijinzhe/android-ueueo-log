package com.ueueo.log;

import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

/**
 * 日志打印机
 * <p>
 * 负责日志的格式化和打印
 */
public final class UELogPrinter {

    /**
     * Json缩紧
     */
    private static final int JSON_INDENT = 4;

    /**
     * The minimum stack trace index, starts at this class after two native calls.
     */
    private static final int MIN_STACK_OFFSET = 3;

    /**
     * Drawing toolbox
     */
    private static final char TOP_LEFT_CORNER = '╔';
    private static final char BOTTOM_LEFT_CORNER = '╚';
    private static final char MIDDLE_CORNER = '╟';
    private static final char HORIZONTAL_DOUBLE_LINE = '║';
    private static final String DOUBLE_DIVIDER = "════════════════════════════════════════════";
    private static final String SINGLE_DIVIDER = "────────────────────────────────────────────";
    private static final String TOP_BORDER = TOP_LEFT_CORNER + DOUBLE_DIVIDER + DOUBLE_DIVIDER;
    private static final String BOTTOM_BORDER = BOTTOM_LEFT_CORNER + DOUBLE_DIVIDER + DOUBLE_DIVIDER;
    private static final String MIDDLE_BORDER = MIDDLE_CORNER + SINGLE_DIVIDER + SINGLE_DIVIDER;
    /**
     * Localize single tag and method count for each thread
     */
    private final ThreadLocal<String> localTag = new ThreadLocal<>();
    private final ThreadLocal<Integer> localMethodCount = new ThreadLocal<>();
    private final ThreadLocal<Boolean> localIsPrintToFile = new ThreadLocal<>();

    private final ThreadLocal<List<String>> localMessageList = new ThreadLocal<>();

    /**
     * It is used to determine log settings such as method count, thread info visibility
     */
    private UELogConfig mLogConfig = new UELogConfig();

    public UELogPrinter tag(String tag) {
        if (tag != null) {
            localTag.set(tag);
        }
        return this;
    }

    public UELogPrinter method(int methodCount) {
        localMethodCount.set(methodCount);
        return this;
    }

    public UELogPrinter file(boolean isPrintToFile) {
        localIsPrintToFile.set(isPrintToFile);
        return this;
    }

    UELogConfig getLogConfig() {
        return mLogConfig;
    }

    public UELogPrinter append(String message, Object... args) {
        String msg = createMessage(message, args);
        if (!TextUtils.isEmpty(msg)) {
            List<String> msgList = localMessageList.get();
            if (msgList == null) {
                msgList = new ArrayList<>();
                localMessageList.set(msgList);
            }
            msgList.add(msg);
        }
        return this;
    }

    public UELogPrinter appendJson(String json) {
        String msg = parseJsonMessage(json);
        if (!TextUtils.isEmpty(msg)) {
            List<String> msgList = localMessageList.get();
            if (msgList == null) {
                msgList = new ArrayList<>();
                localMessageList.set(msgList);
            }
            msgList.add(msg);
        }
        return this;
    }

    public UELogPrinter appendXml(String xml) {
        String msg = parseXmlMessage(xml);
        if (!TextUtils.isEmpty(msg)) {
            List<String> msgList = localMessageList.get();
            if (msgList == null) {
                msgList = new ArrayList<>();
                localMessageList.set(msgList);
            }
            msgList.add(msg);
        }
        return this;
    }

    public UELogPrinter appendObject(Object obj) {
        String msg = parseObjectMessage(obj);
        if (!TextUtils.isEmpty(msg)) {
            List<String> msgList = localMessageList.get();
            if (msgList == null) {
                msgList = new ArrayList<>();
                localMessageList.set(msgList);
            }
            msgList.add(msg);
        }
        return this;
    }

    public void d(String message, Object... args) {
        log(UELogLevel.DEBUG, message, args);
    }

    public void e(String message, Object... args) {
        e(null, message, args);
    }

    public void e(Throwable throwable, String message, Object... args) {
        if (throwable != null && message != null) {
            message += " : " + Log.getStackTraceString(throwable);
        }
        if (throwable != null && message == null) {
            message = throwable.toString();
        }
        if (message == null) {
            message = "No message/exception is set";
        }
        log(UELogLevel.ERROR, message, args);
    }

    public void w(String message, Object... args) {
        log(UELogLevel.WARN, message, args);
    }

    public void i(String message, Object... args) {
        log(UELogLevel.INFO, message, args);
    }

    public void v(String message, Object... args) {
        log(UELogLevel.VERBOSE, message, args);
    }

    public void wtf(String message, Object... args) {
        log(UELogLevel.ASSERT, message, args);
    }

    public void json(String json) {
        d(parseJsonMessage(json));
    }

    /**
     * Formats the json content and print it
     *
     * @param xml the xml content
     */
    public void xml(String xml) {
        d(parseXmlMessage(xml));
    }

    /**
     * Formats the obj content and print it
     *
     * @param obj the xml content
     */
    public void object(Object obj) {
        d(parseObjectMessage(obj));
    }

    /**
     * This method is synchronized in order to avoid messy of logs' order.
     */
    private synchronized void log(int logType, String msg, Object... args) {
        if (logType < mLogConfig.getLogLevel()) {
            return;
        }
        String tag = getTag();
        boolean isPrintToFile = getIsPrintToFile();
        if (mLogConfig.isShowThreadInfo()) {
            tag += "[" + Thread.currentThread().getName() + "]";
        }
        String message = createMessage(msg, args);
        int methodCount = getMethodCount();

        if (TextUtils.isEmpty(message)) {
            message = "Empty/NULL log message";
        }

        List<String> appendMsgList = localMessageList.get();
        localMessageList.remove();

        if (methodCount <= 0 && (appendMsgList == null || appendMsgList.size() == 0) && !message.contains(System.getProperty("line.separator"))) {
            //如果只是单行日志，则不加边框直接输出
            logChunk(logType, tag, message, isPrintToFile);
        } else {
            logTopBorder(logType, tag, isPrintToFile);
            logHeaderContent(logType, tag, methodCount, isPrintToFile);

            if (methodCount > 0) {
                logDivider(logType, tag, isPrintToFile);
            }

            if (appendMsgList != null && appendMsgList.size() > 0) {
                for (String appendMsg : appendMsgList) {
                    logContent(logType, tag, appendMsg, isPrintToFile);
                    logDivider(logType, tag, isPrintToFile);
                }
            }
            logContent(logType, tag, message, isPrintToFile);
            logBottomBorder(logType, tag, isPrintToFile);
        }
    }

    private void logTopBorder(int logType, String tag, boolean printToFile) {
        logChunk(logType, tag, TOP_BORDER, printToFile);
    }

    private void logHeaderContent(int logType, String tag, int methodCount, boolean printToFile) {
        StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        String level = "";

        int stackOffset = getStackOffset(trace);

        //corresponding method count with the current stack may exceeds the stack trace. Trims the count
        if (methodCount + stackOffset > trace.length) {
            methodCount = trace.length - stackOffset - 1;
        }

        for (int i = methodCount; i > 0; i--) {
            int stackIndex = i + stackOffset;
            if (stackIndex >= trace.length) {
                continue;
            }
            StringBuilder builder = new StringBuilder();
            builder.append("║ ")
                    .append(level)
                    .append(getSimpleClassName(trace[stackIndex].getClassName()))
                    .append(".")
                    .append(trace[stackIndex].getMethodName())
                    .append(" ")
                    .append(" (")
                    .append(trace[stackIndex].getFileName())
                    .append(":")
                    .append(trace[stackIndex].getLineNumber())
                    .append(")");
            level += "   ";
            logChunk(logType, tag, builder.toString(), printToFile);
        }
    }

    private void logBottomBorder(int logType, String tag, boolean printToFile) {
        logChunk(logType, tag, BOTTOM_BORDER, printToFile);
    }

    private void logDivider(int logType, String tag, boolean printToFile) {
        logChunk(logType, tag, MIDDLE_BORDER, printToFile);
    }

    private void logContent(int logType, String tag, String chunk, boolean printToFile) {
        String[] lines = chunk.split(System.getProperty("line.separator"));
        for (String line : lines) {
            logChunk(logType, tag, HORIZONTAL_DOUBLE_LINE + " " + line, printToFile);
        }
    }

    private void logChunk(int logType, String tag, String chunk, boolean printToFile) {
        List<UELogTool> logTools = mLogConfig.getLogToolList();
        for (UELogTool logTool : logTools) {
            if (!(logTool instanceof UEFileLogTool) || printToFile) {
                switch (logType) {
                    case UELogLevel.ERROR:
                        logTool.e(tag, chunk);
                        break;
                    case UELogLevel.INFO:
                        logTool.i(tag, chunk);
                        break;
                    case UELogLevel.VERBOSE:
                        logTool.v(tag, chunk);
                        break;
                    case UELogLevel.WARN:
                        logTool.w(tag, chunk);
                        break;
                    case UELogLevel.ASSERT:
                        logTool.wtf(tag, chunk);
                        break;
                    case UELogLevel.DEBUG:
                        logTool.d(tag, chunk);
                        break;
                    default:
                        logTool.v(tag, chunk);
                        break;
                }
            }
        }

    }

    private String getSimpleClassName(String name) {
        int lastIndex = name.lastIndexOf(".");
        return name.substring(lastIndex + 1);
    }

    /**
     * @return the appropriate tag based on local or global
     */
    private String getTag() {
        String tag = localTag.get();
        if (!TextUtils.isEmpty(tag)) {
            localTag.remove();
            return tag;
        }
        tag = mLogConfig.getTag();
        if (!TextUtils.isEmpty(tag)) {
            return tag;
        } else {
            return UELogConfig.DEFAULT_TAG;
        }
    }

    private boolean getIsPrintToFile() {
        Boolean printToFile = localIsPrintToFile.get();
        if (printToFile != null) {
            localIsPrintToFile.remove();
            return printToFile;
        }
        return this.mLogConfig.isPrintToFile();
    }

    private String createMessage(String message, Object... args) {
        return args.length == 0 ? message : String.format(message, args);
    }

    private int getMethodCount() {
        Integer count = localMethodCount.get();
        int result = mLogConfig.getMethodCount();
        if (count != null) {
            localMethodCount.remove();
            result = count;
        }
        if (result < 0) {
            throw new IllegalStateException("methodCount cannot be negative");
        }
        return result;
    }

    /**
     * Determines the starting index of the stack trace, after method calls made by this class.
     *
     * @param trace the stack trace
     * @return the stack offset
     */
    private int getStackOffset(StackTraceElement[] trace) {
        for (int i = MIN_STACK_OFFSET; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            String name = e.getClassName();
            if (!name.equals(UELogPrinter.class.getName()) && !name.equals(UELog.class.getName())) {
                return --i;
            }
        }
        return -1;
    }

    /**
     * 格式化xml字符串
     *
     * @param xml
     * @return
     */
    private String parseXmlMessage(String xml) {
        if (UELogLevel.DEBUG < mLogConfig.getLogLevel()) {
            //因为对象输出是以debug级别输出的，所以如果日志级别配置高于DEBUG等级，则不会输出，所以也不需要进行字符串格式化
            return null;
        }
        if (!TextUtils.isEmpty(xml)) {
            try {
                Source xmlInput = new StreamSource(new StringReader(xml));
                StreamResult xmlOutput = new StreamResult(new StringWriter());
                Transformer transformer = TransformerFactory.newInstance().newTransformer();
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
                transformer.transform(xmlInput, xmlOutput);
                return xmlOutput.getWriter().toString().replaceFirst(">", ">\n");
            } catch (TransformerException e) {
                return "Invalid xml content";
            }
        } else {
            return "Empty/Null xml content";
        }
    }

    /**
     * 格式化json字符串
     *
     * @param json
     * @return
     */
    private String parseJsonMessage(String json) {
        if (UELogLevel.DEBUG < mLogConfig.getLogLevel()) {
            //因为对象输出是以debug级别输出的，所以如果日志级别配置高于DEBUG等级，则不会输出，所以也不需要进行字符串格式化
            return null;
        }
        if (!TextUtils.isEmpty(json)) {
            try {
                json = json.trim();
                if (json.startsWith("{")) {
                    JSONObject jsonObject = new JSONObject(json);
                    String message = jsonObject.toString(JSON_INDENT);
                    return message;
                } else if (json.startsWith("[")) {
                    JSONArray jsonArray = new JSONArray(json);
                    String message = jsonArray.toString(JSON_INDENT);
                    return message;
                } else {
                    return "Invalid json content";
                }
            } catch (JSONException e) {
                return "Invalid json content";
            }
        } else {
            return "Empty/Null json content";
        }
    }

    /**
     * 格式化对象
     *
     * @param obj
     * @return
     */
    private String parseObjectMessage(Object obj) {
        if (UELogLevel.DEBUG < mLogConfig.getLogLevel()) {
            //因为对象输出是以debug级别输出的，所以如果日志级别配置高于DEBUG等级，则不会输出，所以也不需要进行字符串格式化
            return null;
        }
        if (obj != null) {
            try {
                if (obj instanceof List) {
                    JSONArray jsonArray = new JSONArray();
                    for (Object o : (List) obj) {
                        JSONObject jo = new JSONObject(new Gson().toJson(o));
                        jsonArray.put(jo);
                    }
                    String message = jsonArray.toString(JSON_INDENT);
                    return message;
                } else if (obj instanceof Map) {
                    Gson gson = new GsonBuilder().enableComplexMapKeySerialization().create();
                    JSONObject jsonObject = new JSONObject(gson.toJson(obj));
                    String message = jsonObject.toString(JSON_INDENT);
                    return message;
                } else {
                    JSONObject jsonObject = new JSONObject(new Gson().toJson(obj));
                    String message = jsonObject.toString(JSON_INDENT);
                    return message;
                }
            } catch (JSONException e) {
                return "Invalid object content";
            }
        } else {
            return "Null object content";
        }
    }
}
