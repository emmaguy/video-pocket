package com.emmaguy.videopocket.video;

/*
 * Copyright (C) 2014 Read It Later Inc. (Pocket)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

/**
 * Helper methods for saving to Pocket with an Intent.
 */
class PocketUtils {
    private static final String[] POCKET_PACKAGE_NAMES = new String[]{
            "com.ideashower.readitlater.pro",
            "com.pocket.cn",
            "com.pocket.ru",
            "com.pocket.corgi"
    };

    public static Intent sendUrlToPocket(Context context, String url) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setPackage(PocketUtils.getPocketPackageName(context));
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, url);
        return intent;
    }

    /**
     * This looks for all possible Pocket versions and returns the package name of one if it is installed.
     * Otherwise returns null if Pocket is not installed.
     */
    private static String getPocketPackageName(Context context) {
        for (String pname : POCKET_PACKAGE_NAMES) {
            if (isAppInstalled(context, pname)) {
                return pname;
            }
        }
        return null;
    }

    private static boolean isAppInstalled(Context context, String packageName) {
        PackageManager pm = context.getPackageManager();
        PackageInfo info;
        try {
            info = pm.getPackageInfo(packageName, 0);
        } catch (NameNotFoundException e) {
            info = null;
        }

        return info != null;
    }
}
