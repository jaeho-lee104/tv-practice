/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.naver.android.sampletv.utils

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.res.Resources
import android.net.Uri
import android.util.Rational
import androidx.annotation.RequiresApi
import androidx.tvprovider.media.tv.TvContractCompat

/** Collection of static methods used to handle Android TV Home Screen Launcher operations */
@RequiresApi(26)
@SuppressLint("RestrictedApi")
class TvLauncherUtils private constructor() {
    companion object {
        private val TAG = TvLauncherUtils::class.java.simpleName

        /** Helper function used to get the URI of something from the resources folder */
        fun resourceUri(resources: Resources, id: Int): Uri = Uri.Builder()
            .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(resources.getResourcePackageName(id))
            .appendPath(resources.getResourceTypeName(id))
            .appendPath(resources.getResourceEntryName(id))
            .build()

        /**
         * Parse an aspect ratio constant into the equivalent rational number. For example,
         * [TvContractCompat.PreviewPrograms.ASPECT_RATIO_16_9] becomes `Rational(16, 9)`. The
         * constant must be one of ASPECT_RATIO_* in [TvContractCompat.PreviewPrograms].
         */
        fun parseAspectRatio(ratioConstant: Int): Rational = when (ratioConstant) {
            TvContractCompat.PreviewPrograms.ASPECT_RATIO_16_9 -> Rational(16, 9)
            TvContractCompat.PreviewPrograms.ASPECT_RATIO_1_1 -> Rational(1, 1)
            TvContractCompat.PreviewPrograms.ASPECT_RATIO_2_3 -> Rational(2, 3)
            TvContractCompat.PreviewPrograms.ASPECT_RATIO_3_2 -> Rational(3, 2)
            TvContractCompat.PreviewPrograms.ASPECT_RATIO_4_3 -> Rational(4, 3)
            TvContractCompat.PreviewPrograms.ASPECT_RATIO_MOVIE_POSTER -> Rational(1000, 1441)
            else -> throw IllegalArgumentException(
                "Constant must be one of ASPECT_RATIO_* in TvContractCompat.PreviewPrograms"
            )
        }

    }
}