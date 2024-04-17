/**
 * 这段代码定义了一个名为 NetworkFailureException 的异常类，用于表示网络操作失败的异常。以下是其主要功能总结：
 *
 * 继承关系：NetworkFailureException 类是 Exception 类的子类，因此它继承了 Exception 类的所有特性和行为。
 *
 * 构造函数：提供了三种不同的构造函数，用于创建不同类型的 NetworkFailureException 实例：
 *
 * 无参构造函数：创建一个不带详细信息的异常实例。
 * 带有详细信息的构造函数：创建一个包含错误信息的异常实例。
 * 带有详细信息和导致异常的 Throwable 对象的构造函数：创建一个包含错误信息和导致异常的原因的异常实例。
 * 序列化版本号：定义了一个静态的 serialVersionUID 常量，用于确保序列化和反序列化过程中的兼容性。
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
