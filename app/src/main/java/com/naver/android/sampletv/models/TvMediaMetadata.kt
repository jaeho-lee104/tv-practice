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

package com.naver.android.sampletv.models

import android.net.Uri
import android.os.Parcelable
import androidx.tvprovider.media.tv.TvContractCompat
import kotlinx.android.parcel.Parcelize

/**
 * Data class representing a piece of content metadata (title, content URI, state-related fields
 * [playback position], etc.)
 */
@Parcelize
data class TvMediaMetadata(
    /** User-provided identifier for this piece of content */
    val id: String,

    /** Each metadata item can only be part of one collection */
    var collectionId: String,

    /** Title displayed to user */
    var title: String,

    /** URI for the content to be played */
    var contentUri: Uri,

    /** Author of the metadata content */
    var author: String? = null,

    /** Year in which the metadata content was released */
    var year: Int? = null,

    /** Duration in seconds of the metadata content */
    var playbackDurationMillis: Long? = null,

    /** Current playback position for this piece of content */
    var playbackPositionMillis: Long? = null,

    /** Content ratings (e.g. G, PG, R) */
    var ratings: List<String>? = null,

    /** Content genres, from TvContractCompat.Programs.Genres */
    var genres: List<String>? = null,

    /** Short description of the content shown to users */
    var description: String? = null,

    /** Track or episode number for this piece of metadata */
    var trackNumber: Int? = null,

    /** URI pointing to the album or poster art */
    var artUri: Uri? = null,

    /**
     * Aspect ratio for the art, must be one of the constants under
     * [TvContractCompat.PreviewPrograms]. Defaults to movie poster.
     */
    var artAspectRatio: Int = TvContractCompat.PreviewPrograms.ASPECT_RATIO_MOVIE_POSTER,

    /** Flag indicating if it's hidden from home screen channel */
    var hidden: Boolean = false,

    /** Flag indicating if it's added to watch next channel */
    var watchNext: Boolean = false,

    /** The type of program. Defaults to movie, must be one of PreviewProgramColumns.TYPE_... */
    var programType: Int = TvContractCompat.PreviewProgramColumns.TYPE_MOVIE

) : Parcelable {

    /**
     * Determine if an instance of this class carries state based on whether the fields below have
     * anything other than the default values.
     */
    private fun isStateless() = playbackDurationMillis == null && !hidden && !watchNext

    /** Compares only fields not related to the state */
    override fun equals(other: Any?): Boolean = if (isStateless()) {
        super.equals(other)
    } else {
        copy(playbackDurationMillis = null, hidden = false, watchNext = false).equals(other)
    }

    /** We must override [hashCode] if we override the [equals] function */
    override fun hashCode(): Int = if (isStateless()) {
        super.hashCode()
    } else {
        copy(playbackDurationMillis = null, hidden = false, watchNext = false).hashCode()
    }

}