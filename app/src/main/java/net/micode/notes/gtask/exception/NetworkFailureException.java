/*
 * NetworkFailureException 类的注释
 *
 * 该异常类用于表示网络操作失败的异常。它是 Exception 的子类，可用来捕获和处理应用程序中发生的网络错误。
 * 可以通过不同的构造函数来创建包含详细信息或不包含详细信息的 NetworkFailureException 实例。
 */

package net.micode.notes.gtask.exception;

public class NetworkFailureException extends Exception {
    private static final long serialVersionUID = 2107610287180234136L;

    // 无参构造函数，用于创建一个不带详细信息的 NetworkFailureException 实例。
    public NetworkFailureException() {
        super();
    }

    // 带有详细信息的构造函数，用于创建一个包含错误信息的 NetworkFailureException 实例。
    public NetworkFailureException(String paramString) {
        super(paramString);
    }

    // 带有详细信息和导致异常的 Throwable 对象的构造函数，用于创建包含错误信息和原因的 NetworkFailureException 实例。
    public NetworkFailureException(String paramString, Throwable paramThrowable) {
        super(paramString, paramThrowable);
    }
}
