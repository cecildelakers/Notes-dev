/*
 * Contact 类用于通过电话号码查询联系人信息。
 * 该类实现了从联系人数据库中获取与特定电话号码相关联的显示名称。
 */

package net.micode.notes.data;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Data;
import android.telephony.PhoneNumberUtils;
import android.util.Log;

import java.util.HashMap;

public class Contact {
    // 缓存已查询过的电话号码和对应的联系人名称，以减少数据库查询次数。
    private static HashMap<String, String> sContactCache;
    private static final String TAG = "Contact"; // 日志标签

    // 用于查询具有完整国际号码格式的电话号码的selection字符串。
    private static final String CALLER_ID_SELECTION = "PHONE_NUMBERS_EQUAL(" + Phone.NUMBER
            + ",?) AND " + Data.MIMETYPE + "='" + Phone.CONTENT_ITEM_TYPE + "'"
            + " AND " + Data.RAW_CONTACT_ID + " IN "
            + "(SELECT raw_contact_id "
            + " FROM phone_lookup"
            + " WHERE min_match = '+')";

    /**
     * 根据电话号码获取联系人名称。
     *
     * @param context     上下文对象，用于访问内容解析器。
     * @param phoneNumber 需要查询的电话号码。
     * @return 与电话号码相关联的联系人名称，如果找不到则返回null。
     */
    public static String getContact(Context context, String phoneNumber) {
        // 初始化或获取联系人缓存
        if (sContactCache == null) {
            sContactCache = new HashMap<String, String>();
        }

        // 从缓存中直接获取联系人名称，如果存在。
        if (sContactCache.containsKey(phoneNumber)) {
            return sContactCache.get(phoneNumber);
        }

        // 使用PhoneNumberUtils将电话号码格式化为适合查询的形式
        String selection = CALLER_ID_SELECTION.replace("+",
                PhoneNumberUtils.toCallerIDMinMatch(phoneNumber));

        // 执行查询以获取与电话号码相关联的联系人名称
        Cursor cursor = context.getContentResolver().query(
                Data.CONTENT_URI,
                new String[]{Phone.DISPLAY_NAME},
                selection,
                new String[]{phoneNumber},
                null);

        if (cursor != null && cursor.moveToFirst()) {
            try {
                // 从查询结果中获取联系人名称并加入缓存
                String name = cursor.getString(0);
                sContactCache.put(phoneNumber, name);
                return name;
            } catch (IndexOutOfBoundsException e) {
                // 处理查询结果异常
                Log.e(TAG, " Cursor get string error " + e.toString());
                return null;
            } finally {
                // 关闭游标
                cursor.close();
            }
        } else {
            // 如果查询无结果，记录日志
            Log.d(TAG, "No contact matched with number:" + phoneNumber);
            return null;
        }
    }
}
