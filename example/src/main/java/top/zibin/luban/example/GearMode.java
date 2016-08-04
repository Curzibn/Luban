package top.zibin.luban.example;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import top.zibin.luban.Luban;

/**
 * Created by liuyao on 16/8/4.
 * ~
 */
@IntDef({
                Luban.FIRST_GEAR,
                Luban.THIRD_GEAR
        })
@Retention(RetentionPolicy.SOURCE)
public @interface GearMode {
}
