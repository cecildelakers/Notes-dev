/**
 * 这段代码定义了一个名为 ActionFailureException 的异常类，用于表示操作失败的异常情况。以下是其主要功能总结：
 *
 * 继承关系：ActionFailureException 类是 RuntimeException 的子类，因此它继承了 RuntimeException 类的所有特性和行为。
 *
 * 构造函数：提供了三种不同的构造函数，用于创建不同类型的 ActionFailureException 实例：
 *
 * 无参构造函数：创建一个不带详细信息的异常实例。
 * 带有详细信息的构造函数：创建一个包含错误信息的异常实例。
 * 带有详细信息和导致异常的 Throwable 对象的构造函数：创建一个包含错误信息和导致异常的原因的异常实例。
 * 序列化版本号：定义了一个静态的 serialVersionUID 常量，用于确保序列化和反序列化过程中的兼容性。
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
