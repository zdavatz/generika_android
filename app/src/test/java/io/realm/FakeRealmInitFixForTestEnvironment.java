package io.realm;

import android.content.Context;
import java.lang.reflect.Field;


public class FakeRealmInitFixForTestEnvironment {
	public static void init(Context context) {
		try {
			Field applicationContext = BaseRealm.class.getDeclaredField(
        "applicationContext");
			applicationContext.set(applicationContext, context);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
