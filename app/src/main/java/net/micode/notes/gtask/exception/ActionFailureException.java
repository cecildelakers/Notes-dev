/*
 * ActionFailureException 类的注释
 *
 * 该异常类是运行时异常的子类，用于表示操作失败的异常情况。它可以包含一个错误消息和导致异常的 Throwable 对象。
 * 这个类主要是为了处理任务或动作执行失败的情况，提供了一个通用的方式来报告和处理这类错误。
 *
 * 许可证信息: 见类文件头部的版权声明
 */

package net.micode.notes.gtask.exception;

// 引入 Java 运行时异常类

import java.lang.RuntimeException;

/**
 * ActionFailureException 类定义了一个操作失败时抛出的异常。
 */
public class ActionFailureException extends RuntimeException {
    private static final long serialVersionUID = 4425249765923293627L; // 序列化 ID

    /**
     * 无参构造函数，创建一个不带详细消息的动作失败异常实例。
     */
    public ActionFailureException() {
        super();
    }

    /**
     * 带有详细信息的构造函数，创建一个带有详细错误消息的动作失败异常实例。
     *
     * @param paramString 错误信息字符串，用于描述异常的详细情况。
     */
    public ActionFailureException(String paramString) {
        super(paramString);
    }

    /**
     * 带有详细信息和导致异常的原因的构造函数，创建一个带有详细错误消息和导致异常的 Throwable 对象的动作失败异常实例。
     *
     * @param paramString    错误信息字符串，用于描述异常的详细情况。
     * @param paramThrowable 导致异常的 Throwable 对象。
     */
    public ActionFailureException(String paramString, Throwable paramThrowable) {
        super(paramString, paramThrowable);
    }
}
