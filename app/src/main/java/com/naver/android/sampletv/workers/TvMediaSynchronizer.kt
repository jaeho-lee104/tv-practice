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

package com.naver.android.sampletv.workers

import android.content.Context
import android.net.Uri
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.naver.android.sampletv.models.TvMediaBackground
import com.naver.android.sampletv.models.TvMediaCollection
import com.naver.android.sampletv.models.TvMediaMetadata
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets

/** Maps a JSONArray of strings */
private fun <T> JSONArray.mapString(transform: (String) -> T): List<T> =
    (0 until length()).map { transform(getString(it)) }

/** Maps a JSONArray of objects */
private fun <T> JSONArray.mapObject(transform: (JSONObject) -> T): List<T> =
    (0 until length()).map { transform(getJSONObject(it)) }

/** Worker that parses metadata from our assets folder and synchronizes the database */
class TvMediaSynchronizer(private val context: Context, params: WorkerParameters) :
    Worker(context, params) {

    /** Helper data class used to pass results around functions */
    data class FeedParseResult(
        val metadata: List<TvMediaMetadata>,
        val collections: List<TvMediaCollection>,
        val backgrounds: List<TvMediaBackground>
    )

    override fun doWork(): Result = try {
        Result.success()
    } catch (exc: Exception) {
        Result.failure()
    }

    companion object {
        private val TAG = TvMediaSynchronizer::class.java.simpleName

        /** Fetches the metadata feed from our assets folder and parses its metadata */
        public fun parseMediaFeed(context: Context): FeedParseResult {
            // Reads JSON input into a JSONArray
            // We are using a local file, in your app you most likely will be using a remote URL
            val stream = context.resources.assets.open("media-feed.json")
            val data = JSONObject(
                String(stream.readBytes(), StandardCharsets.UTF_8)
            )

            // Initializes an empty list to populate with metadata metadata
            val metadatas: MutableList<TvMediaMetadata> = mutableListOf()

            // Traverses the feed and maps each collection
            val feed = data.getJSONArray("feed")
            val collections = feed.mapObject { obj ->

                val collection = TvMediaCollection(
                    id = obj.getString("id"),
                    title = obj.getString("title"),
                    description = obj.getString("description"),
                    artUri = obj.getString("image")?.let { Uri.parse(it) })

                // Traverses the collection and map each content item metadata
                val subItemsMetadata = obj.getJSONArray("items").mapObject { subItem ->
                    TvMediaMetadata(
                        collectionId = collection.id,
                        id = subItem.getString("id"),
                        title = subItem.getString("title"),
                        ratings = subItem.optJSONArray("ratings")?.mapString { x -> x },
                        contentUri = subItem.getString("url")?.let { Uri.parse(it) }!!,
                        playbackDurationMillis = subItem.getLong("duration") * 1000,
                        year = subItem.getInt("year"),
                        author = subItem.getString("director"),
                        description = subItem.getString("description"),
                        artUri = subItem.getString("art")?.let { Uri.parse(it) })
                }

                // Adds all the subitems to the flat list
                metadatas.addAll(subItemsMetadata)

                // Returns parsed collection
                collection
            }

            // Gets background images from metadata feed as well
            val bgArray = data.getJSONArray("backgrounds")
            val backgrounds = (0 until bgArray.length()).map { idx ->
                TvMediaBackground("$idx", Uri.parse(bgArray.getString(idx)))
            }

            return FeedParseResult(metadatas, collections, backgrounds)
        }
    }
}
